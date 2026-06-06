package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.domain.TruthMapEntry

internal class MotifMapper {

    fun motifToReason(entry: TruthMapEntry): String = when {
        entry.motif == "checkmate"   -> CriticalMoment.ReasonCategory.MISSED_WIN.name
        entry.motif == "hanging"     -> CriticalMoment.ReasonCategory.HANGING_PIECE.name
        entry.motif == "fork"        -> CriticalMoment.ReasonCategory.MISSED_TACTIC.name
        entry.moveIndex <= 20        -> CriticalMoment.ReasonCategory.OPENING_DEVIATION.name
        isEndgamePosition(entry.fen) -> CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE.name
        entry.playerEvalDelta <= -400 -> CriticalMoment.ReasonCategory.MISSED_WIN.name
        else                         -> CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE.name
    }

    fun isEndgamePosition(fen: String): Boolean {
        val placement = fen.substringBefore(' ')
        val queens    = placement.count { it == 'Q' || it == 'q' }
        val rooks     = placement.count { it == 'R' || it == 'r' }
        val minors    = placement.count { it in "BbNn" }
        return queens == 0 && (rooks + minors) <= 4
    }

    fun buildPhaseSummary(totalMoves: Int): String = when {
        totalMoves <= 10 -> "Short game · ${totalMoves / 2} moves each side"
        totalMoves <= 30 -> "Middlegame battle · $totalMoves half-moves"
        else             -> "Full game · ${totalMoves / 2} moves"
    }
}
