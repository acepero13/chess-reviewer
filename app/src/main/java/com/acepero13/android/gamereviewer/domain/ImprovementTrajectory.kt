package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.data.model.ReviewGame

/**
 * Compares the first half vs second half of analyzed games for a given failure
 * pattern to reveal whether the player is actually improving on their top weakness.
 *
 * Requires at least 4 analyzed games to produce a meaningful result.
 */
data class ImprovementTrajectory(
    val patternTitle:    String,
    val emoji:           String,
    val firstHalfRate:   Float,   // fraction of first-half games showing the pattern
    val secondHalfRate:  Float,   // fraction of second-half games showing the pattern
    val firstHalfCount:  Int,     // games in first half showing pattern
    val secondHalfCount: Int,     // games in second half showing pattern
    val firstHalfGames:  Int,     // total analyzed games in first half
    val secondHalfGames: Int,     // total analyzed games in second half
    val isImproving:     Boolean,
) {
    val deltaPercent: Int
        get() = ((secondHalfRate - firstHalfRate) * 100).toInt()
}

object ImprovementTrajectoryAnalyzer {

    /**
     * Returns null when fewer than 4 analyzed games exist (not enough data).
     *
     * @param topTrend The top behavioral failure trend to track.
     */
    fun compute(
        moments:  List<CriticalMoment>,
        games:    List<ReviewGame>,
        topTrend: BehavioralDiagnostic.FailureTrend,
    ): ImprovementTrajectory? {
        val analyzedIds = moments
            .filter { it.type == CriticalMoment.Type.ENGINE_MARKED.name }
            .map { it.gameId }
            .toSet()

        val sorted = games
            .filter { it.id in analyzedIds }
            .sortedBy { it.importedAt }

        if (sorted.size < 4) return null

        val mid        = sorted.size / 2
        val firstHalf  = sorted.subList(0, mid)
        val secondHalf = sorted.subList(mid, sorted.size)

        val byGame = moments
            .filter { it.type == CriticalMoment.Type.ENGINE_MARKED.name }
            .groupBy { it.gameId }

        fun gamesWithPattern(list: List<ReviewGame>): Int =
            list.count { game ->
                byGame[game.id]?.any { m -> m.toReason() in topTrend.triggerCategories } == true
            }

        val firstCount  = gamesWithPattern(firstHalf)
        val secondCount = gamesWithPattern(secondHalf)
        val firstRate   = firstCount.toFloat() / firstHalf.size
        val secondRate  = secondCount.toFloat() / secondHalf.size

        return ImprovementTrajectory(
            patternTitle    = topTrend.title,
            emoji           = topTrend.emoji,
            firstHalfRate   = firstRate,
            secondHalfRate  = secondRate,
            firstHalfCount  = firstCount,
            secondHalfCount = secondCount,
            firstHalfGames  = firstHalf.size,
            secondHalfGames = secondHalf.size,
            isImproving     = secondRate < firstRate - 0.05f,
        )
    }
}
