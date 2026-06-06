package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.data.db.CriticalMomentDao
import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.chess.core.data.db.PositionAnnotationDao
import com.acepero13.chess.core.data.model.PositionAnnotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class CriticalMomentController(
    private val session: GameSession,
    private val criticalMomentDao: CriticalMomentDao,
    private val annotationDao: PositionAnnotationDao,
    private val treeBuilder: MoveTreeBuilder,
    private val motifMapper: MotifMapper,
) {

    fun markCurrentAsCritical() {
        session.uiState.update { it.copy(showCriticalSheet = true) }
    }

    fun dismissCriticalSheet() {
        session.uiState.update { it.copy(showCriticalSheet = false) }
    }

    fun saveCriticalAnswers(plan: String, threats: String, candidates: String) {
        val idx = session.uiState.value.moveIndex
        val fen = session.uiState.value.boardState.fen
        val comment = buildSelfAnalysisComment(plan, threats, candidates)
        session.scope.launch(Dispatchers.IO) {
            persistCommentToAnnotation(fen, comment)
            insertCriticalMoment(idx, fen, comment)
        }
    }

    private fun buildSelfAnalysisComment(plan: String, threats: String, candidates: String) =
        buildString {
            appendLine("### Self-Analysis")
            if (plan.isNotBlank())       appendLine("**My plan:** $plan")
            if (threats.isNotBlank())    appendLine("**Threats I see:** $threats")
            if (candidates.isNotBlank()) appendLine("**Candidates:** $candidates")
        }.trim()

    private suspend fun persistCommentToAnnotation(fen: String, comment: String) {
        val upd = (session.annotationCache[fen] ?: PositionAnnotation(fen = fen))
            .copy(moveComment = comment)
        annotationDao.upsert(upd)
        session.annotationCache[fen] = upd
    }

    private suspend fun insertCriticalMoment(idx: Int, fen: String, comment: String) {
        val truthEntry     = session.truthMap.find { it.moveIndex == idx }
        val reasonCategory = truthEntry?.let { motifMapper.motifToReason(it) }
            ?: CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE.name
        val severity       = truthEntry?.let { kotlin.math.abs(it.playerEvalDelta) } ?: 0
        criticalMomentDao.insert(
            CriticalMoment(
                gameId = session.gameId, moveIndex = idx,
                type = CriticalMoment.Type.USER_MARKED.name, severity = severity,
                reasonCategory = reasonCategory,
                explanationState = CriticalMoment.ExplanationState.HIDDEN.name, fen = fen,
            )
        )
        session.uiState.update {
            it.copy(showCriticalSheet = false, currentComment = comment, hasAnnotationAtCurrent = true)
        }
        treeBuilder.refreshTreeItems()
    }
}
