package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.ui.screens.analysis.UciMoveUtils
import com.github.bhlangonijr.chesslib.Board

private const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

/** Half-move index around which the middlegame structure has usually formed. */
private const val MIDDLEGAME_ENTRY_PLY = 20

/**
 * Labels a game by its dominant middlegame **pawn structure** (IQP, Hanging Pawns, …) using the
 * chess-core [com.acepero13.chess.core.middlegame.MiddlegamePlanClassifier] (via the injected
 * [MiddlegamePlanDetector]). Replays the game's UCI moves locally — no engine cost.
 */
class PawnStructureTagger(private val detector: MiddlegamePlanDetector) {

    /** @return the top pawn-structure title, or "" when nothing distinctive is detected. */
    fun tag(uciMoves: List<String>, isWhite: Boolean): String {
        if (uciMoves.isEmpty()) return ""
        val fens = buildFens(uciMoves)
        val classification = detector.detect(fens, isWhite, startFromIndex = MIDDLEGAME_ENTRY_PLY)
        return classification?.plans?.firstOrNull()?.title ?: ""
    }

    /** FEN per position: index 0 = start, index i = after i half-moves (matches MiddlegamePlanDetector). */
    private fun buildFens(uciMoves: List<String>): List<String> {
        val fens = mutableListOf(START_FEN)
        val board = Board().apply { loadFromFen(START_FEN) }
        for (uci in uciMoves) {
            val move = UciMoveUtils.uciToMove(board, uci) ?: break
            if (!board.doMove(move)) break
            fens.add(board.fen)
        }
        return fens
    }
}
