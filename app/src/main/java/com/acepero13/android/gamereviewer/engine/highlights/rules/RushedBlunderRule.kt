package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*

/**
 * Detects when a move is both played very fast AND a significant blunder.
 */
class RushedBlunderRule : HighlightRule {
    override val ruleType = "rushed_blunder"

    companion object {
        const val RUSHED_THRESHOLD_SECONDS = 10
        const val BLUNDER_THRESHOLD        = -2.0f
        const val MIN_INTERESTING_EVAL     = 0.5f
    }

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        val timeSpent = context.timeSpentSeconds ?: return emptyList()

        if (timeSpent > RUSHED_THRESHOLD_SECONDS) return emptyList()
        if (context.playerDelta > BLUNDER_THRESHOLD) return emptyList()

        val evalForMover = if (context.isWhiteMove) context.evalBefore else -context.evalBefore
        if (evalForMover < -MIN_INTERESTING_EVAL) return emptyList()

        if (context.phase == GamePhase.OPENING && timeSpent <= 5) return emptyList()

        val side     = if (context.isWhiteMove) "White" else "Black"
        val dropDesc = "%.1f".format(-context.playerDelta)
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
                title          = "Rushed blunder",
                description    = "$side played ${context.moveSan} in just ${timeSpent}s — a ${dropDesc}-pawn blunder in a position that required more thought.",
                improvementTip = "Spending even 10 extra seconds to run the CCT check (Checks, Captures, Threats) before moving " +
                    "can eliminate most rushed blunders. Ask yourself: 'What can my opponent do next?' before clicking."
            )
        )
    }
}
