package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square

/** Detects when a side gains or secures the bishop pair advantage. */
class BishopPairRule : HighlightRule {
    override val ruleType = "bishop_pair"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.phase == GamePhase.OPENING) return emptyList()

        val moverBishopsBefore = countBishops(context.boardBefore, context.moverColor)
        val moverBishopsAfter  = countBishops(context.boardAfter,  context.moverColor)
        val enemyBishopsBefore = countBishops(context.boardBefore, context.enemyColor)
        val enemyBishopsAfter  = countBishops(context.boardAfter,  context.enemyColor)

        // Mover still has both, enemy lost one → mover gained bishop pair advantage
        val gained = moverBishopsAfter == 2 && moverBishopsBefore == 2 &&
                     enemyBishopsBefore == 2 && enemyBishopsAfter < 2

        // Mover captured enemy bishop and now has both bishops vs enemy's single bishop
        val secured = moverBishopsAfter == 2 && moverBishopsBefore < 2 && context.isCapture

        if (!gained && !secured) return emptyList()

        // Only fire on moves that didn't lose material
        if (context.playerDelta < -0.5f) return emptyList()

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
                severity       = HighlightSeverity.NOTABLE,
                title          = "Bishop pair",
                description    = "$side secured the bishop pair advantage with ${context.moveSan}.",
                improvementTip = "Two bishops vs bishop+knight or two knights is a long-term positional advantage, especially in open positions."
            )
        )
    }

    private fun countBishops(board: Board, side: Side) =
        Square.values().filter { it != Square.NONE }.count { sq ->
            val p = board.getPiece(sq)
            p != Piece.NONE && p.pieceSide == side && p.pieceType == PieceType.BISHOP
        }
}
