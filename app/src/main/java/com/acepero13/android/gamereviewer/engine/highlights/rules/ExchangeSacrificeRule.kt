package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square

/** Detects when a rook is sacrificed for a minor piece with positional compensation. */
class ExchangeSacrificeRule : HighlightRule {
    override val ruleType = "exchange_sacrifice"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (!context.isCapture) return emptyList()
        if (context.phase == GamePhase.OPENING) return emptyList()

        val moverRooksBefore  = countRooks(context.boardBefore, context.moverColor)
        val moverRooksAfter   = countRooks(context.boardAfter,  context.moverColor)
        val enemyMinorsBefore = countMinors(context.boardBefore, context.enemyColor)
        val enemyMinorsAfter  = countMinors(context.boardAfter,  context.enemyColor)

        if (moverRooksBefore - moverRooksAfter != 1) return emptyList()
        if (enemyMinorsBefore - enemyMinorsAfter != 1) return emptyList()

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
                severity       = HighlightSeverity.IMPORTANT,
                title          = "Exchange sacrifice",
                description    = "$side sacrificed the exchange with ${context.moveSan} — giving up a rook for a minor piece to gain positional compensation.",
                improvementTip = "Exchange sacrifices are powerful when they destroy pawn structure, eliminate a key defender, or create a passed pawn. Material is not everything."
            )
        )
    }

    private fun countRooks(board: Board, side: Side) =
        Square.values().filter { it != Square.NONE }.count { sq ->
            val p = board.getPiece(sq)
            p != Piece.NONE && p.pieceSide == side && p.pieceType == PieceType.ROOK
        }

    private fun countMinors(board: Board, side: Side) =
        Square.values().filter { it != Square.NONE }.count { sq ->
            val p = board.getPiece(sq)
            p != Piece.NONE && p.pieceSide == side &&
            (p.pieceType == PieceType.BISHOP || p.pieceType == PieceType.KNIGHT)
        }
}
