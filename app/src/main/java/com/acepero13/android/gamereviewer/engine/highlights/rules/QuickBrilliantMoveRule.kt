package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*

/**
 * Positive highlight: detects when a player finds a strongly accurate move in very
 * little time — evidence of excellent pattern recognition or intuition.
 */
class QuickBrilliantMoveRule : HighlightRule {
    override val ruleType = "quick_brilliant_move"

    companion object {
        const val INSTANT_THRESHOLD_SECONDS = 8
        const val STRONG_GAIN_THRESHOLD     = 1.5f
        const val MIN_CONTESTED_EVAL        = 0.3f
    }

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        val timeSpent = context.timeSpentSeconds ?: return emptyList()

        if (timeSpent > INSTANT_THRESHOLD_SECONDS) return emptyList()
        if (context.playerDelta < STRONG_GAIN_THRESHOLD) return emptyList()
        if (context.phase == GamePhase.OPENING) return emptyList()

        val absEvalBefore = kotlin.math.abs(context.evalBefore)
        if (absEvalBefore < MIN_CONTESTED_EVAL && !context.isCheck) return emptyList()

        val side     = if (context.isWhiteMove) "White" else "Black"
        val gainDesc = "%.1f".format(context.playerDelta)

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
                title          = "Brilliant intuition",
                description    = "$side found the strong ${context.moveSan} in just ${timeSpent}s — excellent pattern recognition.",
                improvementTip = "Your pattern recognition is working well here. Keep sharpening tactics puzzles to sustain this speed-accuracy balance."
            )
        )
    }
}
