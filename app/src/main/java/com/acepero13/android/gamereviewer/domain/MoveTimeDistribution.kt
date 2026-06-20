package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.data.model.MoveTimeData

/**
 * Average move time (seconds) split by how the user stood when the move was played —
 * surfaces "blitzing" through critical moments while ahead or behind.
 */
object MoveTimeDistribution {

    /** Mover-perspective eval (cp) at/above which a position is clearly winning. */
    private const val WINNING_CP = 200
    /** Mover-perspective eval (cp) at/below which a position is clearly losing. */
    private const val LOSING_CP = -200

    data class Result(val avgWinningSec: Float, val avgLosingSec: Float)

    /**
     * @param userEvals the user's own half-moves.
     * @param moveTimes clock-derived time per half-move (may be empty when the PGN had no `[%clk]`).
     */
    fun compute(
        userEvals: List<GameEvaluation>,
        moveTimes: List<MoveTimeData>,
        isWhite: Boolean,
    ): Result {
        if (moveTimes.isEmpty()) return Result(0f, 0f)
        val timeByIndex = moveTimes.associate { it.moveIndex to it.timeSpentSeconds }

        val winning = mutableListOf<Int>()
        val losing = mutableListOf<Int>()
        userEvals.forEach { ev ->
            val secs = timeByIndex[ev.moveIndex] ?: return@forEach
            when {
                MoveMetrics.moverEvalBefore(ev, isWhite) >= WINNING_CP -> winning += secs
                MoveMetrics.moverEvalBefore(ev, isWhite) <= LOSING_CP -> losing += secs
            }
        }
        return Result(winning.avgOrZero(), losing.avgOrZero())
    }

    private fun List<Int>.avgOrZero(): Float = if (isEmpty()) 0f else average().toFloat()
}
