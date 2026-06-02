package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import kotlin.math.abs

object PivotalMomentsSelector {

    /**
     * Identifies the three pedagogically most valuable moments in a game:
     *
     * 1. **Turning Point** — the single half-move with the largest absolute evaluation swing,
     *    regardless of which side caused it. This is where the game's balance shifted.
     *
     * 2. **Missed Opportunity** — the user's worst tactical miss: a position where a concrete
     *    tactical motif existed (fork / hanging / checkmate threat) and the player's evaluation
     *    still dropped ≥ 100 cp. Falls back to the worst non-tactical user move if no tactical
     *    miss is found. Excludes the Turning Point index.
     *
     * 3. **Educational Moment** — the most frequent [CriticalMoment.ReasonCategory] among the
     *    player's ENGINE_MARKED mistakes, picking the most severe representative. Excludes
     *    indices already used by the two slots above.
     *
     * @param truthMap      Full per-half-move engine data for this game.
     * @param criticalMoments ENGINE_MARKED moments for this game (already filtered by type).
     * @param isUserMove    Returns true if the given 1-based move index belongs to the player.
     */
    fun select(
        truthMap: List<TruthMapEntry>,
        criticalMoments: List<CriticalMoment>,
        isUserMove: (Int) -> Boolean,
    ): PivotalMoments {
        if (truthMap.isEmpty()) return PivotalMoments.EMPTY

        val turningPoint = selectTurningPoint(truthMap)
        val usedIndices  = mutableSetOf(turningPoint?.moveIndex)

        val missedOpportunity = selectMissedOpportunity(truthMap, isUserMove, usedIndices)
        missedOpportunity?.moveIndex?.let { usedIndices.add(it) }

        val educationalMoment = selectEducationalMoment(criticalMoments, truthMap, isUserMove, usedIndices)

        return PivotalMoments(
            turningPoint      = turningPoint,
            missedOpportunity = missedOpportunity,
            educationalMoment = educationalMoment,
        )
    }

    private fun selectTurningPoint(truthMap: List<TruthMapEntry>): PivotalMoment? {
        val entry = truthMap
            .filter { abs(it.evalDelta) >= 100 }
            .maxByOrNull { abs(it.evalDelta) }
            ?: return null

        return PivotalMoment(
            moveIndex           = entry.moveIndex,
            fen                 = entry.fen,
            evalDeltaFromPlayer = entry.playerEvalDelta,
            motif               = entry.motif,
            role                = PivotalMomentRole.TURNING_POINT,
        )
    }

    private fun selectMissedOpportunity(
        truthMap: List<TruthMapEntry>,
        isUserMove: (Int) -> Boolean,
        usedIndices: Set<Int?>,
    ): PivotalMoment? {
        val candidates = truthMap.filter { isUserMove(it.moveIndex) && it.moveIndex !in usedIndices }

        // Prefer a concrete tactical miss (motif present + real cp loss)
        val entry = candidates
            .filter { it.hasTacticalMotif && it.playerEvalDelta <= -100 }
            .minByOrNull { it.playerEvalDelta }
            ?: candidates
                .filter { it.playerEvalDelta <= -100 }
                .minByOrNull { it.playerEvalDelta }
            ?: return null

        return PivotalMoment(
            moveIndex           = entry.moveIndex,
            fen                 = entry.fen,
            evalDeltaFromPlayer = entry.playerEvalDelta,
            motif               = entry.motif,
            role                = PivotalMomentRole.MISSED_OPPORTUNITY,
        )
    }

    private fun selectEducationalMoment(
        criticalMoments: List<CriticalMoment>,
        truthMap: List<TruthMapEntry>,
        isUserMove: (Int) -> Boolean,
        usedIndices: Set<Int?>,
    ): PivotalMoment? {
        val userMoments = criticalMoments.filter {
            isUserMove(it.moveIndex) && it.moveIndex !in usedIndices
        }
        if (userMoments.isEmpty()) return null

        val mostFrequentCategory = userMoments
            .groupBy { it.reasonCategory }
            .maxByOrNull { (_, list) -> list.size }
            ?.key ?: return null

        val representative = userMoments
            .filter { it.reasonCategory == mostFrequentCategory }
            .maxByOrNull { it.severity }
            ?: return null

        val truthEntry = truthMap.find { it.moveIndex == representative.moveIndex } ?: return null

        return PivotalMoment(
            moveIndex           = truthEntry.moveIndex,
            fen                 = truthEntry.fen,
            evalDeltaFromPlayer = truthEntry.playerEvalDelta,
            motif               = truthEntry.motif,
            role                = PivotalMomentRole.EDUCATIONAL_MOMENT,
            recurringCategory   = mostFrequentCategory,
        )
    }
}
