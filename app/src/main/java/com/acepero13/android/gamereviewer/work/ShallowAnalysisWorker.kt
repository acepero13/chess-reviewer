package com.acepero13.android.gamereviewer.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.acepero13.android.gamereviewer.data.db.GameEvaluationDao
import com.acepero13.android.gamereviewer.data.db.GameStatsDao
import com.acepero13.android.gamereviewer.data.db.MotifTacticStatDao
import com.acepero13.android.gamereviewer.data.db.MoveTimeDao
import com.acepero13.android.gamereviewer.data.db.NotablePositionDao
import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.data.repository.SettingsRepository
import com.acepero13.android.gamereviewer.domain.GameStatsCalculator
import com.acepero13.android.gamereviewer.domain.MiddlegamePlanDetector
import com.acepero13.android.gamereviewer.domain.MotifTacticAggregator
import com.acepero13.android.gamereviewer.domain.NotablePositionExtractor
import com.acepero13.android.gamereviewer.domain.PawnStructureTagger
import com.acepero13.android.gamereviewer.domain.TruthMapBuilder
import com.acepero13.chess.core.opening.OpeningClassifier
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Background worker that runs a **fast, shallow** ([SHALLOW_DEPTH]) Stockfish pass over every
 * imported game that has no cached [com.acepero13.android.gamereviewer.data.model.GameStats] yet,
 * then computes and persists its Chess.com-style stats.
 *
 * Games already analyzed at full depth (their [GameEvaluation] rows exist) are reused for free —
 * only un-evaluated games pay the engine cost. Runs as a foreground service so a long batch
 * (40–200 games) survives the OS background execution limit.
 *
 * Dependencies are pulled from the global Koin graph via [KoinComponent], so the default
 * WorkManager factory can instantiate this worker with no custom factory plumbing.
 */
class ShallowAnalysisWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val repo: GameRepository by inject()
    private val gameEvaluationDao: GameEvaluationDao by inject()
    private val moveTimeDao: MoveTimeDao by inject()
    private val gameStatsDao: GameStatsDao by inject()
    private val notablePositionDao: NotablePositionDao by inject()
    private val motifTacticStatDao: MotifTacticStatDao by inject()
    private val truthMapBuilder: TruthMapBuilder by inject()
    private val settingsRepo: SettingsRepository by inject()
    private val middlegamePlanDetector: MiddlegamePlanDetector by inject()
    private val openingClassifier: OpeningClassifier by inject()

    private val pawnStructureTagger by lazy { PawnStructureTagger(middlegamePlanDetector) }

    override suspend fun doWork(): Result {
        val pending = gameStatsDao.gameIdsNeedingStats(GameStatsCalculator.STATS_VERSION)
        if (pending.isEmpty()) return Result.success()

        runCatching { setForeground(foregroundInfo(0, pending.size)) }
        val username = settingsRepo.username.first()

        pending.forEachIndexed { i, gameId ->
            runCatching { analyzeGame(gameId, username) }
            setProgress(workDataOf(KEY_DONE to (i + 1), KEY_TOTAL to pending.size))
            runCatching { setForeground(foregroundInfo(i + 1, pending.size)) }
        }
        return Result.success()
    }

    private suspend fun analyzeGame(gameId: Long, username: String) {
        val game = repo.findById(gameId) ?: return

        val uci = game.movesUci.split(' ').filter { it.isNotBlank() }

        // Reuse already-stored evaluations when present; otherwise run the shallow pass.
        var evaluations = gameEvaluationDao.getByGameId(gameId)
        val ranShallowPass = evaluations.isEmpty()
        if (ranShallowPass) {
            if (uci.isEmpty()) return
            val map = truthMapBuilder.build(uci, depth = SHALLOW_DEPTH)
            evaluations = map.map { e ->
                GameEvaluation(
                    gameId = gameId, moveIndex = e.moveIndex,
                    evalCp = e.evalCp, evalDelta = e.evalDelta,
                    motif = e.motif, pvLine = e.pvLine,
                )
            }
            if (evaluations.isNotEmpty()) gameEvaluationDao.insertAll(evaluations)
        }
        if (evaluations.isEmpty()) return

        val moveTimes = moveTimeDao.getByGameId(gameId)
        val playerIsWhite = GameStatsCalculator.playerIsWhite(game, username)
        val pawnStructure = runCatching {
            pawnStructureTagger.tag(uci, playerIsWhite)
        }.getOrDefault("")
        val bookDepthPly = runCatching {
            bookDepthPly(openingClassifier.classifyByMoves(uci)?.pgn)
        }.getOrDefault(0)
        val stats = GameStatsCalculator.compute(
            game = game,
            evaluations = evaluations,
            moveTimes = moveTimes,
            username = username,
            // Reused evals were produced by the full in-app analysis pass.
            analysisDepth = if (ranShallowPass) SHALLOW_DEPTH else com.acepero13.chess.core.data.model.ChessConstants.DEFAULT_ANALYSIS_DEPTH,
            pawnStructure = pawnStructure,
            bookDepthPly = bookDepthPly,
        )
        gameStatsDao.upsert(stats)

        // Board-thumbnail positions + per-motif find-rate (cleared per-game before re-inserting).
        notablePositionDao.deleteByGameId(gameId)
        runCatching {
            NotablePositionExtractor.extract(gameId, uci, evaluations, playerIsWhite)
        }.getOrDefault(emptyList()).takeIf { it.isNotEmpty() }?.let { notablePositionDao.insertAll(it) }

        motifTacticStatDao.deleteByGameId(gameId)
        MotifTacticAggregator.aggregate(gameId, evaluations, playerIsWhite)
            .takeIf { it.isNotEmpty() }?.let { motifTacticStatDao.insertAll(it) }
    }

    /** Half-move depth of the matched opening line, counting plies in its PGN (e.g. "1. e4 c5" = 2). */
    private fun bookDepthPly(pgn: String?): Int =
        pgn?.split(' ')?.count { it.isNotBlank() && !it.endsWith(".") } ?: 0

    private fun foregroundInfo(done: Int, total: Int): ForegroundInfo {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Game Analysis", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setContentTitle("Analyzing games")
            .setContentText("$done / $total games")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setProgress(total, done, false)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        /** Fast screening depth — enough to separate a blunder from a best move. */
        const val SHALLOW_DEPTH = 12

        const val WORK_NAME = "shallow_analysis"
        const val KEY_DONE = "done"
        const val KEY_TOTAL = "total"

        private const val CHANNEL_ID = "game_analysis"
        private const val NOTIFICATION_ID = 4201
    }
}
