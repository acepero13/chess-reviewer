package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.GameStats
import com.acepero13.android.gamereviewer.data.model.NotablePosition

/**
 * Aggregated "When Ahead / When Behind" view — backs the **Conversion** tab.
 *
 * Reframes cached [GameStats] around two stories: how often the user converts a winning position
 * into a win, and how often they save a losing one. All inputs are already-cached aggregates;
 * nothing here re-runs the engine.
 */
data class ConversionReport(
    val whenAhead: SideStats,
    val whenBehind: SideStats,
    val winningScatter: List<ConversionPoint>,
    val losingScatter: List<ConversionPoint>,
    val missedSimplifications: List<NotablePosition>,
) {
    val hasData: Boolean get() = whenAhead.games > 0 || whenBehind.games > 0
}

/** One side of the story (ahead or behind). [ratePct] is conversion/save %, [accuracy] is 0–100. */
data class SideStats(
    val games: Int,
    val successes: Int,
    val ratePct: Int,
    val secondaryCount: Int,
    val accuracy: Float,
)

/**
 * A single game's dot on the ahead/behind plot. [accuracy] is 0–100 in the relevant phase;
 * [success] is the *good* outcome for that side (converted a win / saved a loss).
 */
data class ConversionPoint(val accuracy: Float, val success: Boolean)

object ConversionAnalyzer {

    fun analyze(
        stats: List<GameStats>,
        missedSimplifications: List<NotablePosition>,
    ): ConversionReport {
        val ahead = stats.filter { it.reachedWinning }
        val behind = stats.filter { it.reachedLosing }

        val converted = ahead.count { it.convertedWin }
        val saved = behind.count { it.savedLoss }

        val whenAhead = SideStats(
            games          = ahead.size,
            successes      = converted,
            ratePct        = pct(converted, ahead.size),
            secondaryCount = ahead.size - converted,           // "throws"
            accuracy       = ahead.map { it.conversionAccuracy }.filter { it > 0f }.avgOrZero(),
        )
        val whenBehind = SideStats(
            games          = behind.size,
            successes      = saved,
            ratePct        = pct(saved, behind.size),
            secondaryCount = saved,                            // "saves"
            accuracy       = behind.map { it.middlegameDefenseAccuracy }.filter { it > 0f }.avgOrZero(),
        )

        val winningScatter = ahead.map {
            ConversionPoint(it.conversionAccuracy, it.convertedWin)
        }
        val losingScatter = behind.map {
            ConversionPoint(it.middlegameDefenseAccuracy, it.savedLoss)
        }

        return ConversionReport(
            whenAhead             = whenAhead,
            whenBehind            = whenBehind,
            winningScatter        = winningScatter,
            losingScatter         = losingScatter,
            missedSimplifications = missedSimplifications,
        )
    }

    private fun pct(n: Int, total: Int): Int = if (total == 0) 0 else n * 100 / total
    private fun List<Float>.avgOrZero(): Float = if (isEmpty()) 0f else average().toFloat()
}
