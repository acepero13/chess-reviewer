package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*

/**
 * Detects when a player spends an excessive amount of time on a position that is
 * objectively quiet and roughly equal — a common symptom of analysis paralysis.
 */
class OverthinkingRule : HighlightRule {
    override val ruleType = "overthinking"

    companion object {
        const val OVERTHINK_THRESHOLD_SECONDS = 90
        const val BALANCED_EVAL_THRESHOLD = 0.4f
        const val BLUNDER_THRESHOLD = -2.0f
    }

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        val timeSpent = context.timeSpentSeconds ?: return emptyList()

        if (timeSpent < OVERTHINK_THRESHOLD_SECONDS) return emptyList()
        if (context.isCapture) return emptyList()
        if (context.isCheck)   return emptyList()
        if (context.playerDelta <= BLUNDER_THRESHOLD) return emptyList()

        if (kotlin.math.abs(context.evalBefore) > BALANCED_EVAL_THRESHOLD) return emptyList()

        val prev = context.prevContext
        if (prev != null && prev.playerDelta <= -1.5f) return emptyList()

        val side    = if (context.isWhiteMove) "White" else "Black"
        val mins    = timeSpent / 60
        val secs    = timeSpent % 60
        val timeStr = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"

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
                title          = "Overthinking",
                description    = "$side spent $timeStr on ${context.moveSan} — a balanced, non-tactical position " +
                    "with little urgency. That time may have been better saved for later.",
                improvementTip = "In equal, quiet positions the question is not 'what is the best move?' " +
                    "but 'what is a solid, non-weakening move I can play quickly?' " +
                    "Reserve deep calculation for positions with concrete threats or imbalances."
            )
        )
    }
}
