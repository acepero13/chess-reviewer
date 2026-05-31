package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*

/** Detects blunders, mistakes, and inaccuracies from the evaluation delta. */
class EvaluationDropRule : HighlightRule {
    override val ruleType = "eval_drop"

    companion object {
        const val BLUNDER_THRESHOLD    = -2.0f
        const val MISTAKE_THRESHOLD    = -1.0f
        const val INACCURACY_THRESHOLD = -0.5f
    }

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        val delta = context.playerDelta
        val (severity, title, description, tip) = when {
            delta <= BLUNDER_THRESHOLD -> Quad(
                HighlightSeverity.CRITICAL,
                "Blunder",
                "${if (context.isWhiteMove) "White" else "Black"} played ${context.moveSan} — a serious error that dropped ${(-delta).format()} pawns.",
                "Before moving, scan for checks, captures, and opponent threats (CCT)."
            )
            delta <= MISTAKE_THRESHOLD -> Quad(
                HighlightSeverity.IMPORTANT,
                "Mistake",
                "${if (context.isWhiteMove) "White" else "Black"} played ${context.moveSan} — an error that lost ${(-delta).format()} pawns.",
                "Slow down at critical moments. Ask: is my piece safe after this move?"
            )
            delta <= INACCURACY_THRESHOLD -> Quad(
                HighlightSeverity.NOTABLE,
                "Inaccuracy",
                "${if (context.isWhiteMove) "White" else "Black"} played ${context.moveSan} — a slightly inaccurate move (${(-delta).format()} pawns).",
                "Look for candidate moves before committing. There was a stronger option."
            )
            else -> return emptyList()
        }

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
                title          = title,
                description    = description,
                improvementTip = tip
            )
        )
    }

    private fun Float.format() = "%.1f".format(this)

    private data class Quad(
        val severity: HighlightSeverity,
        val title: String,
        val description: String,
        val tip: String
    )
}
