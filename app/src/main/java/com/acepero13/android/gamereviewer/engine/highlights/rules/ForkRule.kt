package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType

/**
 * Detects when a move creates a fork — one piece attacks two or more valuable enemy pieces
 * simultaneously.
 */
class ForkRule : HighlightRule {
    override val ruleType = "fork"

    companion object {
        private const val VALUABLE_THRESHOLD = 3
    }

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.playerDelta < -1.0f) return emptyList()
        if (context.moveSan.startsWith("O-O")) return emptyList()

        val destSq = BoardAttackHelper.movedPieceSquare(context.moveSan, context.boardAfter, context.moverColor)
            ?: return emptyList()

        val forkerPiece = context.boardAfter.getPiece(destSq)
        if (forkerPiece == Piece.NONE) return emptyList()

        // The forking piece must not itself be hanging
        val forkerValue = forkerPiece.materialValue
        if (BoardAttackHelper.isAttackedBy(context.boardAfter, destSq, context.enemyColor)) {
            val cheapestAttacker = BoardAttackHelper.attackersOf(context.boardAfter, destSq, context.enemyColor)
                .mapNotNull { sq ->
                    val p = context.boardAfter.getPiece(sq)
                    if (p != Piece.NONE) p.materialValue else null
                }
                .minOrNull() ?: Int.MAX_VALUE
            if (cheapestAttacker <= forkerValue) return emptyList()
        }

        val attacked = BoardAttackHelper.attacksFrom(context.boardAfter, destSq)
        val forkedPieces = attacked.mapNotNull { sq ->
            val p = context.boardAfter.getPiece(sq)
            if (p == Piece.NONE) return@mapNotNull null
            if (p.pieceSide != context.enemyColor) return@mapNotNull null
            if (p.pieceType == PieceType.KING || p.materialValue >= VALUABLE_THRESHOLD) sq
            else null
        }

        val undefended = forkedPieces.filter { sq ->
            val p = context.boardAfter.getPiece(sq)
            p.pieceType == PieceType.KING ||
            !BoardAttackHelper.isAttackedBy(context.boardAfter, sq, context.enemyColor)
        }

        if (undefended.size < 2 && forkedPieces.size < 2) return emptyList()

        val side      = if (context.isWhiteMove) "White" else "Black"
        val pieceName = pieceName(forkerPiece.pieceType)

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
                title          = "Fork",
                description    = "$side's $pieceName created a fork with ${context.moveSan}, attacking ${forkedPieces.size} pieces at once.",
                improvementTip = "Forks win material. Always look for pieces your opponent cannot save simultaneously."
            )
        )
    }

    private fun pieceName(type: PieceType) = when (type) {
        PieceType.KNIGHT -> "knight"
        PieceType.QUEEN  -> "queen"
        PieceType.ROOK   -> "rook"
        PieceType.BISHOP -> "bishop"
        PieceType.PAWN   -> "pawn"
        PieceType.KING   -> "king"
        else             -> "piece"
    }
}
