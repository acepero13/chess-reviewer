package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*

/**
 * Detects sequences of 3+ consecutive checks by the same side.
 */
class PerpetualCheckRule : HighlightRule {
    override val ruleType = "perpetual_check"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (!context.isCheck) return emptyList()

        val sameColorPrev1 = context.prevContext?.prevContext
        val sameColorPrev2 = sameColorPrev1?.prevContext?.prevContext

        if (sameColorPrev1 == null || !sameColorPrev1.isCheck) return emptyList()
        if (sameColorPrev1.isWhiteMove != context.isWhiteMove) return emptyList()

        if (sameColorPrev2 != null &&
            sameColorPrev2.isWhiteMove == context.isWhiteMove &&
            sameColorPrev2.isCheck) return emptyList()

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
                title          = "Perpetual check",
                description    = "$side is delivering repeated checks — a potential perpetual check for a draw.",
                improvementTip = "Perpetual check is a powerful defensive resource when losing, and a key concept to recognise when attacking to avoid a draw."
            )
        )
    }
}
