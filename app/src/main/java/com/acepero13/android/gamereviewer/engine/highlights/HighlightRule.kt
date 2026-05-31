package com.acepero13.android.gamereviewer.engine.highlights

/** Stateless rule that evaluates one move and returns zero or more highlights. */
interface HighlightRule {
    val ruleType: String
    fun evaluate(context: HighlightRuleContext): List<GameHighlight>
}
