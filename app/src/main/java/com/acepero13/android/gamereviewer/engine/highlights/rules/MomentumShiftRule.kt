package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*

/** Detects when the advantage switches sides (eval crosses zero with significant swing). */
class MomentumShiftRule : HighlightRule {
    override val ruleType = "momentum_shift"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.prevContext == null) return emptyList()
        if (context.playerDelta < 0f) return emptyList()

        val before = context.evalBefore
        val after  = context.evalAfter

        val crossedZero = (before > 0.3f && after < -0.3f) || (before < -0.3f && after > 0.3f)
        if (!crossedZero) return emptyList()
        if (kotlin.math.abs(after - before) < 0.5f) return emptyList()

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
                title          = "Momentum shift",
                description    = "$side's move ${context.moveSan} flipped the advantage — the balance swung from ${fmt(before)} to ${fmt(after)}.",
                improvementTip = "Turning points are the most critical moments. Recognize when the balance is shifting and act decisively to seize the initiative."
            )
        )
    }

    private fun fmt(e: Float): String {
        val sign = if (e >= 0f) "+" else ""
        return "$sign${"%.1f".format(e)}"
    }
}
