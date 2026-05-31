package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*

/**
 * Detects when a player seizes and maintains the initiative across two consecutive
 * of their own moves.
 */
class InitiativeRule : HighlightRule {
    override val ruleType = "initiative"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.phase == GamePhase.OPENING) return emptyList()
        if (context.playerDelta < 0.3f) return emptyList()

        val prevSameSide = context.prevContext?.prevContext ?: return emptyList()
        if (prevSameSide.isWhiteMove != context.isWhiteMove) return emptyList()
        if (prevSameSide.playerDelta < 0.3f) return emptyList()

        // prevContext is always non-null here (prevSameSide required it via prevContext?.prevContext)
        val opponentMove = context.prevContext
        if (opponentMove?.playerDelta ?: 0f > 0.3f) return emptyList()

        val prevPrevSameSide = prevSameSide.prevContext?.prevContext
        if (prevPrevSameSide != null &&
            prevPrevSameSide.isWhiteMove == context.isWhiteMove &&
            prevPrevSameSide.playerDelta >= 0.3f) return emptyList()

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
                severity       = HighlightSeverity.NOTABLE,
                title          = "Initiative seized",
                description    = "$side seized the initiative with ${context.moveSan} — consistently improving the position while keeping the opponent under pressure.",
                improvementTip = "Maintaining the initiative means making moves the opponent must respond to. Keep creating threats and don't let them settle."
            )
        )
    }
}
