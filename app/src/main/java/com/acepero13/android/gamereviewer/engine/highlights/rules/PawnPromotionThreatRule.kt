package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square

/** Detects when a pawn advances to the 6th or 7th rank, threatening promotion. */
class PawnPromotionThreatRule : HighlightRule {
    override val ruleType = "pawn_promotion_threat"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (!context.isPawnMove) return emptyList()

        val destSq   = BoardAttackHelper.destinationSquare(context.moveSan) ?: return emptyList()
        val destRank = BoardAttackHelper.rankOf(destSq)

        val isThreateningPromotion = if (context.moverColor == Side.WHITE) destRank >= 5
                                      else destRank <= 2
        if (!isThreateningPromotion) return emptyList()

        val piece = context.boardAfter.getPiece(destSq)
        if (piece == Piece.NONE || piece.pieceType != PieceType.PAWN || piece.pieceSide != context.moverColor) return emptyList()

        val side      = if (context.isWhiteMove) "White" else "Black"
        val sqName    = BoardAttackHelper.squareName(destSq)
        val movesAway = if (context.moverColor == Side.WHITE) 7 - destRank else destRank

        return listOf(
            GameHighlight(
                moveIndex      = context.moveIndex,
                moveNumber     = context.moveNumber,
                isWhiteMove    = context.isWhiteMove,
                moveSan        = context.moveSan,
                fenBefore      = context.fenBefore,
                phase          = context.phase,
                ruleType       = ruleType,
                severity       = if (movesAway <= 1) HighlightSeverity.CRITICAL else HighlightSeverity.IMPORTANT,
                title          = "Promotion threat",
                description    = "$side's pawn on $sqName threatens to promote — only $movesAway move${if (movesAway == 1) "" else "s"} away.",
                improvementTip = "Advanced passed pawns are a crucial winning resource. Watch for them and calculate their queening path carefully."
            )
        )
    }
}
