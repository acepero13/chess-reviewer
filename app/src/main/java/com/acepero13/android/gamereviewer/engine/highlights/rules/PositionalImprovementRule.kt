package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*

/** Detects significant positional gains achieved without material exchange. */
class PositionalImprovementRule : HighlightRule {
    override val ruleType = "positional_improvement"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.phase == GamePhase.OPENING) return emptyList()
        if (context.isCapture) return emptyList()
        if (context.playerDelta < 0.5f) return emptyList()

        val prev = context.prevContext
        if (prev != null && prev.playerDelta < -0.5f) return emptyList()

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
                title          = "Positional improvement",
                description    = "$side improved the position strategically with ${context.moveSan} — a purely positional gain without material exchange.",
                improvementTip = "Quiet positional moves are often the strongest. Always look to improve your worst-placed piece before launching an attack."
            )
        )
    }
}
