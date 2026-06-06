package com.acepero13.android.gamereviewer.ui.screens.analysis

import android.util.Log
import com.acepero13.android.gamereviewer.ui.screens.AnalyseSubMode
import com.acepero13.android.gamereviewer.ui.screens.ReviewMode
import com.acepero13.chess.core.data.model.PositionAnnotation
import com.acepero13.chess.core.ui.board.BoardState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "BoardStateApplicator"

internal class BoardStateApplicator(
    private val session: GameSession,
    private val treeBuilder: MoveTreeBuilder,
    private val engineOverlay: EngineOverlayController,
) {
    var onFenChanged: ((String) -> Unit)? = null

    fun applyMoveIndex(index: Int) {
        val fen      = session.fenSequence.getOrElse(index) { session.startFen }
        val annot    = session.annotationCache[fen]
        val comment  = resolveComment(index, annot)
        val panels   = computePanelFlags(index)
        session.uiState.update { s ->
            s.copy(
                moveIndex = index, boardState = buildBoardState(index, fen, annot),
                currentComment = comment,
                hasAnnotationAtCurrent = hasAnnotation(annot, comment),
                treeItems = treeBuilder.buildTreeItems(index),
                blunderGuardActive = false,
                mentorAvailable = index > 0 && session.uciMoves.isNotEmpty(),
                currentEvalCp = session.truthMap.find { it.moveIndex == index }?.evalCp,
                showOpeningDeviationPanel = panels.showDeviation,
                showEndgameRecognitionPanel = panels.showEndgame,
                showMiddlegamePlanPanel = panels.showMiddlegame,
                showProactiveCoaching = false, activeProactiveTrigger = null,
                proactiveInteractiveMode = false, proactiveAnswerFeedback = null,
                proactiveAnswerIsCorrect = null, coachHighlightSquares = emptyList(),
                proactiveHangingSquares = emptyList(), proactiveHangingOwnSquares = emptySet(),
                proactiveFoundSquares = emptySet(), showCalibrationPanel = false,
                calibrationTrigger = null, calibrationUserValue = 0,
                calibrationLocked = false, calibrationFeedback = "",
                calibrationFeedbackPositive = false,
            )
        }
        Log.d(TAG, "applyMoveIndex($index): fen=${fen.take(40)}")
        checkPostNavigationEffects(index)
        maybeRefreshEngineOverlays(fen)
        onFenChanged?.invoke(fen)
    }

    private fun buildBoardState(index: Int, fen: String, annot: PositionAnnotation?): BoardState {
        val lastMove   = if (index > 0) UciMoveUtils.uciToMoveFromFens(session.uciMoves, session.fenSequence, session.startFen, index) else null
        val arrows     = annot?.arrowsJson?.let { AnnotationParser.parseArrows(session.gson, it) } ?: emptyList()
        val marks      = annot?.markedSquaresJson?.let { AnnotationParser.parseMarks(session.gson, it) } ?: emptyList()
        val live       = session.uiState.value
        val inEditMode = live.reviewMode == ReviewMode.ANALYSE && live.analyseSubMode == AnalyseSubMode.EDIT
        return live.boardState.copy(
            fen = fen, lastMove = lastMove, selectedSquare = null, legalMoves = emptyList(),
            userArrows = arrows, markedSquares = marks, arrows = emptyList(),
            isEditorMode = inEditMode, showingFlash = false,
            flippedForBlack = session.boardFlippedForBlack,
        )
    }

    private fun computePanelFlags(index: Int): PanelFlags {
        val live = session.uiState.value
        val inNav = live.reviewMode == ReviewMode.NAVIGATE
        val showDev = inNav && !live.openingDeviationDismissed
            && live.openingDeviation?.moveIndex == index && index > 0
        return PanelFlags(
            showDeviation  = showDev,
            showEndgame    = inNav && !live.endgamePanelDismissed
                && live.endgameClassification?.firstEndgameMoveIndex == index && index > 0,
            showMiddlegame = inNav && !live.middlegamePlanPanelDismissed && !showDev
                && live.middlegamePlanClassification?.moveIndex == index && index > 0,
        )
    }

    private fun resolveComment(index: Int, annot: PositionAnnotation?): String {
        val userComment = annot?.moveComment ?: ""
        return userComment.ifBlank { session.pgnAnnotations[index - 1] ?: "" }
    }

    private fun hasAnnotation(annot: PositionAnnotation?, comment: String): Boolean =
        comment.isNotBlank() || (annot?.arrowsJson?.length ?: 0) > 2 || (annot?.markedSquaresJson?.length ?: 0) > 2

    private fun checkPostNavigationEffects(index: Int) {
        val st = session.uiState.value
        if (index == session.uciMoves.size && st.isBackgroundAnalysisDone && !st.showPostGameDebrief) {
            session.uiState.update { it.copy(showPostGameDebrief = true, gameStoryUnlocked = true) }
        }
    }

    private fun maybeRefreshEngineOverlays(fen: String) {
        val s = session.uiState.value
        if (s.reviewMode != ReviewMode.ANALYSE) return
        if (s.evalBarVisible && s.currentEvalCp == null) {
            session.scope.launch(Dispatchers.Default) { engineOverlay.fetchEval(fen) }
        }
        if (s.bestMoveVisible) {
            session.scope.launch(Dispatchers.Default) { engineOverlay.fetchBestMove(fen) }
        }
    }

    private data class PanelFlags(
        val showDeviation: Boolean, val showEndgame: Boolean, val showMiddlegame: Boolean,
    )
}
