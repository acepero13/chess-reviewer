package com.acepero13.android.gamereviewer.ui.screens.analysis

import android.util.Log
import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.ui.screens.ReviewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "NavigationController"
private const val CATEGORY_COOLDOWN_MOVES = 5

internal class NavigationController(
    private val session: GameSession,
    private val applicator: BoardStateApplicator,
    private val motifMapper: MotifMapper,
) {

    fun goToMove(index: Int) {
        if (session.uiState.value.reviewMode == ReviewMode.MENTOR) {
            Log.d(TAG, "goToMove($index): BLOCKED (mentor active)")
            return
        }
        val clamped = index.coerceIn(0, session.uciMoves.size)
        val prev    = session.uiState.value.moveIndex
        if (clamped > prev) checkMissedMoments(fromIndex = prev, toIndex = clamped)
        session.scope.launch(Dispatchers.Default) { applicator.applyMoveIndex(clamped) }
    }

    fun stepForward()  = goToMove(session.uiState.value.moveIndex + 1)
    fun stepBackward() = goToMove(session.uiState.value.moveIndex - 1)
    fun goToStart()    = goToMove(0)
    fun goToEnd()      = goToMove(session.uciMoves.size)
    fun onMoveNodeClick(nodeId: Long) = goToMove(nodeId.toInt())

    fun applyMoveIndex(index: Int) = applicator.applyMoveIndex(index)

    fun checkMissedMoments(fromIndex: Int, toIndex: Int) {
        val state = session.uiState.value
        if (session.truthMap.isEmpty() && state.criticalMoments.isEmpty()) return
        val criticalIndices = resolveCriticalIndices(state)
        for (idx in (fromIndex + 1)..toIndex) {
            if (idx !in criticalIndices) continue
            if (!isUserMove(idx)) continue
            val fen      = session.fenSequence.getOrElse(idx) { continue }
            val annot    = session.annotationCache[fen]
            val reviewed = (annot?.moveComment?.isNotBlank() == true) ||
                (annot?.arrowsJson?.length ?: 0) > 2
            if (!reviewed) {
                val category = getCategoryForMove(idx)
                val lastAt   = state.shownCategoryAtMove[category]
                if (lastAt != null && idx - lastAt <= CATEGORY_COOLDOWN_MOVES) continue
                session.uiState.update {
                    it.copy(
                        showMissedMomentBanner = true,
                        missedMomentMoveIndex  = idx,
                        shownCategoryAtMove    = it.shownCategoryAtMove + (category to idx),
                    )
                }
                return
            }
        }
    }

    fun isUserMove(moveIndex: Int): Boolean {
        if (!session.playerSideKnown) return true
        val isBlackMove = moveIndex % 2 == 0
        return session.boardFlippedForBlack == isBlackMove
    }

    private fun resolveCriticalIndices(state: com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState): Set<Int> =
        if (session.truthMap.isNotEmpty())
            session.truthMap.filter { it.isCritical || it.isSignificantTacticalMiss }.map { it.moveIndex }.toSet()
        else
            state.criticalMoments.filter { it.type == CriticalMoment.Type.ENGINE_MARKED.name }.map { it.moveIndex }.toSet()

    private fun getCategoryForMove(idx: Int): String =
        if (session.truthMap.isNotEmpty())
            session.truthMap.firstOrNull { it.moveIndex == idx }?.let { motifMapper.motifToReason(it) }
                ?: CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE.name
        else
            session.uiState.value.criticalMoments.firstOrNull { it.moveIndex == idx }?.reasonCategory
                ?: CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE.name
}
