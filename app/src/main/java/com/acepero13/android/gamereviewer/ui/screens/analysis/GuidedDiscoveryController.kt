package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.data.db.CriticalMomentDao
import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.domain.InsightReconciler
import com.acepero13.chess.core.data.db.PositionAnnotationDao
import com.acepero13.chess.core.data.model.ChessConstants
import com.acepero13.chess.core.data.model.PositionAnnotation
import com.acepero13.chess.core.engine.StockfishEngine
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class GuidedDiscoveryController(
    private val session: GameSession,
    private val engine: StockfishEngine,
    private val annotationDao: PositionAnnotationDao,
    private val criticalMomentDao: CriticalMomentDao,
    private val navigation: NavigationController,
    private val treeBuilder: MoveTreeBuilder,
    private val quizController: ClassificationQuizController,
) {

    fun enterGuidedDiscovery(moveIndex: Int) {
        val clamped = moveIndex.coerceIn(0, session.uciMoves.size)
        val moment = session.uiState.value.criticalMoments
            .firstOrNull { it.moveIndex == clamped && it.type == CriticalMoment.Type.ENGINE_MARKED.name }
        val insight = InsightReconciler.forReason(moment?.toReason() ?: CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE)
        session.scope.launch(Dispatchers.Default) {
            navigation.applyMoveIndex(clamped)
            session.uiState.update {
                it.copy(
                    guidedDiscoveryMode = true, guidedDiscoveryInsight = insight,
                    guidedDiscoveryCriticalMoment = moment, guidedDiscoveryThoughts = "",
                    guidedDiscoveryHintVisible = false, guidedDiscoveryAnswerRevealed = false,
                    guidedDiscoveryRevealedEvalCp = null, guidedDiscoveryEngineThinking = false,
                    boardState = it.boardState.copy(isEditorMode = false, arrows = emptyList()),
                    showMissedMomentBanner = false,
                )
            }
        }
    }

    fun exitGuidedDiscovery() {
        val idx = session.uiState.value.moveIndex
        session.uiState.update {
            it.copy(
                guidedDiscoveryMode = false, guidedDiscoveryInsight = null,
                guidedDiscoveryCriticalMoment = null, guidedDiscoveryAnswerRevealed = false,
                guidedDiscoveryHintVisible = false, guidedDiscoveryRevealedEvalCp = null,
            )
        }
        session.scope.launch(Dispatchers.Default) { navigation.applyMoveIndex(idx) }
    }

    fun updateGuidedThoughts(text: String) = session.uiState.update { it.copy(guidedDiscoveryThoughts = text) }

    fun revealGuidedHint() {
        session.uiState.update { it.copy(guidedDiscoveryHintVisible = true, guidedDiscoveryInsightRevealed = true) }
        session.scope.launch(Dispatchers.IO) {
            val moment = session.uiState.value.guidedDiscoveryCriticalMoment ?: return@launch
            criticalMomentDao.update(moment.copy(explanationState = CriticalMoment.ExplanationState.HINTED.name))
        }
    }

    fun revealGuidedAnswer() {
        val fen = session.uiState.value.boardState.fen
        val thoughts = session.uiState.value.guidedDiscoveryThoughts
        session.uiState.update { it.copy(guidedDiscoveryEngineThinking = true) }
        session.scope.launch(Dispatchers.Default) {
            val result = runCatching { engine.analyzePosition(fen, ChessConstants.DEFAULT_ANALYSIS_DEPTH) }.getOrNull()
            val engineArrow = result?.toArrow()
            val evalCp = result?.toWhitePerspective(Board().apply { loadFromFen(fen) })
            if (thoughts.isNotBlank()) launch(Dispatchers.IO) { persistThoughts(fen, thoughts) }
            val moment = session.uiState.value.guidedDiscoveryCriticalMoment
            if (moment != null) launch(Dispatchers.IO) {
                criticalMomentDao.update(moment.copy(explanationState = CriticalMoment.ExplanationState.REVEALED.name))
            }
            session.uiState.update { st ->
                st.copy(
                    guidedDiscoveryEngineThinking = false, guidedDiscoveryAnswerRevealed = true,
                    guidedDiscoveryRevealedEvalCp = evalCp, guidedDiscoveryInsightRevealed = true,
                    boardState = st.boardState.copy(arrows = if (engineArrow != null) listOf(engineArrow) else emptyList()),
                )
            }
            if (!session.uiState.value.showClassificationQuiz) quizController.triggerClassificationQuiz()
        }
    }

    fun submitGuidedThoughts(
        advanceMentorSession: () -> Unit,
        exitMentorMode: () -> Unit,
    ) {
        val thoughts = session.uiState.value.guidedDiscoveryThoughts
        val fen = session.uiState.value.boardState.fen
        if (thoughts.isNotBlank()) {
            session.scope.launch(Dispatchers.IO) { persistThoughts(fen, thoughts) }
        }
        if (session.uiState.value.mentorSessionQueue.isNotEmpty()) advanceMentorSession()
        else exitMentorMode()
    }

    private suspend fun persistThoughts(fen: String, thoughts: String) {
        val existing = session.annotationCache[fen] ?: annotationDao.getByFen(fen)
        val newComment = buildString {
            val prev = existing?.moveComment?.takeIf { it.isNotBlank() }
            if (prev != null) { appendLine(prev); appendLine() }
            append("### Guided Discovery Notes\n")
            appendLine(thoughts)
        }.trim()
        val upd = (existing ?: PositionAnnotation(fen = fen)).copy(moveComment = newComment)
        annotationDao.upsert(upd)
        session.annotationCache[fen] = upd
        session.uiState.update { it.copy(currentComment = newComment) }
        treeBuilder.refreshTreeItems()
    }
}
