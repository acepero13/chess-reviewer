package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side

/**
 * Detects when a knight lands on an outpost square — a central square that cannot
 * be attacked by any enemy pawn.
 */
class KnightOutpostRule : HighlightRule {
    override val ruleType = "knight_outpost"

    companion object {
        // Central outpost squares: c4,d4,e4,f4,c5,d5,e5,f5
        private val OUTPOST_SQUARES_WHITE = setOf(
            2 + 3 * 8,  // c4
            3 + 3 * 8,  // d4
            4 + 3 * 8,  // e4
            5 + 3 * 8,  // f4
            2 + 4 * 8,  // c5
            3 + 4 * 8,  // d5
            4 + 4 * 8,  // e5
            5 + 4 * 8   // f5
        )
        private val OUTPOST_SQUARES_BLACK = setOf(
            2 + 2 * 8,  // c3
            3 + 2 * 8,  // d3
            4 + 2 * 8,  // e3
            5 + 2 * 8,  // f3
            2 + 3 * 8,  // c4
            3 + 3 * 8,  // d4
            4 + 3 * 8,  // e4
            5 + 3 * 8   // f4
        )
    }

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.phase == GamePhase.OPENING) return emptyList()
        if (context.playerDelta < -0.5f) return emptyList()
        if (context.moveSan.startsWith("O-O")) return emptyList()

        val outpostSquares = if (context.isWhiteMove) OUTPOST_SQUARES_WHITE else OUTPOST_SQUARES_BLACK

        val destSq = BoardAttackHelper.movedPieceSquare(context.moveSan, context.boardAfter, context.moverColor)
            ?: return emptyList()
        val piece = context.boardAfter.getPiece(destSq)
        if (piece == Piece.NONE || piece.pieceType != PieceType.KNIGHT) return emptyList()
        if (destSq.ordinal !in outpostSquares) return emptyList()

        val enemyPawnDir = if (context.moverColor == Side.WHITE) -1 else 1
        val file = BoardAttackHelper.fileOf(destSq)
        val rank = BoardAttackHelper.rankOf(destSq)

        val leftAttackSq  = BoardAttackHelper.squareAt(file - 1, rank + enemyPawnDir)
        val rightAttackSq = BoardAttackHelper.squareAt(file + 1, rank + enemyPawnDir)

        val attackedByPawn = (leftAttackSq != null && run {
            val p = context.boardAfter.getPiece(leftAttackSq)
            p != Piece.NONE && p.pieceType == PieceType.PAWN && p.pieceSide == context.enemyColor
        }) || (rightAttackSq != null && run {
            val p = context.boardAfter.getPiece(rightAttackSq)
            p != Piece.NONE && p.pieceType == PieceType.PAWN && p.pieceSide == context.enemyColor
        })

        if (attackedByPawn) return emptyList()

        val squareName = BoardAttackHelper.squareName(destSq)
        val side       = if (context.isWhiteMove) "White" else "Black"

        return listOf(
            GameHighlight(
                moveIndex      = context.moveIndex,
                moveNumber     = context.moveNumber,
                isWhiteMove    = context.isWhiteMove,
                moveSan        = context.moveSan,
                fenBefore      = context.fenBefore,
                phase          = context.phase,
                ruleType       = ruleType,
                severity       = HighlightSeverity.NOTABLE,
                title          = "Knight outpost",
                description    = "$side established a knight outpost on $squareName with ${context.moveSan}.",
                improvementTip = "Outpost knights are powerful. A knight on a central square that can't be chased by enemy pawns is a long-term asset."
            )
        )
    }
}
