package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Square

/**
 * Detects a pawn storm: a non-capturing pawn advance on a flank where another friendly
 * pawn is already advanced on an adjacent file.
 */
class PawnStormRule : HighlightRule {
    override val ruleType = "pawn_storm"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.phase != GamePhase.MIDDLEGAME) return emptyList()
        if (!context.isPawnMove) return emptyList()
        if (context.isCapture) return emptyList()
        if (context.playerDelta < -0.3f) return emptyList()

        val destSq   = BoardAttackHelper.destinationSquare(context.moveSan) ?: return emptyList()
        val destFile = BoardAttackHelper.fileOf(destSq)
        val destRank = BoardAttackHelper.rankOf(destSq)

        // Only flank pawns (not central d/e files)
        if (destFile in 3..4) return emptyList()

        val isAdvanced = if (context.isWhiteMove) destRank >= 4 else destRank <= 3
        if (!isAdvanced) return emptyList()

        val adjacentFiles = listOf(destFile - 1, destFile + 1).filter { it in 0..7 }
        val hasStormPartner = adjacentFiles.any { adjFile ->
            Square.values().filter { it != Square.NONE }.any { sq ->
                val p = context.boardAfter.getPiece(sq)
                p != Piece.NONE && p.pieceSide == context.moverColor &&
                p.pieceType == PieceType.PAWN &&
                BoardAttackHelper.fileOf(sq) == adjFile &&
                if (context.isWhiteMove) BoardAttackHelper.rankOf(sq) >= 4
                else                    BoardAttackHelper.rankOf(sq) <= 3
            }
        }
        if (!hasStormPartner) return emptyList()

        val side  = if (context.isWhiteMove) "White" else "Black"
        val flank = if (destFile <= 3) "queenside" else "kingside"
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
                title          = "Pawn storm",
                description    = "$side is launching a coordinated $flank pawn storm with ${context.moveSan}.",
                improvementTip = "Pawn storms are most effective when your king is safe and the opponent's king is on the stormed flank. Advance pawns in coordination, not one at a time."
            )
        )
    }
}
