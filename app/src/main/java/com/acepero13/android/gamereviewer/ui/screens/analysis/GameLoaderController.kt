package com.acepero13.android.gamereviewer.ui.screens.analysis

import android.util.Log
import com.acepero13.android.gamereviewer.data.db.CriticalMomentDao
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "GameLoaderController"

internal class GameLoaderController(
    private val session: GameSession,
    private val repo: GameRepository,
    private val criticalMomentDao: CriticalMomentDao,
    private val settingsRepo: SettingsRepository,
    private val sequenceBuilder: GameSequenceBuilder,
    private val analysisRunner: GameAnalysisRunner,
    private val applicator: BoardStateApplicator,
    private val motifMapper: MotifMapper,
    private val backgroundAnalysis: BackgroundAnalysisController,
) {

    fun loadGame() {
        session.scope.launch(Dispatchers.IO) {
            val game = repo.findById(session.gameId) ?: run {
                Log.e(TAG, "loadGame: game ${session.gameId} NOT FOUND"); return@launch
            }
            applySettings()
            resolvePlayerSide(game)
            sequenceBuilder.buildMoveLists(game)
            sequenceBuilder.prewarmAnnotationCache()
            val storedMoments = criticalMomentDao.getByGameId(session.gameId)
            val analysis      = analysisRunner.run()
            analysisRunner.persistEndgame(analysis.endgame)
            session.uiState.update {
                it.copy(
                    game = game, totalMoves = session.uciMoves.size,
                    openingSummary = analysis.openingSummary,
                    phaseSummary   = motifMapper.buildPhaseSummary(session.uciMoves.size),
                    criticalMoments = storedMoments,
                    openingDeviation = analysis.deviation,
                    endgameClassification = analysis.endgame,
                    middlegamePlanClassification = analysis.middlegame,
                )
            }
            applicator.applyMoveIndex(0)
            session.uiState.update { it.copy(showPredictionGate = true) }
            backgroundAnalysis.launch(storedMoments)
        }
    }

    private suspend fun applySettings() {
        val posCoach = settingsRepo.positionCoachEnabled.first()
        val devMode  = settingsRepo.developerModeEnabled.first()
        session.uiState.update {
            it.copy(positionCoachEnabled = posCoach, developerModeEnabled = devMode,
                gameStoryUnlocked = it.gameStoryUnlocked || posCoach)
        }
    }

    private suspend fun resolvePlayerSide(game: com.acepero13.android.gamereviewer.data.model.ReviewGame) {
        val username = settingsRepo.username.first().trim()
        session.boardFlippedForBlack = username.isNotEmpty() &&
            game.blackPlayer.equals(username, ignoreCase = true) &&
            !game.whitePlayer.equals(username, ignoreCase = true)
        session.playerSideKnown = username.isNotEmpty() &&
            (game.whitePlayer.equals(username, ignoreCase = true) ||
             game.blackPlayer.equals(username, ignoreCase = true))
    }
}
