package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*

/**
 * Detects zwischenzug (in-between move): opponent captured a piece, but instead of
 * recapturing, the mover inserts a strong check or capture.
 */
class ZwischenzugRule : HighlightRule {
    override val ruleType = "zwischenzug"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.phase == GamePhase.OPENING) return emptyList()
        if (context.playerDelta < 0.3f) return emptyList()

        val prev = context.prevContext ?: return emptyList()

        if (!prev.isCapture) return emptyList()

        val prevDestSq = BoardAttackHelper.destinationSquare(prev.moveSan)
        val currDestSq = BoardAttackHelper.destinationSquare(context.moveSan)
        if (prevDestSq != null && prevDestSq == currDestSq) return emptyList()

        if (!context.isCheck && !context.isCapture) return emptyList()

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
                title          = "Zwischenzug",
                description    = "$side played an in-between move (${context.moveSan}) instead of recapturing — an intermezzo that changed the game.",
                improvementTip = "Before recapturing, always check if there is an even stronger move available. An unexpected zwischenzug can completely transform the position."
            )
        )
    }
}
