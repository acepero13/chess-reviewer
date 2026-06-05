package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.data.model.MoveTimeData

/**
 * Categorises each half-move as one of five decision types by crossing two axes:
 *   • Speed axis  : FAST (≤ [FAST_MOVE_SECONDS] s) | SLOW (≥ [SLOW_MOVE_SECONDS] s) | normal
 *   • Quality axis: BLUNDER (centipawn loss ≥ [BLUNDER_THRESHOLD_CP]) | OK
 *
 * Used by the Game Report (Task 4.2) to draw the Decision Velocity chart and to
 * contribute data to the BehavioralDiagnostic (Task 4.3).
 */
object TimeAnalyzer {

    // ── Thresholds ──────────────────────────────────────────────────────────────

    /** Moves faster than this are "rushed" (likely intuition-only). */
    const val FAST_MOVE_SECONDS = 10

    /** Moves slower than this are "careful" (deep calculation). */
    const val SLOW_MOVE_SECONDS = 60

    /** Centipawn loss from the moving player's perspective that constitutes a blunder. */
    const val BLUNDER_THRESHOLD_CP = 150

    // ── Model ───────────────────────────────────────────────────────────────────

    /**
     * Qualitative label for a single half-move decision.
     *
     * | DecisionType      | Speed   | Quality |
     * |-------------------|---------|---------|
     * | RUSHED_BLUNDER    | fast    | blunder |
     * | RUSHED_OK         | fast    | ok      |
     * | CAREFUL_BLUNDER   | slow    | blunder |
     * | CAREFUL_OK        | slow    | ok      |
     * | NORMAL            | normal  | ok      |
     */
    enum class DecisionType {
        /** Fast move that turned out to be a blunder — the classic "time-pressure mistake". */
        RUSHED_BLUNDER,
        /** Fast move that was acceptable — good intuition. */
        RUSHED_OK,
        /** Slow move that still ended up as a blunder — calculation failed or wrong candidate. */
        CAREFUL_BLUNDER,
        /** Slow, well-considered move that held quality — solid play. */
        CAREFUL_OK,
        /** Medium speed, acceptable quality — baseline move. */
        NORMAL,
    }

    /**
     * All data associated with one half-move decision.
     *
     * @param moveIndex      1-based move index (matches [GameEvaluation.moveIndex]).
     * @param timeSpentSeconds Time spent on this move (from [MoveTimeData]).
     * @param evalDeltaCp   Centipawn delta from the player's perspective (positive = loss).
     *                      Derived from [GameEvaluation.evalDelta] adjusted for side-to-move.
     * @param isBlunder     Convenience flag: `evalDeltaCp >= BLUNDER_THRESHOLD_CP`.
     * @param decisionType  Compound label from the speed × quality matrix.
     */
    data class MoveDecision(
        val moveIndex: Int,
        val timeSpentSeconds: Int,
        val evalDeltaCp: Int,
        val isBlunder: Boolean,
        val decisionType: DecisionType,
    )

    // ── Public API ──────────────────────────────────────────────────────────────

    /**
     * Cross-joins [evaluations] with [moveTimes] by `moveIndex` to produce
     * a [MoveDecision] list.  Moves present in one list but absent in the other
     * are silently skipped.
     *
     * [GameEvaluation.evalDelta] is stored as White-perspective change; this function
     * flips it for Black's moves so [MoveDecision.evalDeltaCp] always represents
     * the loss for the player who just moved.
     */
    fun analyze(
        evaluations: List<GameEvaluation>,
        moveTimes: List<MoveTimeData>,
    ): List<MoveDecision> {
        val evalByIndex = evaluations.associateBy { it.moveIndex }
        val timeByIndex = moveTimes.associateBy  { it.moveIndex }

        return moveTimes.mapNotNull { mt ->
            val ev = evalByIndex[mt.moveIndex] ?: return@mapNotNull null
            val isWhiteMove = mt.moveIndex % 2 == 1          // move 1=W, 2=B, 3=W …

            // evalDelta is White-perspective change; for Black a *positive* delta means
            // White improved → Black blundered → negate to get Black's loss
            val playerLoss = if (isWhiteMove) -ev.evalDelta else ev.evalDelta
            val cpLoss     = maxOf(0, playerLoss)

            val isBlunder  = cpLoss >= BLUNDER_THRESHOLD_CP
            val type = when {
                mt.timeSpentSeconds <= FAST_MOVE_SECONDS && isBlunder  -> DecisionType.RUSHED_BLUNDER
                mt.timeSpentSeconds <= FAST_MOVE_SECONDS               -> DecisionType.RUSHED_OK
                mt.timeSpentSeconds >= SLOW_MOVE_SECONDS && isBlunder  -> DecisionType.CAREFUL_BLUNDER
                mt.timeSpentSeconds >= SLOW_MOVE_SECONDS               -> DecisionType.CAREFUL_OK
                else                                                    -> DecisionType.NORMAL
            }
            MoveDecision(
                moveIndex        = mt.moveIndex,
                timeSpentSeconds = mt.timeSpentSeconds,
                evalDeltaCp      = cpLoss,
                isBlunder        = isBlunder,
                decisionType     = type,
            )
        }
    }

    // ── Aggregate helpers used by GameReportViewModel ────────────────────────

    /** Count of RUSHED_BLUNDER moves in a decision list. */
    fun countRushedBlunders(decisions: List<MoveDecision>): Int =
        decisions.count { it.decisionType == DecisionType.RUSHED_BLUNDER }

    /** Count of CAREFUL_BLUNDER moves in a decision list. */
    fun countCarefulBlunders(decisions: List<MoveDecision>): Int =
        decisions.count { it.decisionType == DecisionType.CAREFUL_BLUNDER }

    /** Average time spent on moves that turned out to be blunders. */
    fun avgTimeOnBlunders(decisions: List<MoveDecision>): Float {
        val blunders = decisions.filter { it.isBlunder }
        return if (blunders.isEmpty()) 0f else blunders.map { it.timeSpentSeconds }.average().toFloat()
    }

    /** Average time spent on non-blunder moves. */
    fun avgTimeOnGoodMoves(decisions: List<MoveDecision>): Float {
        val good = decisions.filter { !it.isBlunder }
        return if (good.isEmpty()) 0f else good.map { it.timeSpentSeconds }.average().toFloat()
    }

    /**
     * Move indices (1-based) where the player spent a long time calculating but
     * still produced a blunder — the classic "overthought" failure mode.
     * These positions are candidates for Blunder Guard reflection prompts.
     */
    fun overthougtMoveIndices(decisions: List<MoveDecision>): Set<Int> =
        decisions
            .filter { it.decisionType == DecisionType.CAREFUL_BLUNDER }
            .map { it.moveIndex }
            .toSet()

    /** Population standard deviation of time-per-move for a single game's decisions. */
    fun timeStdDev(decisions: List<MoveDecision>): Float {
        if (decisions.size < 2) return 0f
        val times = decisions.map { it.timeSpentSeconds.toFloat() }
        val mean  = times.average().toFloat()
        return kotlin.math.sqrt(times.map { (it - mean) * (it - mean) }.average().toFloat())
    }
}
