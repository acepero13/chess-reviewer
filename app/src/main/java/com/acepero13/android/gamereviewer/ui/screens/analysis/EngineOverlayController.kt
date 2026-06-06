package com.acepero13.android.gamereviewer.ui.screens.analysis

import android.util.Log
import com.acepero13.chess.core.data.model.ChessConstants
import com.acepero13.chess.core.engine.EngineResult
import com.acepero13.chess.core.engine.StockfishEngine
import com.acepero13.chess.core.ui.board.Arrow
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import kotlinx.coroutines.flow.update

private const val TAG = "EngineOverlay"

internal class EngineOverlayController(
    private val session: GameSession,
    private val engine: StockfishEngine,
) {

    suspend fun fetchEval(fen: String) {
        val result = runCatching {
            engine.analyzePosition(fen, depth = ChessConstants.DEFAULT_ANALYSIS_DEPTH)
        }.getOrNull() ?: run { Log.w(TAG, "fetchEval: null for ${fen.take(40)}"); return }
        val evalCp = result.toWhitePerspective(Board().apply { loadFromFen(fen) })
        if (session.uiState.value.boardState.fen == fen && session.uiState.value.evalBarVisible) {
            session.uiState.update { it.copy(currentEvalCp = evalCp) }
        }
    }

    suspend fun fetchBestMove(fen: String) {
        val result = runCatching { engine.analyzePosition(fen, depth = 10) }.getOrNull()
            ?: run { Log.w(TAG, "fetchBestMove: null for ${fen.take(40)}"); return }
        val arrow = result.toArrow()
        if (session.uiState.value.boardState.fen == fen && session.uiState.value.bestMoveVisible) {
            session.uiState.update { s ->
                s.copy(boardState = s.boardState.copy(arrows = if (arrow != null) listOf(arrow) else emptyList()))
            }
        }
    }
}

internal fun EngineResult.toWhitePerspective(board: Board): Int =
    if (board.sideToMove == Side.WHITE) score else -score

internal fun EngineResult.toArrow(): Arrow? {
    val uci = bestMoveUci
    if (uci.length < 4) return null
    return runCatching {
        Arrow(
            from  = Square.valueOf(uci.substring(0, 2).uppercase()),
            to    = Square.valueOf(uci.substring(2, 4).uppercase()),
            color = androidx.compose.ui.graphics.Color(0xCC4CAF50.toInt()),
        )
    }.getOrNull()
}
