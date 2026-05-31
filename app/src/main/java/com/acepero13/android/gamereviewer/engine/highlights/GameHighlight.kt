package com.acepero13.android.gamereviewer.engine.highlights

/**
 * A single coaching highlight produced by a [HighlightRule].
 *
 * Highlights are generated after background engine analysis completes and are
 * displayed in the Navigate-mode move timeline as colour-coded badges.
 */
data class GameHighlight(
    val moveIndex: Int,
    val moveNumber: Int,
    val isWhiteMove: Boolean,
    val moveSan: String,
    val fenBefore: String,
    val phase: GamePhase,
    val ruleType: String,
    val severity: HighlightSeverity,
    val title: String,
    val description: String,
    val improvementTip: String,
)
