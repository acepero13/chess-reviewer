package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Square

/** Detects beneficial queen or rook trades that steer toward a favourable endgame. */
class SimplificationRule : HighlightRule {
    override val ruleType = "simplification"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (!context.isCapture) return emptyList()
        if (context.phase == GamePhase.OPENING) return emptyList()
        if (context.playerDelta < 0f) return emptyList()

        val queensBefore = countType(context.boardBefore, PieceType.QUEEN)
        val queensAfter  = countType(context.boardAfter,  PieceType.QUEEN)
        val rooksBefore  = countType(context.boardBefore, PieceType.ROOK)
        val rooksAfter   = countType(context.boardAfter,  PieceType.ROOK)

        val queenTrade = queensBefore >= 2 && queensAfter < queensBefore
        val rookTrade  = !queenTrade && rooksBefore >= 2 && rooksAfter < rooksBefore

        if (!queenTrade && !rookTrade) return emptyList()

        val side      = if (context.isWhiteMove) "White" else "Black"
        val tradeDesc = if (queenTrade) "queens" else "rooks"
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
                title          = "Simplification",
                description    = "$side simplified by trading $tradeDesc with ${context.moveSan}, steering toward a favourable endgame.",
                improvementTip = "When ahead, trade pieces to simplify. When behind, keep pieces on the board to create complications. Know which side you want to be on."
            )
        )
    }

    private fun countType(board: Board, type: PieceType) =
        Square.values().filter { it != Square.NONE }.count { sq ->
            val p = board.getPiece(sq)
            p != Piece.NONE && p.pieceType == type
        }
}
