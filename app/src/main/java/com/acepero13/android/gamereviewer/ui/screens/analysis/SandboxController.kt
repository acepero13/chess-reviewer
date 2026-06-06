package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.chess.core.engine.StockfishEngine
import com.acepero13.chess.core.util.ChessUtils
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class SandboxController(
    private val session: GameSession,
    private val engine: StockfishEngine,
    private val engineOverlay: EngineOverlayController,
) {

    private val analyzer = SandboxMoveAnalyzer(session, engine, engineOverlay)
    var applyMoveIndexCallback: (suspend (Int) -> Unit)? = null

    fun enterSandboxMode() {
        val fen = session.uiState.value.boardState.fen
        session.sandboxEvalCp = session.truthMap.find { it.fen == fen }?.evalCp
        session.uiState.update {
            it.copy(
                sandboxMode = true, blunderReflectionMode = false,
                reviewMode = com.acepero13.android.gamereviewer.ui.screens.ReviewMode.ANALYSE,
                analyseSubMode = com.acepero13.android.gamereviewer.ui.screens.AnalyseSubMode.EXPLORE,
                boardState = it.boardState.copy(isEditorMode = false),
            )
        }
    }

    fun exitSandboxMode() {
        val idx = session.uiState.value.moveIndex
        session.uiState.update {
            it.copy(
                sandboxMode = false, blunderGuardActive = false, blunderReflectionMode = false,
                sandboxEngineThinking = false,
                analyseSubMode = com.acepero13.android.gamereviewer.ui.screens.AnalyseSubMode.VIEW,
                forcingSequenceMode = false, forcingSequenceAnimating = false,
                forcingSequenceComplete = false, forcingSequencePvMoves = emptyList(),
                forcingSequenceStartFen = "", forcingSequenceCurrentStep = 0,
            )
        }
        session.scope.launch(Dispatchers.Default) { applyMoveIndexCallback?.invoke(idx) }
    }

    fun onSandboxSquareTap(square: Square) {
        if (session.uiState.value.sandboxEngineThinking) return
        if (session.uiState.value.blunderReflectionMode) return
        val cur = session.uiState.value.boardState
        val selected = cur.selectedSquare
        if (selected == null) handleSquareSelection(cur, square)
        else handleMoveAttempt(cur, selected, square)
    }

    fun retryAfterBlunder() {
        val preFen = session.uiState.value.blunderPreMoveFen
        if (preFen.isBlank()) { exitSandboxMode(); return }
        session.uiState.update {
            it.copy(
                blunderGuardActive = false, blunderReflectionMode = false, blunderReflectionInsight = null,
                boardState = it.boardState.copy(
                    fen = preFen, lastMove = null, selectedSquare = null,
                    legalMoves = emptyList(), showingFlash = false,
                ),
            )
        }
    }

    fun continueAfterBlunder() {
        session.uiState.update {
            it.copy(blunderGuardActive = false, blunderReflectionMode = false,
                blunderReflectionInsight = null, boardState = it.boardState.copy(showingFlash = false))
        }
        analyzer.triggerEngineReply(session.uiState.value.boardState.fen)
    }

    fun attemptSandboxMove(preFen: String, move: Move) {
        val postBoard = Board().apply { loadFromFen(preFen) }
        postBoard.doMove(move)
        val postFen = postBoard.fen
        applyMoveOptimistically(move, postFen)
        session.scope.launch(Dispatchers.Default) { analyzer.analyze(preFen, move, postBoard, postFen) }
    }

    private fun handleSquareSelection(cur: com.acepero13.chess.core.ui.board.BoardState, square: Square) {
        val board = Board().apply { loadFromFen(cur.fen) }
        val piece = board.getPiece(square)
        if (piece != Piece.NONE && piece.pieceSide == board.sideToMove) {
            session.uiState.update { it.copy(boardState = cur.copy(selectedSquare = square, legalMoves = board.legalMoves().filter { it.from == square })) }
        }
    }

    private fun handleMoveAttempt(cur: com.acepero13.chess.core.ui.board.BoardState, selected: Square, square: Square) {
        val board = Board().apply { loadFromFen(cur.fen) }
        val move = ChessUtils.buildMove(board, selected, square, solutionUci = null)
        if (board.legalMoves().contains(move)) attemptSandboxMove(cur.fen, move)
        else session.uiState.update { it.copy(boardState = cur.copy(selectedSquare = null, legalMoves = emptyList())) }
    }

    private fun applyMoveOptimistically(move: Move, postFen: String) {
        session.uiState.update {
            it.copy(
                boardState = it.boardState.copy(fen = postFen, lastMove = move, selectedSquare = null,
                    legalMoves = emptyList(), showingFlash = false, arrows = emptyList()),
                sandboxEngineThinking = true, blunderGuardActive = false,
            )
        }
    }
}
