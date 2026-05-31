package com.acepero13.android.gamereviewer.domain

import com.acepero13.chess.core.opening.OpeningClassifier
import com.acepero13.chess.core.opening.OpeningEntry
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

private const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

/** Maximum half-moves to scan for a deviation — roughly move 15 per side. */
private const val MAX_OPENING_HALF_MOVES = 30

/**
 * Describes the first position where the player left opening theory.
 *
 * @param moveIndex       1-based index of the deviation move (e.g. 9 = 5th Black move)
 * @param movePlayedUci   UCI string of the deviation move (e.g. "g8f6")
 * @param movePlayedSan   SAN notation for display (e.g. "Nf6")
 * @param openingName     The last matched opening name before the deviation (e.g. "Sicilian Defense")
 * @param openingEco      ECO code of that opening (e.g. "B20")
 * @param theoreticalDepth How many half-moves were still in the opening book before this one
 */
data class OpeningDeviation(
    val moveIndex: Int,
    val movePlayedUci: String,
    val movePlayedSan: String,
    val openingName: String,
    val openingEco: String,
    val theoreticalDepth: Int,
) {
    val moveNumber: Int get() = (moveIndex + 1) / 2
    val isWhiteMove: Boolean get() = moveIndex % 2 == 1
    val moveLabel: String get() {
        val suffix = if (isWhiteMove) "." else "..."
        return "$moveNumber$suffix $movePlayedSan"
    }
}

/**
 * Detects the first move in a game where the player left opening theory.
 *
 * Uses [OpeningClassifier.classify] (O(1) EPD lookup) at each position to find
 * the exact half-move where the game departed from the ECO database.
 */
class OpeningDeviationAnalyzer(private val classifier: OpeningClassifier) {

    /**
     * Finds the first deviation from opening theory within the first [MAX_OPENING_HALF_MOVES]
     * half-moves.
     *
     * Returns null when:
     * - the game stays in book all the way through the opening phase
     * - the opening moves are empty or cannot be parsed
     */
    fun analyze(uciMoves: List<String>, sanMoves: List<String>): OpeningDeviation? {
        val movesToCheck = uciMoves.take(MAX_OPENING_HALF_MOVES)
        if (movesToCheck.isEmpty()) return null

        val board = Board()
        board.loadFromFen(START_FEN)

        // If even the starting position isn't in the DB, the index failed to load — bail out.
        var lastEntry: OpeningEntry = classifier.classify(board.fen) ?: return null
        var lastInBookIndex = 0

        for ((i, uci) in movesToCheck.withIndex()) {
            val move    = uciToMove(board, uci) ?: break
            val applied = runCatching { board.doMove(move) }.getOrElse { false }
            if (!applied) break

            val entry = classifier.classify(board.fen)
            if (entry != null) {
                lastEntry       = entry
                lastInBookIndex = i + 1
            } else {
                // i+1 is the 1-based move index of the first out-of-book position
                return OpeningDeviation(
                    moveIndex        = i + 1,
                    movePlayedUci    = uci,
                    movePlayedSan    = sanMoves.getOrElse(i) { uci },
                    openingName      = lastEntry.name,
                    openingEco       = lastEntry.eco,
                    theoreticalDepth = lastInBookIndex,
                )
            }
        }
        return null
    }

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
