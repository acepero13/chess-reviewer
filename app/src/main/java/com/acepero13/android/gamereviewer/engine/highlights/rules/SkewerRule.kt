package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType

/**
 * Detects a skewer: a sliding piece attacks a valuable enemy piece, and behind that piece
 * on the same ray sits a less valuable enemy piece.
 */
class SkewerRule : HighlightRule {
    override val ruleType = "skewer"

    private val directions = listOf(
        1 to 0, -1 to 0, 0 to 1, 0 to -1,
        1 to 1, 1 to -1, -1 to 1, -1 to -1
    )

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.playerDelta < -1.0f) return emptyList()
        if (context.moveSan.startsWith("O-O")) return emptyList()
        if (context.phase == GamePhase.OPENING) return emptyList()

        val destSq = BoardAttackHelper.movedPieceSquare(context.moveSan, context.boardAfter, context.moverColor)
            ?: return emptyList()
        val moverPiece = context.boardAfter.getPiece(destSq)
        if (moverPiece == Piece.NONE) return emptyList()

        if (moverPiece.pieceType !in listOf(PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN)) return emptyList()

        val forkerValue = moverPiece.materialValue
        if (BoardAttackHelper.isAttackedBy(context.boardAfter, destSq, context.enemyColor)) {
            val cheapest = BoardAttackHelper.attackersOf(context.boardAfter, destSq, context.enemyColor)
                .mapNotNull { sq ->
                    val p = context.boardAfter.getPiece(sq)
                    if (p != Piece.NONE) p.materialValue else null
                }.minOrNull() ?: Int.MAX_VALUE
            if (cheapest <= forkerValue) return emptyList()
        }

        val isDiag     = moverPiece.pieceType == PieceType.BISHOP || moverPiece.pieceType == PieceType.QUEEN
        val isStraight = moverPiece.pieceType == PieceType.ROOK   || moverPiece.pieceType == PieceType.QUEEN

        for ((dFile, dRank) in directions) {
            val diagonal = dFile != 0 && dRank != 0
            if (diagonal && !isDiag) continue
            if (!diagonal && !isStraight) continue

            val ray = BoardAttackHelper.raySquares(context.boardAfter, destSq, dFile, dRank)
            if (ray.size < 2) continue

            val firstSq    = ray[0]
            val firstPiece = context.boardAfter.getPiece(firstSq)
            if (firstPiece == Piece.NONE) continue
            if (firstPiece.pieceSide != context.enemyColor) continue
            if (firstPiece.materialValue < 5) continue

            val restRay    = BoardAttackHelper.raySquares(context.boardAfter, firstSq, dFile, dRank)
            val secondSq   = restRay.firstOrNull() ?: continue
            val secondPiece = context.boardAfter.getPiece(secondSq)
            if (secondPiece == Piece.NONE) continue
            if (secondPiece.pieceSide != context.enemyColor) continue
            if (secondPiece.materialValue >= firstPiece.materialValue) continue

            if (BoardAttackHelper.isAttackedBy(context.boardAfter, secondSq, context.enemyColor)) continue

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
                    title          = "Skewer",
                    description    = "$side executed a skewer with ${context.moveSan}, forcing the ${pieceName(firstPiece.pieceType)} to move and exposing the ${pieceName(secondPiece.pieceType)}.",
                    improvementTip = "Skewers force valuable pieces to move. Use your sliders to X-ray through enemy pieces."
                )
            )
        }
        return emptyList()
    }

    private fun pieceName(type: PieceType) = when (type) {
        PieceType.KING   -> "king"
        PieceType.QUEEN  -> "queen"
        PieceType.ROOK   -> "rook"
        PieceType.BISHOP -> "bishop"
        PieceType.KNIGHT -> "knight"
        PieceType.PAWN   -> "pawn"
        else             -> "piece"
    }
}
