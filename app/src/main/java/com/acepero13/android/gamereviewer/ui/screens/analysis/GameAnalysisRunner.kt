package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.data.db.EndgameEncounterDao
import com.acepero13.android.gamereviewer.data.model.EndgameEncounter
import com.acepero13.android.gamereviewer.domain.EndgameClassification
import com.acepero13.android.gamereviewer.domain.EndgameRecognizer
import com.acepero13.android.gamereviewer.domain.MiddlegamePlanClassification
import com.acepero13.android.gamereviewer.domain.MiddlegamePlanDetector
import com.acepero13.android.gamereviewer.domain.OpeningDeviation
import com.acepero13.android.gamereviewer.domain.OpeningDeviationAnalyzer
import com.acepero13.chess.core.opening.OpeningClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class GameAnalysisResult(
    val openingSummary: String,
    val deviation: OpeningDeviation?,
    val endgame: EndgameClassification?,
    val middlegame: MiddlegamePlanClassification?,
)

internal class GameAnalysisRunner(
    private val session: GameSession,
    private val opening: OpeningClassifier,
    private val deviationAnalyzer: OpeningDeviationAnalyzer,
    private val endgameRecognizer: EndgameRecognizer,
    private val middlegamePlanDetector: MiddlegamePlanDetector,
    private val endgameEncounterDao: EndgameEncounterDao,
) {

    suspend fun run(): GameAnalysisResult {
        val entry     = runCatching { opening.classifyByMoves(session.uciMoves.take(20)) }.getOrNull()
        val deviation = withContext(Dispatchers.Default) {
            runCatching { deviationAnalyzer.analyze(session.uciMoves, session.sanMoves) }.getOrNull()
        }
        val endgame   = withContext(Dispatchers.Default) {
            runCatching { endgameRecognizer.analyze(session.fenSequence) }.getOrNull()
        }
        val startIndex = (deviation?.moveIndex ?: 20).coerceIn(session.fenSequence.indices)
        val middlegame = withContext(Dispatchers.Default) {
            runCatching {
                middlegamePlanDetector.detect(session.fenSequence, !session.boardFlippedForBlack, startIndex)
            }.getOrNull()
        }
        return GameAnalysisResult(
            openingSummary = entry?.let { "${it.eco} · ${it.name}" } ?: "",
            deviation = deviation, endgame = endgame, middlegame = middlegame,
        )
    }

    suspend fun persistEndgame(ec: EndgameClassification?) {
        ec ?: return
        if (endgameEncounterDao.getByGameId(session.gameId) == null) {
            endgameEncounterDao.upsert(
                EndgameEncounter(gameId = session.gameId, moveIndex = ec.firstEndgameMoveIndex,
                    chapter = ec.entry.chapter, category = ec.entry.category,
                    name = ec.entry.name, fen = ec.fen)
            )
        }
    }
}
