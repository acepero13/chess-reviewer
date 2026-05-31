package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType

/** Detects when a move creates a battery — two aligned sliding pieces on the same line. */
class BatteryRule : HighlightRule {
    override val ruleType = "battery"

    private val straightDirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
    private val diagDirs     = listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.phase == GamePhase.OPENING) return emptyList()
        if (context.playerDelta < -1.0f) return emptyList()
        if (context.moveSan.startsWith("O-O")) return emptyList()

        val destSq = BoardAttackHelper.movedPieceSquare(context.moveSan, context.boardAfter, context.moverColor)
            ?: return emptyList()
        val moverPiece = context.boardAfter.getPiece(destSq)
        if (moverPiece == Piece.NONE) return emptyList()

        val isSlider = moverPiece.pieceType in listOf(PieceType.ROOK, PieceType.QUEEN, PieceType.BISHOP)
        if (!isSlider) return emptyList()

        val dirs = when (moverPiece.pieceType) {
            PieceType.ROOK   -> straightDirs
            PieceType.BISHOP -> diagDirs
            PieceType.QUEEN  -> straightDirs + diagDirs
            else             -> return emptyList()
        }

        for ((dFile, dRank) in dirs) {
            val isDiag = dFile != 0 && dRank != 0

            // Use the full ray so we catch batteries where pieces aren't adjacent
            val ray = BoardAttackHelper.raySquares(context.boardAfter, destSq, dFile, dRank)
            val partnerSq = ray.firstOrNull { sq -> context.boardAfter.getPiece(sq) != Piece.NONE } ?: continue
            val nextPiece = context.boardAfter.getPiece(partnerSq)
            if (nextPiece == Piece.NONE) continue
            if (nextPiece.pieceSide != context.moverColor) continue
            if (!isCompatible(nextPiece.pieceType, isDiag)) continue

            // Verify this battery wasn't already there before the move
            val prevPiece = context.boardBefore.getPiece(destSq)
            val wasBefore = prevPiece != Piece.NONE &&
                prevPiece.pieceSide == context.moverColor &&
                isCompatible(prevPiece.pieceType, isDiag)
            if (wasBefore) continue

            val side = if (context.isWhiteMove) "White" else "Black"
            val dirName = directionName(dFile, dRank)

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
                    title          = "Battery",
                    description    = "$side created a battery on the $dirName with ${context.moveSan}.",
                    improvementTip = "Batteries double the pressure on a file, rank, or diagonal. Use them to control key lines."
                )
            )
        }

        return emptyList()
    }

    private fun isCompatible(type: PieceType, diagonal: Boolean) = if (diagonal) {
        type == PieceType.BISHOP || type == PieceType.QUEEN
    } else {
        type == PieceType.ROOK || type == PieceType.QUEEN
    }

    private fun directionName(dFile: Int, dRank: Int) = when {
        dFile == 0 && dRank != 0 -> "file"
        dFile != 0 && dRank == 0 -> "rank"
        else -> "diagonal"
    }
}
