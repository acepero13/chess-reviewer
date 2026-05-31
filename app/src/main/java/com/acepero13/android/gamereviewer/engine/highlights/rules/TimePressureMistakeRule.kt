package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*

/**
 * Detects moves made when the player's clock had fallen into a critical range, AND
 * the move was a mistake or blunder.
 */
class TimePressureMistakeRule : HighlightRule {
    override val ruleType = "time_pressure_mistake"

    companion object {
        const val CRITICAL_CLOCK_SECONDS = 30
        const val MODERATE_CLOCK_SECONDS = 60
        const val BLUNDER_THRESHOLD      = -2.0f
        const val MISTAKE_THRESHOLD      = -1.0f
    }

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        val clockRemaining = context.clockRemainingSeconds ?: return emptyList()

        val isBlunder = context.playerDelta <= BLUNDER_THRESHOLD
        val isMistake = context.playerDelta <= MISTAKE_THRESHOLD

        if (!isMistake) return emptyList()

        val clockThreshold = if (isBlunder) MODERATE_CLOCK_SECONDS else CRITICAL_CLOCK_SECONDS
        if (clockRemaining > clockThreshold) return emptyList()

        val side       = if (context.isWhiteMove) "White" else "Black"
        val dropDesc   = "%.1f".format(-context.playerDelta)
        val clockDesc  = if (clockRemaining < 10) "under 10s" else "${clockRemaining}s"
        val errorLabel = if (isBlunder) "blunder" else "mistake"
        val severity   = if (isBlunder) HighlightSeverity.CRITICAL else HighlightSeverity.IMPORTANT

        return listOf(
            GameHighlight(
                moveIndex      = context.moveIndex,
                moveNumber     = context.moveNumber,
                isWhiteMove    = context.isWhiteMove,
                moveSan        = context.moveSan,
                fenBefore      = context.fenBefore,
                phase          = context.phase,
                ruleType       = ruleType,
                severity       = severity,
                title          = "Time-scramble $errorLabel",
                description    = "$side made a ${dropDesc}-pawn $errorLabel with ${context.moveSan} " +
                    "when only $clockDesc remained on the clock.",
                improvementTip = "Under severe time pressure: prioritise safety over ambition. " +
                    "Ask just two questions before moving: 'Does this lose material?' and " +
                    "'Does this allow checkmate?' If neither — it is safe enough. " +
                    "Longer time control games are the best place to practice managing the clock by " +
                    "spending more on critical moments early, and less on clearly quiet ones."
            )
        )
    }
}
