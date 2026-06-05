package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.MoveTimeData
import kotlin.math.sqrt

/**
 * Measures how consistently the player allocates time across moves.
 *
 * A high standard deviation within a game signals erratic pacing: some moves
 * blitzed in seconds, others massively over-thought. This pattern often
 * correlates with clock-management problems and late-game time pressure.
 */
data class VelocityConsistency(
    val avgStdDevSeconds:    Float,   // average per-game time std dev
    val erraticGameCount:    Int,     // games with std dev > ERRATIC_THRESHOLD_SECONDS
    val totalGameCount:      Int,     // games with enough time data
    val erraticGameFraction: Float,   // erraticGameCount / totalGameCount
    val mostConsistentStdDev: Float,  // std dev of the most consistent game
    val mostErraticStdDev:    Float,  // std dev of the most erratic game
)

object VelocityConsistencyAnalyzer {

    /** Games with a per-move time std dev above this are classified as "erratic". */
    const val ERRATIC_THRESHOLD_SECONDS = 30f

    /** Games with fewer than this many timed moves are skipped. */
    private const val MIN_TIMED_MOVES = 5

    /**
     * @param timesByGame Map of gameId → list of move-time records for that game.
     * @return null if no game has enough timed moves.
     */
    fun compute(timesByGame: Map<Long, List<MoveTimeData>>): VelocityConsistency? {
        val valid = timesByGame.values.filter { it.size >= MIN_TIMED_MOVES }
        if (valid.isEmpty()) return null

        val stdDevs = valid.map { times ->
            val ts   = times.map { it.timeSpentSeconds.toFloat() }
            val mean = ts.average().toFloat()
            sqrt(ts.map { (it - mean) * (it - mean) }.average().toFloat())
        }

        val erratic = stdDevs.count { it > ERRATIC_THRESHOLD_SECONDS }

        return VelocityConsistency(
            avgStdDevSeconds     = stdDevs.average().toFloat(),
            erraticGameCount     = erratic,
            totalGameCount       = stdDevs.size,
            erraticGameFraction  = erratic.toFloat() / stdDevs.size,
            mostConsistentStdDev = stdDevs.min(),
            mostErraticStdDev    = stdDevs.max(),
        )
    }
}
