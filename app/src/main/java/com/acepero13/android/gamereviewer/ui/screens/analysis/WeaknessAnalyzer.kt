package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.domain.BehavioralDiagnostic

internal object WeaknessAnalyzer {

    fun buildWeakTriggerTypes(allMoments: List<CriticalMoment>): Set<String> {
        if (allMoments.isEmpty()) return emptySet()
        val trends = BehavioralDiagnostic.diagnose(allMoments, topN = 2)
        return trends.flatMap { trend ->
            trend.triggerCategories.flatMap { triggerTypesFor(it) }
        }.toSet()
    }

    private fun triggerTypesFor(cat: CriticalMoment.ReasonCategory): List<String> = when (cat) {
        CriticalMoment.ReasonCategory.MISSED_TACTIC,
        CriticalMoment.ReasonCategory.HANGING_PIECE    -> listOf("PRE_MOVE_CHECKLIST", "FORCING_MOVE")
        CriticalMoment.ReasonCategory.KING_SAFETY      -> listOf("SAFETY")
        CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE -> listOf("CANDIDATE_MOVES", "CANDIDATE_SEARCH")
        CriticalMoment.ReasonCategory.MISSED_WIN       -> listOf("FORCING_MOVE", "CCT_CHECK")
        CriticalMoment.ReasonCategory.TIME_PRESSURE    -> listOf("IMPULSE_CONTROL", "CALCULATION_BLUNDER")
        CriticalMoment.ReasonCategory.OPENING_DEVIATION -> listOf("CANDIDATE_MOVES")
        CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE -> listOf("WORST_PIECE", "ROOK_ACTIVATION")
    }
}
