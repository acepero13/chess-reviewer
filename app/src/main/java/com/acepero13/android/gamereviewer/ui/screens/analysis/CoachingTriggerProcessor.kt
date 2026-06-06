package com.acepero13.android.gamereviewer.ui.screens.analysis

import android.util.Log
import com.acepero13.android.gamereviewer.data.db.GameEvaluationDao
import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.data.repository.TriggerMasteryRepository
import com.acepero13.android.gamereviewer.domain.CoachingTrigger
import com.acepero13.android.gamereviewer.domain.CoachingTriggerEvaluator
import com.acepero13.android.gamereviewer.data.db.MoveTimeDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "CoachingTriggerProcessor"

internal class CoachingTriggerProcessor(
    private val session: GameSession,
    private val masteryRepo: TriggerMasteryRepository,
    private val moveTimeDao: MoveTimeDao,
    private val gameEvaluationDao: GameEvaluationDao,
) {

    suspend fun evaluate(
        evaluations: List<GameEvaluation>,
        weakTypes: Set<String>,
    ): Map<Int, List<CoachingTrigger>> {
        val moveTimes = withContext(Dispatchers.IO) {
            moveTimeDao.getByGameId(session.gameId).associateBy { it.moveIndex }
        }
        val allTriggers = withContext(Dispatchers.Default) {
            CoachingTriggerEvaluator.evaluate(
                evaluations      = evaluations,
                fenByMoveIndex   = { idx -> session.fenSequence.getOrElse(idx) { "" } },
                timeByMoveIndex  = { idx -> moveTimes[idx]?.timeSpentSeconds },
                playerIsWhite    = !session.boardFlippedForBlack,
                gameId           = session.gameId,
                weakTriggerTypes = weakTypes,
            )
        }
        Log.d(TAG, "evaluate: ${allTriggers.values.sumOf { it.size }} raw triggers")
        val mastered = masteryRepo.getMasteredTypes()
        persistTriggers(allTriggers)
        return allTriggers
            .mapValues { (_, triggers) -> triggers.filter { it.typeName() !in mastered } }
            .filter { (_, triggers) -> triggers.isNotEmpty() }
    }

    private fun persistTriggers(allTriggers: Map<Int, List<CoachingTrigger>>) {
        if (allTriggers.isEmpty()) return
        session.scope.launch(Dispatchers.IO) {
            allTriggers.forEach { (moveIndex, triggers) ->
                val encoded = triggers.joinToString(",") { it.typeName() }
                gameEvaluationDao.updateTriggers(session.gameId, moveIndex, encoded)
            }
        }
    }
}
