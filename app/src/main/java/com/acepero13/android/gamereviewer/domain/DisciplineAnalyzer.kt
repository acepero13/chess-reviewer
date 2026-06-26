package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.GameStats
import com.acepero13.android.gamereviewer.data.model.MoveTimeData

/**
 * Aggregated time-discipline view — backs the **Discipline** tab.
 *
 * Combines cached per-game flags ([GameStats]) with cross-game per-move clock data
 * ([MoveTimeData]) to tell the composure story: when the user rushes, when they flag, and how
 * their time budget is spread across the three game phases versus their opponents.
 */
data class DisciplineReport(
    val gamesInTimePressure: Int,
    val flaggedOnTime: Int,
    val blundersUnderPressure: Int,
    val decisiveBlunders: Int,
    val composure: List<ComposurePoint>,
    val youBudget: PhaseBudget,
    val opponentBudget: PhaseBudget,
) {
    val hasData: Boolean get() = composure.isNotEmpty() ||
        gamesInTimePressure > 0 || blundersUnderPressure > 0 || decisiveBlunders > 0
}

/** Average seconds spent on a given half-move index, user ("you") vs opponent. */
data class ComposurePoint(val moveIndex: Int, val youAvgSec: Float, val oppAvgSec: Float)

/** Share of thinking time spent in each phase (fractions sum to ~1), with avg seconds/move. */
data class PhaseBudget(
    val openingFraction: Float,
    val middlegameFraction: Float,
    val endgameFraction: Float,
    val openingAvgSec: Float,
    val middlegameAvgSec: Float,
    val endgameAvgSec: Float,
) {
    companion object { val EMPTY = PhaseBudget(0f, 0f, 0f, 0f, 0f, 0f) }
}

object DisciplineAnalyzer {

    private const val OPENING_LAST_PLY = 20
    private const val MIDDLEGAME_LAST_PLY = 60
    private const val MAX_PLOTTED_PLY = 80

    fun analyze(
        stats: List<GameStats>,
        moveTimes: List<MoveTimeData>,
    ): DisciplineReport {
        val whiteByGame = stats.associate { it.gameId to it.playerIsWhite }

        // Split every move-time into "you" vs "opponent" using the game's user colour.
        val (yours, theirs) = moveTimes.partition { mt ->
            val userIsWhite = whiteByGame[mt.gameId] ?: true
            (mt.moveIndex % 2 == 1) == userIsWhite
        }

        val composure = (1..MAX_PLOTTED_PLY).mapNotNull { ply ->
            val you = yours.filter { it.moveIndex == ply }.map { it.timeSpentSeconds }
            val opp = theirs.filter { it.moveIndex == ply }.map { it.timeSpentSeconds }
            if (you.isEmpty() && opp.isEmpty()) null
            else ComposurePoint(ply, you.avgOrZero(), opp.avgOrZero())
        }

        return DisciplineReport(
            gamesInTimePressure  = stats.count { it.inTimePressure },
            flaggedOnTime        = stats.count { it.flaggedOnTime },
            blundersUnderPressure = stats.sumOf { it.blundersUnderPressure },
            decisiveBlunders     = stats.sumOf { it.decisiveBlunders },
            composure            = composure,
            youBudget            = budgetOf(yours),
            opponentBudget       = budgetOf(theirs),
        )
    }

    private fun budgetOf(moves: List<MoveTimeData>): PhaseBudget {
        if (moves.isEmpty()) return PhaseBudget.EMPTY
        val opening = moves.filter { it.moveIndex <= OPENING_LAST_PLY }
        val middle  = moves.filter { it.moveIndex in (OPENING_LAST_PLY + 1)..MIDDLEGAME_LAST_PLY }
        val end     = moves.filter { it.moveIndex > MIDDLEGAME_LAST_PLY }
        val total   = moves.sumOf { it.timeSpentSeconds }.toFloat().coerceAtLeast(1f)
        return PhaseBudget(
            openingFraction    = opening.sumOf { it.timeSpentSeconds } / total,
            middlegameFraction = middle.sumOf { it.timeSpentSeconds } / total,
            endgameFraction    = end.sumOf { it.timeSpentSeconds } / total,
            openingAvgSec      = opening.map { it.timeSpentSeconds }.avgOrZero(),
            middlegameAvgSec   = middle.map { it.timeSpentSeconds }.avgOrZero(),
            endgameAvgSec      = end.map { it.timeSpentSeconds }.avgOrZero(),
        )
    }

    private fun List<Int>.avgOrZero(): Float = if (isEmpty()) 0f else average().toFloat()
}
