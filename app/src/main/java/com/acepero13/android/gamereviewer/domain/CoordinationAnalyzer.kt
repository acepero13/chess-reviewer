package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.engine.highlights.BoardAttackHelper
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.MoveGenerator

/**
 * Heuristic coordination metrics used by [CoachingTriggerEvaluator] to detect
 * [CoachingTrigger.CoordinatedAttack] and [CoachingTrigger.PieceHarmony] events.
 *
 * All methods are pure: they read the board but never mutate it.
 */
object CoordinationAnalyzer {

    /**
     * Count of unique non-pawn pieces of [attackingSide] that can legally move to
     * the opponent's king zone (king square + 8 adjacent squares).
     *
     * Score ≥ 3 indicates a coordinated king attack.
     */
    fun kingAttackScore(board: Board, attackingSide: Side): Int {
        val defendingSide = if (attackingSide == Side.WHITE) Side.BLACK else Side.WHITE
        val kingSquare    = BoardAttackHelper.kingSquare(board, defendingSide) ?: return 0

        val kingFile = BoardAttackHelper.fileOf(kingSquare)
        val kingRank = BoardAttackHelper.rankOf(kingSquare)
        val kingZone = mutableSetOf<Square>().apply {
            add(kingSquare)
            for (df in -1..1) for (dr in -1..1) {
                if (df == 0 && dr == 0) continue
                BoardAttackHelper.squareAt(kingFile + df, kingRank + dr)?.let { add(it) }
            }
        }

        // Collect unique non-pawn attacker squares across all king-zone squares.
        val attackers = mutableSetOf<Square>()
        for (target in kingZone) {
            BoardAttackHelper.attackersOf(board, target, attackingSide).forEach { sq ->
                val p = board.getPiece(sq)
                if (p != Piece.NONE && p.pieceType != PieceType.PAWN) attackers.add(sq)
            }
        }
        return attackers.size
    }

    /**
     * General piece coordination score for [side] — counts squares that are
     * targeted by two or more of [side]'s non-pawn, non-king pieces simultaneously.
     *
     * Higher = more pieces working toward shared targets = better coordination.
     */
    fun generalCoordinationScore(board: Board, side: Side): Int {
        val effectiveBoard = boardForSide(board, side)
        val allMoves = runCatching {
            MoveGenerator.generateLegalMoves(effectiveBoard)
        }.getOrDefault(emptyList())

        val candidateTypes = setOf(PieceType.KNIGHT, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN)
        val pieces = BoardAttackHelper.piecesOf(board, side)
            .filter { (_, p) -> p.pieceType in candidateTypes }
            .map    { (sq, _) -> sq }

        if (pieces.size < 2) return 0

        val attackSets = pieces.map { sq ->
            allMoves.filter { it.from == sq }.map { it.to }.toSet()
        }

        val allTargets = attackSets.flatten().toSet()
        return allTargets.count { target ->
            attackSets.count { set -> target in set } >= 2
        }
    }

    private fun boardForSide(board: Board, side: Side): Board {
        if (board.sideToMove == side) return board
        val parts = board.fen.split(" ")
        if (parts.size < 2) return board
        val flipped = parts.toMutableList()
            .apply { set(1, if (parts[1] == "w") "b" else "w") }
            .joinToString(" ")
        return runCatching { Board().apply { loadFromFen(flipped) } }.getOrElse { board }
    }
}
