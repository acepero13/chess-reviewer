package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.CriticalMoment

/**
 * One row of the 3 × N phase/failure-category matrix.
 *
 * @param phase  "Opening", "Middlegame", or "Endgame"
 * @param cells  Category → occurrence count within this phase
 * @param total  Sum of all cell values (handy for % computations in the UI)
 */
data class PhaseFailureRow(
    val phase: String,
    val cells: Map<CriticalMoment.ReasonCategory, Int>,
    val total: Int,
)

/**
 * Builds a 3-row heatmap showing how many engine-marked mistakes fall into
 * each (phase, reason-category) combination.
 *
 * Phase assignment mirrors the logic used in [DashboardViewModel]: explicit
 * reason-category overrides where the engine has already classified the phase,
 * then move-index as a fallback.
 */
object PhaseFailureHeatmap {

    private val PHASES = listOf("Opening", "Middlegame", "Endgame")

    private fun CriticalMoment.phase(): String = when (toReason()) {
        CriticalMoment.ReasonCategory.OPENING_DEVIATION -> "Opening"
        CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE -> "Endgame"
        else -> when {
            moveIndex <= 15 -> "Opening"
            moveIndex <= 40 -> "Middlegame"
            else            -> "Endgame"
        }
    }

    fun compute(moments: List<CriticalMoment>): List<PhaseFailureRow> {
        val engine = moments.filter { it.type == CriticalMoment.Type.ENGINE_MARKED.name }

        return PHASES.map { phase ->
            val inPhase = engine.filter { it.phase() == phase }
            val cells = inPhase
                .groupBy { it.toReason() }
                .mapValues { (_, v) -> v.size }
            PhaseFailureRow(phase = phase, cells = cells, total = inPhase.size)
        }
    }

    /** Human-readable short label for a [CriticalMoment.ReasonCategory]. */
    fun categoryLabel(cat: CriticalMoment.ReasonCategory): String = when (cat) {
        CriticalMoment.ReasonCategory.MISSED_TACTIC     -> "Tactics"
        CriticalMoment.ReasonCategory.OPENING_DEVIATION -> "Opening"
        CriticalMoment.ReasonCategory.HANGING_PIECE     -> "Hanging"
        CriticalMoment.ReasonCategory.KING_SAFETY       -> "King Safety"
        CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE -> "Endgame"
        CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE -> "Strategy"
        CriticalMoment.ReasonCategory.TIME_PRESSURE     -> "Time"
        CriticalMoment.ReasonCategory.MISSED_WIN        -> "Missed Win"
    }
}
