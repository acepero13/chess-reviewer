package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square

/** Detects material imbalances: piece traded for pawns, or rook vs minor piece. */
class MaterialImbalanceRule : HighlightRule {
    override val ruleType = "material_imbalance"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (!context.isCapture) return emptyList()
        context.prevContext ?: return emptyList()

        val moverMinorsBefore = countMinors(context.boardBefore, context.moverColor)
        val moverMinorsAfter  = countMinors(context.boardAfter,  context.moverColor)
        val enemyPawnsBefore  = countPawns(context.boardBefore,  context.enemyColor)
        val enemyPawnsAfter   = countPawns(context.boardAfter,   context.enemyColor)

        val minorLost   = moverMinorsBefore - moverMinorsAfter
        val pawnsGained = enemyPawnsBefore - enemyPawnsAfter

        if (minorLost == 1 && pawnsGained >= 2 && context.playerDelta > 0f) {
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
                    title          = "Material imbalance",
                    description    = "$side traded a piece for $pawnsGained pawns with ${context.moveSan}, creating a material imbalance.",
                    improvementTip = "Material imbalances require careful evaluation. Piece vs. pawns can favour either side depending on the position."
                )
            )
        }
        return emptyList()
    }

    private fun countMinors(board: Board, side: Side) =
        Square.values().filter { it != Square.NONE }.count { sq ->
            val p = board.getPiece(sq)
            p != Piece.NONE && p.pieceSide == side &&
            (p.pieceType == PieceType.BISHOP || p.pieceType == PieceType.KNIGHT)
        }

    private fun countPawns(board: Board, side: Side) =
        Square.values().filter { it != Square.NONE }.count { sq ->
            val p = board.getPiece(sq)
            p != Piece.NONE && p.pieceSide == side && p.pieceType == PieceType.PAWN
        }
}
