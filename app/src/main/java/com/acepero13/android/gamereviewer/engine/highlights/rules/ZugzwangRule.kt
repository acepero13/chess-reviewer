package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Square

/**
 * Detects simplified endgame positions where the side to move is in zugzwang.
 */
class ZugzwangRule : HighlightRule {
    override val ruleType = "zugzwang"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.phase != GamePhase.ENDGAME) return emptyList()
        if (context.isCapture) return emptyList()

        val nonPawnPieces = Square.values().filter { it != Square.NONE }.count { sq ->
            val p = context.boardBefore.getPiece(sq)
            p != Piece.NONE && p.pieceType != PieceType.KING && p.pieceType != PieceType.PAWN
        }
        if (nonPawnPieces > 4) return emptyList()

        if (context.playerDelta >= -0.5f) return emptyList()

        val evalForMover = if (context.isWhiteMove) context.evalBefore else -context.evalBefore
        if (evalForMover < -0.3f) return emptyList()

        val side = if (context.isWhiteMove) "White" else "Black"
        return listOf(
            GameHighlight(
                moveIndex      = context.moveIndex,
                moveNumber     = context.moveNumber,
                isWhiteMove    = context.isWhiteMove,
                moveSan        = context.moveSan,
                fenBefore      = context.fenBefore,
                phase          = context.phase,
                ruleType       = ruleType,
                severity       = HighlightSeverity.IMPORTANT,
                title          = "Zugzwang",
                description    = "$side appears to be in zugzwang — any move worsens the position.",
                improvementTip = "Zugzwang is a key endgame concept. Try to put your opponent in a position where they must weaken their structure or give ground."
            )
        )
    }
}
