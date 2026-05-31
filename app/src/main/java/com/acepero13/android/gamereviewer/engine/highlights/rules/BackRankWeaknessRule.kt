package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square

/**
 * Detects when the enemy king is trapped on the back rank with no escape squares —
 * a back-rank weakness that the moving side can exploit.
 */
class BackRankWeaknessRule : HighlightRule {
    override val ruleType = "back_rank_weakness"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.phase == GamePhase.OPENING) return emptyList()
        // Only flag on good moves
        if (context.playerDelta < -0.5f) return emptyList()

        val enemyKingSq = BoardAttackHelper.kingSquare(context.boardAfter, context.enemyColor) ?: return emptyList()
        val enemyKingRank = BoardAttackHelper.rankOf(enemyKingSq)

        // Enemy king must be on their back rank
        val onBackRank = if (context.enemyColor == Side.WHITE) enemyKingRank == 0
                         else enemyKingRank == 7
        if (!onBackRank) return emptyList()

        // Count pawns shielding the king (immediately in front)
        val pawnRank = if (context.enemyColor == Side.WHITE) 1 else 6
        val kingFile = BoardAttackHelper.fileOf(enemyKingSq)
        val shieldCount = (-1..1).count { df ->
            val f = kingFile + df
            if (f !in 0..7) return@count false
            val pawnSq = BoardAttackHelper.squareAt(f, pawnRank) ?: return@count false
            val p = context.boardAfter.getPiece(pawnSq)
            p != Piece.NONE && p.pieceType == PieceType.PAWN && p.pieceSide == context.enemyColor
        }

        val escapeSqs = BoardAttackHelper.attacksFrom(context.boardAfter, enemyKingSq).filter { sq ->
            val occupied = context.boardAfter.getPiece(sq)
            occupied == Piece.NONE || occupied.pieceSide != context.enemyColor
        }
        val noEscape = escapeSqs.isEmpty() || escapeSqs.all {
            BoardAttackHelper.isAttackedBy(context.boardAfter, it, context.moverColor)
        }

        // Check if a mover's rook/queen attacks the back rank
        val backRankRank = enemyKingRank
        val moverHasBackRankAttacker = BoardAttackHelper.piecesOf(context.boardAfter, context.moverColor).any { (sq, p) ->
            (p.pieceType == PieceType.ROOK || p.pieceType == PieceType.QUEEN) &&
            BoardAttackHelper.rankOf(sq) != backRankRank &&
            BoardAttackHelper.raySquares(context.boardAfter, sq, 0, if (backRankRank == 0) -1 else 1)
                .any { rsq -> BoardAttackHelper.rankOf(rsq) == backRankRank }
        }

        if (noEscape || (shieldCount >= 3 && moverHasBackRankAttacker)) {
            val side = if (context.isWhiteMove) "White" else "Black"
            val enemySide = if (context.isWhiteMove) "Black" else "White"
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
                    title          = "Back-rank weakness",
                    description    = "$side exploited $enemySide's back-rank weakness with ${context.moveSan}.",
                    improvementTip = "Back-rank weaknesses are deadly. Create escape squares for your king with h3/h6 before they become a problem."
                )
            )
        }
        return emptyList()
    }
}
