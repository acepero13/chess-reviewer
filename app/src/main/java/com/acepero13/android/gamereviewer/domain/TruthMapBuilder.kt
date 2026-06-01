package com.acepero13.android.gamereviewer.domain

import com.acepero13.chess.core.engine.EngineResult
import com.acepero13.chess.core.engine.MotifClassifier
import com.acepero13.chess.core.engine.StockfishEngine
import com.acepero13.chess.core.data.model.ChessConstants
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

private const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

/**
 * Silently analyzes every position in a game and produces the hidden [TruthMapEntry] list.
 *
 * **Never** surface the resulting data directly to the user — it must only power the
 * Insight Reconciliation System (missed-moment detection, blunder guard, etc.).
 */
class TruthMapBuilder(private val engine: StockfishEngine) {

    /**
     * Runs full game analysis.
     *
     * @param uciMoves  Space-separated or list of UCI moves from the starting position.
     * @param onProgress Called with (processedCount, total) after each position finishes.
     *                   Suitable for driving a hidden progress indicator.
     * @return List of [TruthMapEntry], one per half-move.
     */
    suspend fun build(
        uciMoves: List<String>,
        onProgress: ((processed: Int, total: Int) -> Unit)? = null,
    ): List<TruthMapEntry> {
        val entries = mutableListOf<TruthMapEntry>()
        val board = Board()
        board.loadFromFen(START_FEN)

        // Baseline eval at the starting position
        val startResult = runCatching {
            engine.analyzePosition(board.fen, ChessConstants.DEFAULT_ANALYSIS_DEPTH)
        }.getOrNull()
        var prevEvalCp = startResult?.toWhitePerspective(board) ?: 0

        uciMoves.forEachIndexed { idx, uci ->
            val move = uciToMove(board, uci) ?: run {
                onProgress?.invoke(idx + 1, uciMoves.size)
                return@forEachIndexed
            }
            board.doMove(move)

            val result = runCatching {
                engine.analyzePosition(board.fen, ChessConstants.DEFAULT_ANALYSIS_DEPTH)
            }.getOrNull()

            val evalCp = result?.toWhitePerspective(board) ?: prevEvalCp
            val evalDelta = evalCp - prevEvalCp
            val motif = result?.let { MotifClassifier.classify(it, board.fen) } ?: "mixed"
            val pvLine = result?.pv
                ?.take(ChessConstants.MAX_FORCING_SEQUENCE_DEPTH)
                ?.joinToString(",")
                ?: ""

            entries.add(
                TruthMapEntry(
                    moveIndex = idx + 1,   // 1-based
                    fen       = board.fen,
                    evalCp    = evalCp,
                    evalDelta = evalDelta,
                    motif     = motif,
                    pvLine    = pvLine,
                )
            )
            prevEvalCp = evalCp
            onProgress?.invoke(idx + 1, uciMoves.size)
        }
        return entries
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun uciToMove(board: Board, uci: String): Move? {
        if (uci.length < 4) return null
        return runCatching {
            val from = Square.valueOf(uci.substring(0, 2).uppercase())
            val to   = Square.valueOf(uci.substring(2, 4).uppercase())
            if (uci.length == 5) {
                val side = board.sideToMove
                val prom = when (uci[4].lowercaseChar()) {
                    'r' -> if (side == Side.WHITE) Piece.WHITE_ROOK   else Piece.BLACK_ROOK
                    'b' -> if (side == Side.WHITE) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
                    'n' -> if (side == Side.WHITE) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
                    else -> if (side == Side.WHITE) Piece.WHITE_QUEEN  else Piece.BLACK_QUEEN
                }
                Move(from, to, prom)
            } else {
                Move(from, to)
            }
        }.getOrNull()
    }
}

/**
 * Converts Stockfish's side-to-move score into a White-perspective centipawn value.
 *
 * After a move is played, the board's [Board.sideToMove] is the *next* player.
 * Stockfish reports score from the side that will move next.
 * For White's perspective: if it's Black's turn, negate (Black's +100 = White's -100).
 */
private fun EngineResult.toWhitePerspective(board: Board): Int =
    if (board.sideToMove == Side.WHITE) score else -score
