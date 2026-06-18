package com.acepero13.android.gamereviewer.domain

/**
 * Maps a single move's centipawn loss (CPL) to a quality label.
 *
 * Shared between the per-game report ([com.acepero13.android.gamereviewer.ui.screens.GameReportViewModel])
 * and the cross-game Insights stats so both use identical thresholds.
 */
object MoveClassifier {

    enum class Quality(val label: String) {
        BEST("Best Move"),
        EXCELLENT("Excellent"),
        GOOD("Good Move"),
        INACCURACY("Inaccuracy"),
        MISTAKE("Mistake"),
        BLUNDER("Blunder"),
    }

    /** @param cpl centipawn loss for the move (>= 0). */
    fun classify(cpl: Int): Quality = when {
        cpl <= 0   -> Quality.BEST
        cpl <= 10  -> Quality.EXCELLENT
        cpl <= 25  -> Quality.GOOD
        cpl <= 50  -> Quality.INACCURACY
        cpl <= 100 -> Quality.MISTAKE
        else       -> Quality.BLUNDER
    }

    /** Convenience for callers that still work with the human-readable label. */
    fun label(cpl: Int): String = classify(cpl).label
}
