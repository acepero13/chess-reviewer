package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Square

/** Detects when a piece is lost for free (hung or captured without compensation). */
class BlunderedPieceRule : HighlightRule {
    override val ruleType = "blundered_piece"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        // Only flag when the eval also dropped significantly (engine agrees it was bad)
        if (context.playerDelta > -1.0f) return emptyList()

        // Count material lost by the mover: pieces present before that are gone after
        val beforeMover = BoardAttackHelper.piecesOf(context.boardBefore, context.moverColor)
        val afterMover  = BoardAttackHelper.piecesOf(context.boardAfter,  context.moverColor)

        val beforeSquares = beforeMover.associate { (sq, p) -> sq to p.pieceType }
        val afterSquares  = afterMover.associate  { (sq, p) -> sq to p.pieceType }

        // Pieces of the mover's color that disappeared (by square)
        val lostSquares = beforeSquares.keys - afterSquares.keys
        val lostValue = lostSquares.sumOf { sq ->
            context.boardBefore.getPiece(sq).materialValue
        }

        // Also account for opponent pieces captured in the same move
        val beforeEnemy = BoardAttackHelper.piecesOf(context.boardBefore, context.enemyColor)
        val afterEnemy  = BoardAttackHelper.piecesOf(context.boardAfter,  context.enemyColor)
        val capturedSquares = beforeEnemy.map { it.first }.toSet() - afterEnemy.map { it.first }.toSet()
        val capturedValue = capturedSquares.sumOf { sq ->
            context.boardBefore.getPiece(sq).materialValue
        }

        val netLoss = lostValue - capturedValue
        if (netLoss < 3) return emptyList() // Less than bishop/knight: not significant enough

        val pieceName = lostSquares.firstNotNullOfOrNull { sq ->
            pieceName(context.boardBefore.getPiece(sq).pieceType)
        } ?: "piece"

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
                severity       = HighlightSeverity.CRITICAL,
                title          = "Piece blunder",
                description    = "$side lost a $pieceName (net $netLoss point${if (netLoss == 1) "" else "s"}) with ${context.moveSan}.",
                improvementTip = "Before moving, check if your pieces are safe. Ask: can my opponent win material after this?"
            )
        )
    }

    private fun pieceName(type: PieceType) = when (type) {
        PieceType.QUEEN  -> "queen"
        PieceType.ROOK   -> "rook"
        PieceType.BISHOP -> "bishop"
        PieceType.KNIGHT -> "knight"
        PieceType.PAWN   -> "pawn"
        else             -> null
    }
}
