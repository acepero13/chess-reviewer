package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.data.model.ReviewGame

/**
 * Per-game self-awareness data point: how many of the engine's critical positions
 * the player independently flagged vs. scrolled past.
 */
data class SelfAwarenessTrendPoint(
    val gameId:     Long,
    val date:       String,
    val importedAt: Long,
    val noticed:    Int,   // engine-critical positions the user also marked
    val total:      Int,   // total engine-critical positions in this game
) {
    val score: Float get() = if (total == 0) 0f else noticed.toFloat() / total
    val label: String get() = date.ifBlank { "Game $gameId" }
}

object SelfAwarenessTrend {

    /**
     * Builds a time-ordered series of self-awareness scores, one per game.
     * Games without engine-marked moments are excluded (no data to compare against).
     */
    fun compute(
        moments: List<CriticalMoment>,
        games: List<ReviewGame>,
    ): List<SelfAwarenessTrendPoint> {
        val gameMap = games.associateBy { it.id }
        val byGame  = moments.groupBy { it.gameId }

        return byGame.mapNotNull { (gameId, gameMoments) ->
            val game = gameMap[gameId] ?: return@mapNotNull null
            val engineMoments = gameMoments.filter {
                it.type == CriticalMoment.Type.ENGINE_MARKED.name
            }
            if (engineMoments.isEmpty()) return@mapNotNull null

            val userIndices = gameMoments
                .filter { it.type == CriticalMoment.Type.USER_MARKED.name }
                .map { it.moveIndex }
                .toSet()

            SelfAwarenessTrendPoint(
                gameId     = gameId,
                date       = game.date,
                importedAt = game.importedAt,
                noticed    = engineMoments.count { it.moveIndex in userIndices },
                total      = engineMoments.size,
            )
        }.sortedBy { it.importedAt }
    }

    /** Average self-awareness score across all data points. */
    fun averageScore(points: List<SelfAwarenessTrendPoint>): Float =
        if (points.isEmpty()) 0f else points.map { it.score }.average().toFloat()

    /**
     * True if the trend is improving: the second half of [points] has a higher
     * average score than the first half.
     */
    fun isImproving(points: List<SelfAwarenessTrendPoint>): Boolean {
        if (points.size < 4) return false
        val mid  = points.size / 2
        val first  = points.subList(0, mid).map { it.score }.average()
        val second = points.subList(mid, points.size).map { it.score }.average()
        return second > first + 0.05f   // require at least 5% improvement
    }
}
