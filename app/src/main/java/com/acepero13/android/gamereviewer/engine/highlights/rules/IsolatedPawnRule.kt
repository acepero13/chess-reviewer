package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Square
import kotlin.math.abs

/** Detects when a pawn move creates a new isolated pawn (no friendly pawns on adjacent files). */
class IsolatedPawnRule : HighlightRule {
    override val ruleType = "isolated_pawn"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (!context.isPawnMove) return emptyList()
        if (context.phase == GamePhase.ENDGAME) return emptyList()

        val destSq = BoardAttackHelper.destinationSquare(context.moveSan) ?: return emptyList()
        val file   = BoardAttackHelper.fileOf(destSq)

        if (isIsolatedAfter(context, file) && !wasIsolatedBefore(context, file)) {
            val side     = if (context.isWhiteMove) "White" else "Black"
            val fileName = "${'a' + file}"
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
                    title          = "Isolated pawn",
                    description    = "$side created an isolated pawn on the $fileName-file with ${context.moveSan}.",
                    improvementTip = "Isolated pawns are permanent weaknesses. Try to avoid creating them without clear compensation."
                )
            )
        }
        return emptyList()
    }

    private fun isIsolatedAfter(context: HighlightRuleContext, file: Int): Boolean {
        return Square.values().filter { it != Square.NONE }.none { sq ->
            val p = context.boardAfter.getPiece(sq)
            p != Piece.NONE && p.pieceSide == context.moverColor &&
            p.pieceType == PieceType.PAWN && abs(BoardAttackHelper.fileOf(sq) - file) == 1
        }
    }

    private fun wasIsolatedBefore(context: HighlightRuleContext, file: Int): Boolean {
        val hadPawnOnFile = Square.values().filter { it != Square.NONE }.any { sq ->
            val p = context.boardBefore.getPiece(sq)
            p != Piece.NONE && p.pieceSide == context.moverColor &&
            p.pieceType == PieceType.PAWN && BoardAttackHelper.fileOf(sq) == file
        }
        if (!hadPawnOnFile) return false
        return Square.values().filter { it != Square.NONE }.none { sq ->
            val p = context.boardBefore.getPiece(sq)
            p != Piece.NONE && p.pieceSide == context.moverColor &&
            p.pieceType == PieceType.PAWN && abs(BoardAttackHelper.fileOf(sq) - file) == 1
        }
    }
}
