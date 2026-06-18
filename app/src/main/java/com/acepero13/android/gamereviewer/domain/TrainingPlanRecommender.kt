package com.acepero13.android.gamereviewer.domain

data class TrainingRecommendation(
    val category:      TrainingCategory,
    val reason:        String,
    val priority:      Int,
    val isHighlighted: Boolean,
)

object TrainingPlanRecommender {

    fun recommend(
        trends: List<BehavioralDiagnostic.FailureTrend>,
    ): List<TrainingRecommendation> {
        val scores = scoreCategories(trends)
        val sorted = TrainingCategory.entries.sortedByDescending { scores[it] ?: 0 }
        val topScore = scores[sorted.firstOrNull()] ?: 0

        return sorted.mapIndexed { index, category ->
            val score         = scores[category] ?: 0
            val matchingTrends = trends.filter { trend ->
                trend.triggerCategories.any { it in category.targetWeaknesses }
            }
            TrainingRecommendation(
                category      = category,
                reason        = buildReason(matchingTrends),
                priority      = index + 1,
                isHighlighted = index == 0 && score == topScore && topScore > 0,
            )
        }
    }

    private fun scoreCategories(
        trends: List<BehavioralDiagnostic.FailureTrend>,
    ): Map<TrainingCategory, Int> {
        if (trends.isEmpty()) return emptyMap()
        val maxCount = trends.maxOf { it.totalCount }.coerceAtLeast(1)
        val scores = mutableMapOf<TrainingCategory, Int>()

        for (trend in trends) {
            // rank weight: rank 1 → 3, rank 2 → 2, rank 3 → 1
            val rankWeight = maxOf(1, 4 - trend.rank)
            // frequency weight: 1–10 normalized against the most-frequent trend
            val freqWeight = ((trend.totalCount.toFloat() / maxCount) * 10).toInt().coerceAtLeast(1)
            val weight = rankWeight * freqWeight

            for (category in TrainingCategory.entries) {
                if (trend.triggerCategories.any { it in category.targetWeaknesses }) {
                    scores[category] = (scores[category] ?: 0) + weight
                }
            }
        }
        return scores
    }

    private fun buildReason(matching: List<BehavioralDiagnostic.FailureTrend>): String {
        if (matching.isEmpty()) return "General skill development"
        val top = matching.minByOrNull { it.rank }!!
        val others = matching.filter { it.rank != top.rank }
        val gameLabel = if (top.frequency == 1) "game" else "games"
        return if (others.isEmpty()) {
            "#${top.rank} weakness · ${top.title} · ${top.totalCount}× in ${top.frequency} $gameLabel"
        } else {
            val combined = matching.sumOf { it.totalCount }
            "${top.title} + ${others.first().title} · ${combined}× combined"
        }
    }
}
