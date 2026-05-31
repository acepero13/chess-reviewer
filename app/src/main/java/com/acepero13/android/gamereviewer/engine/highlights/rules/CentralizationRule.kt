package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Square

/** Detects when a piece (non-pawn) moves to the central squares. */
class CentralizationRule : HighlightRule {
    override val ruleType = "centralization"

    companion object {
        // Tight center: d4,e4,d5,e5  (ordinal = rank*8 + file; d=3,e=4; rank3=rank index 3, rank4=4)
        private val CENTER_SQUARES = setOf(27, 28, 35, 36)
        // Extended center: c3..f3, c4..f4, c5..f5, c6..f6
        private val EXTENDED_CENTER = (2..5).flatMap { file ->
            (2..5).map { rank -> rank * 8 + file }
        }.toSet()
    }

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.playerDelta < -0.5f) return emptyList()
        if (context.moveSan.startsWith("O-O")) return emptyList()
        if (context.isPawnMove) return emptyList()

        val destSq = BoardAttackHelper.movedPieceSquare(context.moveSan, context.boardAfter, context.moverColor)
            ?: return emptyList()
        val piece = context.boardAfter.getPiece(destSq)
        if (piece == Piece.NONE) return emptyList()
        if (piece.pieceType == PieceType.PAWN || piece.pieceType == PieceType.KING) return emptyList()

        val sqOrdinal = destSq.ordinal
        val inCenter   = sqOrdinal in CENTER_SQUARES
        val inExtended = !inCenter && sqOrdinal in EXTENDED_CENTER
        if (!inCenter && !inExtended) return emptyList()

        // Was already on a central square before? Skip — not a centralization
        val srcSq = BoardAttackHelper.piecesOf(context.boardBefore, context.moverColor)
            .firstOrNull { (sq, p) ->
                p.pieceType == piece.pieceType &&
                context.boardAfter.getPiece(sq) == Piece.NONE
            }?.first
        if (srcSq != null && (srcSq.ordinal in CENTER_SQUARES || srcSq.ordinal in EXTENDED_CENTER)) return emptyList()

        val side      = if (context.isWhiteMove) "White" else "Black"
        val pieceName = pieceName(piece.pieceType)
        val sqName    = BoardAttackHelper.squareName(destSq)

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
                title          = "Centralized ${pieceName}",
                description    = "$side centralized the $pieceName to $sqName with ${context.moveSan}.",
                improvementTip = "Central pieces control the maximum number of squares and are more powerful than pieces on the rim."
            )
        )
    }

    private fun pieceName(type: PieceType) = when (type) {
        PieceType.QUEEN  -> "queen"
        PieceType.ROOK   -> "rook"
        PieceType.BISHOP -> "bishop"
        PieceType.KNIGHT -> "knight"
        else             -> "piece"
    }
}
