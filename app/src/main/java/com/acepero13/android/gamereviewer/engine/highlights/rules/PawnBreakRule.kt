package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType

/** Detects a structural pawn break — pawn captures an enemy pawn to open lines. */
class PawnBreakRule : HighlightRule {
    override val ruleType = "pawn_break"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (!context.isPawnMove) return emptyList()
        if (!context.isCapture) return emptyList()
        if (context.phase == GamePhase.ENDGAME) return emptyList()
        if (context.playerDelta < 0f) return emptyList()

        val destSq   = BoardAttackHelper.destinationSquare(context.moveSan) ?: return emptyList()
        val captured = context.boardBefore.getPiece(destSq)
        if (captured == Piece.NONE || captured.pieceType != PieceType.PAWN) return emptyList()

        val srcFile  = srcFileOfPawn(context) ?: return emptyList()
        val destFile = BoardAttackHelper.fileOf(destSq)
        if (srcFile == destFile) return emptyList()

        val side     = if (context.isWhiteMove) "White" else "Black"
        val fileName = ('a' + destFile).toString()
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
                title          = "Pawn break",
                description    = "$side executed a structural pawn break on the $fileName-file with ${context.moveSan}, opening lines for the pieces.",
                improvementTip = "Pawn breaks are the engine of most middlegame plans. Identify which pawn break fits your position and prepare it carefully before striking."
            )
        )
    }

    private fun srcFileOfPawn(context: HighlightRuleContext): Int? {
        val firstChar = context.moveSan.firstOrNull() ?: return null
        if (!firstChar.isLetter() || firstChar.isUpperCase()) return null
        return firstChar - 'a'
    }
}
