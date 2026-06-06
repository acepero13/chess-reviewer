package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.ui.screens.AnalyseSubMode
import com.acepero13.android.gamereviewer.ui.screens.ReviewMode
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ReviewModeController(
    private val session: GameSession,
    private val engineOverlay: EngineOverlayController,
    private val sandboxController: SandboxController,
) {

    fun enterAnalyseMode() {
        session.uiState.update {
            it.copy(reviewMode = ReviewMode.ANALYSE, analyseSubMode = AnalyseSubMode.VIEW,
                boardState = it.boardState.copy(isEditorMode = false))
        }
    }

    fun setReviewMode(mode: ReviewMode) {
        val needsEditorOff = mode != ReviewMode.ANALYSE ||
            session.uiState.value.analyseSubMode != AnalyseSubMode.EDIT
        session.uiState.update {
            it.copy(reviewMode = mode,
                boardState = it.boardState.copy(
                    isEditorMode = if (needsEditorOff) false else it.boardState.isEditorMode))
        }
    }

    fun setAnalyseSubMode(sub: AnalyseSubMode) {
        if (sub == AnalyseSubMode.EXPLORE) { sandboxController.enterSandboxMode(); return }
        session.uiState.update {
            it.copy(analyseSubMode = sub,
                boardState = it.boardState.copy(isEditorMode = sub == AnalyseSubMode.EDIT))
        }
    }

    fun toggleEvalBar() {
        val newVisible = !session.uiState.value.evalBarVisible
        session.uiState.update { it.copy(evalBarVisible = newVisible) }
        if (newVisible && session.uiState.value.currentEvalCp == null) {
            val fen = session.uiState.value.boardState.fen
            session.scope.launch(Dispatchers.Default) { engineOverlay.fetchEval(fen) }
        }
    }

    fun toggleBestMove() {
        val newVisible = !session.uiState.value.bestMoveVisible
        session.uiState.update { it.copy(bestMoveVisible = newVisible) }
        if (newVisible) {
            val fen = session.uiState.value.boardState.fen
            session.scope.launch(Dispatchers.Default) { engineOverlay.fetchBestMove(fen) }
        } else {
            session.uiState.update { it.copy(boardState = it.boardState.copy(arrows = emptyList())) }
        }
    }

    fun setArrowColor(color: Color) = session.uiState.update { it.copy(currentArrowColor = color) }
}
