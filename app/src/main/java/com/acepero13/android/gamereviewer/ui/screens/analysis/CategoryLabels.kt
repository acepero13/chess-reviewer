package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.data.model.CriticalMoment

internal object CategoryLabels {

    fun label(cat: CriticalMoment.ReasonCategory) = when (cat) {
        CriticalMoment.ReasonCategory.MISSED_TACTIC     -> "Missed a tactical pattern"
        CriticalMoment.ReasonCategory.HANGING_PIECE     -> "Left a piece undefended"
        CriticalMoment.ReasonCategory.KING_SAFETY       -> "King safety neglected"
        CriticalMoment.ReasonCategory.OPENING_DEVIATION -> "Opening principle violated"
        CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE -> "Endgame technique error"
        CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE -> "Strategic / positional error"
        CriticalMoment.ReasonCategory.TIME_PRESSURE     -> "Rushed under time pressure"
        CriticalMoment.ReasonCategory.MISSED_WIN        -> "Missed a winning resource"
    }

    fun description(cat: CriticalMoment.ReasonCategory) = when (cat) {
        CriticalMoment.ReasonCategory.MISSED_TACTIC     -> "Fork, pin, skewer or other combination"
        CriticalMoment.ReasonCategory.HANGING_PIECE     -> "Undefended material left en prise"
        CriticalMoment.ReasonCategory.KING_SAFETY       -> "Attack on the king went unnoticed"
        CriticalMoment.ReasonCategory.OPENING_DEVIATION -> "Development or opening-rule breach"
        CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE -> "Incorrect endgame technique"
        CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE -> "Long-term positional weakness created"
        CriticalMoment.ReasonCategory.TIME_PRESSURE     -> "Move played too quickly"
        CriticalMoment.ReasonCategory.MISSED_WIN        -> "Decisive resource or mate was available"
    }

    fun dominant(moments: List<CriticalMoment>): CriticalMoment.ReasonCategory? =
        moments.groupBy { it.reasonCategory }
            .maxByOrNull { it.value.size }?.key
            ?.let { runCatching { CriticalMoment.ReasonCategory.valueOf(it) }.getOrNull() }
}
