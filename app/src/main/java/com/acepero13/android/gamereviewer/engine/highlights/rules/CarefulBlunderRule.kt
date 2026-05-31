package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*

/**
 * Detects when a player spent a significant amount of time calculating a move
 * that turned out to be a blunder — suggesting the wrong candidate lines were considered.
 */
class CarefulBlunderRule : HighlightRule {
    override val ruleType = "careful_blunder"

    companion object {
        const val CAREFUL_THRESHOLD_SECONDS = 60
        const val BLUNDER_THRESHOLD = -2.0f
        const val LOW_CLOCK_SECONDS = 30
    }

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        val timeSpent = context.timeSpentSeconds ?: return emptyList()

        if (timeSpent < CAREFUL_THRESHOLD_SECONDS) return emptyList()
        if (context.playerDelta > BLUNDER_THRESHOLD) return emptyList()

        // Don't double-fire with TimePressureMistakeRule when the clock was low
        val clockRemaining = context.clockRemainingSeconds
        if (clockRemaining != null && clockRemaining <= LOW_CLOCK_SECONDS) return emptyList()

        val side     = if (context.isWhiteMove) "White" else "Black"
        val dropDesc = "%.1f".format(-context.playerDelta)
        val mins     = timeSpent / 60
        val secs     = timeSpent % 60
        val timeStr  = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"

        return listOf(
            GameHighlight(
                moveIndex      = context.moveIndex,
                moveNumber     = context.moveNumber,
                isWhiteMove    = context.isWhiteMove,
                moveSan        = context.moveSan,
                fenBefore      = context.fenBefore,
                phase          = context.phase,
                ruleType       = ruleType,
                severity       = HighlightSeverity.CRITICAL,
                title          = "Careful blunder",
                description    = "$side spent $timeStr on ${context.moveSan} — and still blundered ${dropDesc} pawns. " +
                    "The calculation went down the wrong path.",
                improvementTip = "When a long think still produces a blunder, you likely calculated the right " +
                    "depth on the wrong candidate move. After finding your main line, always pause and ask: " +
                    "'What is my opponent's BEST reply at EACH step?' " +
                    "Visualise the board after each ply to catch the refutation before committing."
            )
        )
    }
}
