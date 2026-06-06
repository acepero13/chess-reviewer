package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.chess.core.data.model.ChessConstants
import com.acepero13.chess.core.engine.StockfishEngine
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ForcingSequenceController(
    private val session: GameSession,
    private val engine: StockfishEngine,
    private val sandboxController: SandboxController,
) {

    fun enterForcingSequenceMode() {
        val state = session.uiState.value
        val pvMoves = parsePvMoves(state.moveIndex)
        session.uiState.update {
            it.copy(
                forcingSequenceMode = true, forcingSequencePvMoves = pvMoves,
                forcingSequenceStartFen = state.boardState.fen,
                forcingSequenceCurrentStep = 0, forcingSequenceComplete = false,
                forcingSequenceAnimating = false,
            )
        }
        sandboxController.enterSandboxMode()
    }

    fun showForcingSequence() {
        val state = session.uiState.value
        val startFen = state.forcingSequenceStartFen.ifBlank { state.boardState.fen }
        session.uiState.update { it.copy(showProactiveCoaching = true) }
        session.scope.launch(Dispatchers.Default) {
            val pvMoves = resolveOrFetchPvMoves(state, startFen)
            if (pvMoves.isEmpty()) {
                session.uiState.update { it.copy(showProactiveCoaching = false) }
                return@launch
            }
            session.uiState.update { it.copy(forcingSequencePvMoves = pvMoves, forcingSequenceStartFen = startFen) }
            animatePvMoves(startFen, pvMoves)
        }
    }

    fun replayForcingSequence() {
        session.uiState.update {
            it.copy(forcingSequenceCurrentStep = 0, forcingSequenceComplete = false)
        }
        showForcingSequence()
    }

    fun exitForcingSequenceMode() = sandboxController.exitSandboxMode()

    private fun parsePvMoves(moveIndex: Int): List<String> {
        val pvLine = session.truthMap.find { it.moveIndex == moveIndex }?.pvLine ?: ""
        return pvLine.split(",").filter { it.isNotBlank() }
    }

    private suspend fun resolveOrFetchPvMoves(
        state: com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState,
        startFen: String,
    ): List<String> {
        val cached = state.forcingSequencePvMoves.ifEmpty { parsePvMoves(state.moveIndex) }
        if (cached.isNotEmpty()) return cached
        return runCatching {
            engine.analyzePosition(startFen, ChessConstants.DEFAULT_ANALYSIS_DEPTH)
        }.getOrNull()?.pv?.take(ChessConstants.MAX_FORCING_SEQUENCE_DEPTH)
            ?.filter { it.isNotBlank() } ?: emptyList()
    }

    private suspend fun animatePvMoves(startFen: String, pvMoves: List<String>) {
        session.uiState.update {
            it.copy(
                forcingSequenceAnimating = true, forcingSequenceCurrentStep = 0,
                boardState = it.boardState.copy(fen = startFen, selectedSquare = null, legalMoves = emptyList()),
            )
        }
        val board = Board().apply { loadFromFen(startFen) }
        pvMoves.take(ChessConstants.MAX_FORCING_SEQUENCE_DEPTH).forEachIndexed { index, uci ->
            val move = parseUciMove(board, uci) ?: return@forEachIndexed
            board.doMove(move)
            session.uiState.update { st ->
                st.copy(
                    boardState = st.boardState.copy(fen = board.fen, lastMove = move),
                    forcingSequenceCurrentStep = index + 1,
                )
            }
            delay(ChessConstants.FORCING_SEQUENCE_STEP_DELAY_MS)
        }
        session.uiState.update { it.copy(forcingSequenceAnimating = false, forcingSequenceComplete = true) }
    }

    private fun parseUciMove(board: Board, uci: String): Move? {
        if (uci.length < 4) return null
        val from = runCatching { Square.valueOf(uci.substring(0, 2).uppercase()) }.getOrNull() ?: return null
        val to = runCatching { Square.valueOf(uci.substring(2, 4).uppercase()) }.getOrNull() ?: return null
        if (uci.length != 5) return Move(from, to)
        val side = board.sideToMove
        val prom = when (uci[4].lowercaseChar()) {
            'r' -> if (side == Side.WHITE) Piece.WHITE_ROOK else Piece.BLACK_ROOK
            'b' -> if (side == Side.WHITE) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
            'n' -> if (side == Side.WHITE) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
            else -> if (side == Side.WHITE) Piece.WHITE_QUEEN else Piece.BLACK_QUEEN
        }
        return Move(from, to, prom)
    }
}
