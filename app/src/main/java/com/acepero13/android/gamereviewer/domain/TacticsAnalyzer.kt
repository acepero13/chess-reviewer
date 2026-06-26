package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.MotifTacticStat
import com.acepero13.android.gamereviewer.data.model.NotablePosition

/**
 * Aggregated tactical find-rate view — backs the **Tactics** tab.
 *
 * Rolls up per-game [MotifTacticStat] rows into an overall and per-motif "find rate" (how often the
 * user played the engine's best move when a tactic of that type was on the board), plus the
 * board-thumbnail positions they found / missed.
 */
data class TacticsReport(
    val overallFindRatePct: Int,
    val totalOpportunities: Int,
    val motifs: List<MotifFindRate>,
    val strongest: MotifFindRate?,
    val weakest: MotifFindRate?,
    val foundPositions: List<NotablePosition>,
    val missedPositions: List<NotablePosition>,
) {
    val hasData: Boolean get() = totalOpportunities > 0
}

/** Find rate for a single motif. [ratePct] = found / opportunities. */
data class MotifFindRate(
    val motif: String,
    val opportunities: Int,
    val found: Int,
) {
    val ratePct: Int get() = if (opportunities == 0) 0 else found * 100 / opportunities
}

object TacticsAnalyzer {

    /** Stable axis order for the radar so the shape is comparable game-to-game. */
    private val MOTIF_ORDER = listOf("fork", "hanging", "pin", "skewer", "discovered", "mate")

    /** Minimum opportunities before a motif is eligible as strongest / weakest. */
    private const val MIN_OPPORTUNITIES = 2

    fun analyze(
        stats: List<MotifTacticStat>,
        notablePositions: List<NotablePosition>,
    ): TacticsReport {
        val byMotif = stats.groupBy { it.motif }
        val motifs = MOTIF_ORDER.map { motif ->
            val rows = byMotif[motif].orEmpty()
            MotifFindRate(
                motif         = motif,
                opportunities = rows.sumOf { it.opportunities },
                found         = rows.sumOf { it.found },
            )
        }

        val totalOpportunities = motifs.sumOf { it.opportunities }
        val totalFound = motifs.sumOf { it.found }
        val eligible = motifs.filter { it.opportunities >= MIN_OPPORTUNITIES }

        return TacticsReport(
            overallFindRatePct = if (totalOpportunities == 0) 0 else totalFound * 100 / totalOpportunities,
            totalOpportunities = totalOpportunities,
            motifs             = motifs,
            strongest          = eligible.maxByOrNull { it.ratePct },
            weakest            = eligible.minByOrNull { it.ratePct },
            foundPositions     = notablePositions.filter {
                it.kindEnum() == NotablePosition.Kind.TACTIC_FOUND
            },
            missedPositions    = notablePositions.filter {
                it.kindEnum() == NotablePosition.Kind.TACTIC_MISSED
            },
        )
    }
}
