package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side

/** Detects when a rook lifts to the 5th/6th rank (White) or 2nd/3rd rank (Black) for an attack. */
class RookLiftRule : HighlightRule {
    override val ruleType = "rook_lift"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.phase == GamePhase.OPENING) return emptyList()
        if (context.playerDelta < -0.5f) return emptyList()

        val firstChar = context.moveSan.firstOrNull() ?: return emptyList()
        if (firstChar != 'R') return emptyList()

        val destSq = BoardAttackHelper.movedPieceSquare(context.moveSan, context.boardAfter, context.moverColor)
            ?: return emptyList()
        val piece = context.boardAfter.getPiece(destSq)
        if (piece == Piece.NONE || piece.pieceType != PieceType.ROOK) return emptyList()

        val destRank = BoardAttackHelper.rankOf(destSq)

        val isLift = if (context.moverColor == Side.WHITE) destRank in 4..5
                     else destRank in 2..3
        if (!isLift) return emptyList()

        val srcSq = BoardAttackHelper.piecesOf(context.boardBefore, context.moverColor)
            .firstOrNull { (sq, p) ->
                p.pieceType == PieceType.ROOK && context.boardAfter.getPiece(sq) == Piece.NONE
            }?.first

        if (srcSq != null) {
            val srcRank  = BoardAttackHelper.rankOf(srcSq)
            val validLift = if (context.moverColor == Side.WHITE) srcRank < destRank else srcRank > destRank
            if (!validLift) return emptyList()
        }

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
                title          = "Rook lift",
                description    = "$side lifted the rook to $sqName with ${context.moveSan}, preparing an attack.",
                improvementTip = "Rook lifts activate the rook for kingside or central attacks. Look to use this technique when opponent's king is exposed."
            )
        )
    }
}
