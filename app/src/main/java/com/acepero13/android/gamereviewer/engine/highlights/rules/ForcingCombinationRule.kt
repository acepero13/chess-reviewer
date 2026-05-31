package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece

/**
 * Detects forcing combinations: a capture (or sacrifice) that is immediately good
 * AND leads to an even better position after the opponent's forced response.
 */
class ForcingCombinationRule : HighlightRule {
    override val ruleType = "forcing_combination"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.phase == GamePhase.OPENING) return emptyList()
        if (!context.isCapture) return emptyList()

        val next = context.nextContext ?: return emptyList()

        if (context.playerDelta < 0.2f) return emptyList()

        val destSq = BoardAttackHelper.destinationSquare(context.moveSan)
        val capturedValue = if (destSq != null) {
            val p = context.boardBefore.getPiece(destSq)
            if (p != Piece.NONE) p.materialValue else 0
        } else 0
        if (capturedValue < 3) return emptyList()

        if (next.playerDelta >= 0f) return emptyList()

        val twoMoveGain = context.playerDelta - next.playerDelta
        if (twoMoveGain < 0.3f) return emptyList()

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
                title          = "Forcing combination",
                description    = "$side initiated a forcing combination with ${context.moveSan} — the opponent had no good reply.",
                improvementTip = "Forcing moves that limit your opponent's options are very powerful. Always calculate checks, captures, and threats in that order."
            )
        )
    }
}
