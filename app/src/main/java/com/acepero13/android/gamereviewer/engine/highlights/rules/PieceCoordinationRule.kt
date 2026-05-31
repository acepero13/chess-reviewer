package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType

/**
 * Detects when 3 or more friendly pieces coordinate to attack the same enemy target.
 */
class PieceCoordinationRule : HighlightRule {
    override val ruleType = "piece_coordination"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.phase == GamePhase.OPENING) return emptyList()
        if (context.playerDelta < -0.5f) return emptyList()
        if (context.moveSan.startsWith("O-O")) return emptyList()

        val enemySquares = BoardAttackHelper.piecesOf(context.boardAfter, context.enemyColor)
            .filter { (_, p) -> p.pieceType != PieceType.KING }
            .map { it.first }

        for (targetSq in enemySquares) {
            val attackers = BoardAttackHelper.attackersOf(context.boardAfter, targetSq, context.moverColor)
            if (attackers.size < 3) continue

            val beforeAttackers = BoardAttackHelper.attackersOf(context.boardBefore, targetSq, context.moverColor)
            if (beforeAttackers.size >= 3) continue

            val targetPiece = context.boardAfter.getPiece(targetSq)
            if (targetPiece == Piece.NONE || targetPiece.materialValue < 3) continue

            val side       = if (context.isWhiteMove) "White" else "Black"
            val targetName = pieceName(targetPiece.pieceType)
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
                    title          = "Piece coordination",
                    description    = "$side's pieces coordinated effectively — ${attackers.size} pieces attacking the $targetName with ${context.moveSan}.",
                    improvementTip = "Coordinated pieces are far stronger than the sum of their parts. Aim to concentrate your pieces on the same target."
                )
            )
        }
        return emptyList()
    }

    private fun pieceName(type: PieceType) = when (type) {
        PieceType.QUEEN  -> "queen"
        PieceType.ROOK   -> "rook"
        PieceType.BISHOP -> "bishop"
        PieceType.KNIGHT -> "knight"
        PieceType.PAWN   -> "pawn"
        PieceType.KING   -> "king"
        else             -> "piece"
    }
}
