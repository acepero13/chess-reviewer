package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square

/**
 * Detects when a move creates a pin — a sliding piece attacks an enemy piece that shields
 * the enemy king.
 */
class PinRule : HighlightRule {
    override val ruleType = "pin"

    private val directions = listOf(
        1 to 0, -1 to 0, 0 to 1, 0 to -1,
        1 to 1, 1 to -1, -1 to 1, -1 to -1
    )

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.playerDelta < -1.0f) return emptyList()
        if (context.moveSan.startsWith("O-O")) return emptyList()

        val destSq = BoardAttackHelper.movedPieceSquare(context.moveSan, context.boardAfter, context.moverColor)
            ?: return emptyList()
        val moverPiece = context.boardAfter.getPiece(destSq)
        if (moverPiece == Piece.NONE) return emptyList()

        if (moverPiece.pieceType !in listOf(PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN)) return emptyList()

        val enemyKingSq = BoardAttackHelper.kingSquare(context.boardAfter, context.enemyColor) ?: return emptyList()

        val isRayPiece  = moverPiece.pieceType == PieceType.ROOK  || moverPiece.pieceType == PieceType.QUEEN
        val isDiagPiece = moverPiece.pieceType == PieceType.BISHOP || moverPiece.pieceType == PieceType.QUEEN

        for ((dFile, dRank) in directions) {
            val isDiag = dFile != 0 && dRank != 0
            if (isDiag && !isDiagPiece) continue
            if (!isDiag && !isRayPiece) continue

            val ray = BoardAttackHelper.raySquares(context.boardAfter, destSq, dFile, dRank)
            if (ray.isEmpty()) continue

            val firstSq    = ray.first()
            val firstPiece = context.boardAfter.getPiece(firstSq)
            if (firstPiece == Piece.NONE) continue
            if (firstPiece.pieceSide != context.enemyColor) continue
            if (firstPiece.pieceType == PieceType.KING) continue
            if (firstPiece.materialValue < 3) continue

            val rest = BoardAttackHelper.raySquares(context.boardAfter, firstSq, dFile, dRank)
            if (rest.firstOrNull() != enemyKingSq) continue

            val wasAlreadyPinned = wasPinnedBefore(context, firstSq, enemyKingSq, context.enemyColor, dFile, dRank)
            if (wasAlreadyPinned) continue

            val side       = if (context.isWhiteMove) "White" else "Black"
            val pinnerName = pieceName(moverPiece.pieceType)
            val pinnedName = pieceName(firstPiece.pieceType)

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
                    title          = "Pin",
                    description    = "$side's $pinnerName pinned the enemy $pinnedName with ${context.moveSan}.",
                    improvementTip = "Pins restrict enemy pieces. Look for bishops/rooks aligned with the enemy king."
                )
            )
        }

        return emptyList()
    }

    private fun wasPinnedBefore(
        context: HighlightRuleContext,
        pinnedSq: Square,
        kingSq: Square,
        enemyColor: Side,
        dFile: Int,
        dRank: Int
    ): Boolean {
        val isDiag    = dFile != 0 && dRank != 0
        val pinnerRay = BoardAttackHelper.raySquares(context.boardBefore, pinnedSq, -dFile, -dRank)
        return pinnerRay.any { sq ->
            val p = context.boardBefore.getPiece(sq)
            if (p == Piece.NONE) return@any false
            if (p.pieceSide == enemyColor) return@any false
            if (isDiag) p.pieceType == PieceType.BISHOP || p.pieceType == PieceType.QUEEN
            else        p.pieceType == PieceType.ROOK   || p.pieceType == PieceType.QUEEN
        }
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
