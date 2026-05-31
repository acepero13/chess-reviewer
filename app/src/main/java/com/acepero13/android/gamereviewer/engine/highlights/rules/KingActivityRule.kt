package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side

/** Detects when the king becomes active in the endgame by advancing toward the center. */
class KingActivityRule : HighlightRule {
    override val ruleType = "king_activity"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.phase != GamePhase.ENDGAME) return emptyList()
        if (context.playerDelta < -0.5f) return emptyList()
        if (context.moveSan.startsWith("O-O")) return emptyList()

        if (context.moveSan.firstOrNull() != 'K') return emptyList()

        val destSq = BoardAttackHelper.movedPieceSquare(context.moveSan, context.boardAfter, context.moverColor)
            ?: return emptyList()
        val piece = context.boardAfter.getPiece(destSq)
        if (piece == Piece.NONE || piece.pieceType != PieceType.KING) return emptyList()

        val destRank = BoardAttackHelper.rankOf(destSq)
        val destFile = BoardAttackHelper.fileOf(destSq)

        val isActive = destRank in 2..5 && destFile in 1..6
        if (!isActive) return emptyList()

        val srcSq = BoardAttackHelper.piecesOf(context.boardBefore, context.moverColor)
            .firstOrNull { (_, p) -> p.pieceType == PieceType.KING }?.first ?: return emptyList()
        val srcRank = BoardAttackHelper.rankOf(srcSq)

        val towardCenter = if (context.moverColor == Side.WHITE) destRank > srcRank
                          else destRank < srcRank
        if (!towardCenter) return emptyList()

        val side   = if (context.isWhiteMove) "White" else "Black"
        val sqName = BoardAttackHelper.squareName(destSq)
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
                title          = "Active king",
                description    = "$side activated the king to $sqName — a key endgame technique.",
                improvementTip = "In the endgame, activate your king. It becomes a powerful piece and should march toward the center or the passed pawn."
            )
        )
    }
}
