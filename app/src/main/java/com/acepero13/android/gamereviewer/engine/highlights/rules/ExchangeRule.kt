package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square

/** Detects when major pieces (queens, rooks) are exchanged. */
class ExchangeRule : HighlightRule {
    override val ruleType = "exchange"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        fun countType(board: Board, side: Side, type: PieceType) =
            Square.values().filter { it != Square.NONE }.count { sq ->
                val p = board.getPiece(sq)
                p != Piece.NONE && p.pieceSide == side && p.pieceType == type
            }

        for (pieceType in listOf(PieceType.QUEEN, PieceType.ROOK)) {
            val moverBefore = countType(context.boardBefore, context.moverColor, pieceType)
            val moverAfter  = countType(context.boardAfter,  context.moverColor, pieceType)
            val enemyBefore = countType(context.boardBefore, context.enemyColor, pieceType)
            val enemyAfter  = countType(context.boardAfter,  context.enemyColor, pieceType)

            val moverLost = moverBefore - moverAfter
            val enemyLost = enemyBefore - enemyAfter

            if (moverLost == 1 && enemyLost == 1) {
                val pieceName = if (pieceType == PieceType.QUEEN) "queens" else "rooks"
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
                        title          = "${pieceName.replaceFirstChar { it.uppercaseChar() }} exchanged",
                        description    = "$side exchanged $pieceName with ${context.moveSan}.",
                        improvementTip = "Exchanges change the game's character. Consider whether simplification helps or hurts your position."
                    )
                )
            }
        }

        return emptyList()
    }
}
