package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType

/**
 * Detects when a piece (bishop/rook/queen) moves to a square that cannot be attacked
 * by any enemy pawn — a "weak square" for the opponent.
 */
class WeakSquareRule : HighlightRule {
    override val ruleType = "weak_square"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.phase == GamePhase.OPENING) return emptyList()
        if (context.playerDelta < -0.5f) return emptyList()
        if (context.moveSan.startsWith("O-O")) return emptyList()

        val destSq = BoardAttackHelper.movedPieceSquare(context.moveSan, context.boardAfter, context.moverColor)
            ?: return emptyList()
        val piece = context.boardAfter.getPiece(destSq)
        if (piece == Piece.NONE) return emptyList()

        if (piece.pieceType in listOf(PieceType.PAWN, PieceType.KING)) return emptyList()
        if (piece.pieceType == PieceType.KNIGHT) return emptyList() // covered by KnightOutpostRule

        val enemyPawnDir = if (context.isWhiteMove) -1 else 1
        val file = BoardAttackHelper.fileOf(destSq)
        val rank = BoardAttackHelper.rankOf(destSq)

        val leftSq  = BoardAttackHelper.squareAt(file - 1, rank + enemyPawnDir)
        val rightSq = BoardAttackHelper.squareAt(file + 1, rank + enemyPawnDir)

        val isAttackableByPawn = (leftSq != null && run {
            val p = context.boardAfter.getPiece(leftSq)
            p != Piece.NONE && p.pieceType == PieceType.PAWN && p.pieceSide == context.enemyColor
        }) || (rightSq != null && run {
            val p = context.boardAfter.getPiece(rightSq)
            p != Piece.NONE && p.pieceType == PieceType.PAWN && p.pieceSide == context.enemyColor
        })

        if (isAttackableByPawn) return emptyList()

        val meaningfulRank = if (context.isWhiteMove) rank >= 4 else rank <= 3
        if (!meaningfulRank) return emptyList()

        val side      = if (context.isWhiteMove) "White" else "Black"
        val sqName    = BoardAttackHelper.squareName(destSq)
        val pieceName = pieceName(piece.pieceType)
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
                title          = "Weak square occupied",
                description    = "$side's $pieceName occupied the weak square $sqName — no enemy pawn can challenge it.",
                improvementTip = "Pieces on squares enemy pawns can't attack are permanent assets. Look for these outposts."
            )
        )
    }

    private fun pieceName(type: PieceType) = when (type) {
        PieceType.QUEEN  -> "queen"
        PieceType.ROOK   -> "rook"
        PieceType.BISHOP -> "bishop"
        PieceType.KNIGHT -> "knight"
        else -> "piece"
    }
}
