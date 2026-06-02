package com.acepero13.android.gamereviewer.domain

// ── Thresholds ─────────────────────────────────────────────────────────────────

/** Rolling-avg |evalDelta| (cp) at which the board is considered a tactical storm. */
const val VOLATILITY_HIGH_CP    = 60f

/** Average attackers-per-piece at which the board is considered congested. */
const val PRESSURE_HIGH         = 1.5f

/** Heuristic complexity gap (cp) above which one move is clearly forced. */
const val FORCED_MOVE_GAP_CP    = 150

/** Heuristic complexity gap (cp) below which no single move stands out — genuine strategy choice. */
const val STRATEGIC_GAP_MAX_CP  = 60

/** Player's eval-slope threshold (cp/move) for "cruising" — steadily increasing advantage. */
const val CRUISING_SLOPE_CP     = 10f

/** Player's eval-slope threshold (cp/move) for "stumbling" — leaking a held advantage. */
const val STUMBLING_SLOPE_CP    = -30f

// ── Data class ─────────────────────────────────────────────────────────────────

/**
 * A snapshot of the game's narrative at one position, computed per-move inside
 * [CoachingTriggerEvaluator] from rolling windows and board geometry.
 *
 * Used to gate, suppress, and boost coaching triggers based on *context* rather than
 * isolated positional heuristics:
 *
 * - [isVolatile] → suppress all Tier 4 positional advice (tactical storm ongoing).
 * - [isHighPressure] → additionally suppress [CoachingTrigger.RookActivation] (congested board).
 * - [hasForcedBestMove] → suppress [CoachingTrigger.CandidateMoves] / [CoachingTrigger.CandidateSearch].
 * - [isCruising] → suppress strategic "search for a plan" coaching (player already understands it).
 * - [isStumbling] → boost strategic coaching priority (player is leaking a won game).
 */
data class GameNarrativeContext(
    /** Rolling average of |evalDelta| over the last 10 half-moves (cp). */
    val volatility: Float,
    /** Average number of opponent attackers per piece on the board. */
    val pressure: Float,
    /** Rate of evaluation change from the player's perspective (cp/move) over the last 5 moves. */
    val playerEvalSlope: Float,
    /** Heuristic gap (cp) between the best and second-best move. Derived from moverLoss + motif. */
    val complexityGap: Int,
    /** Current evaluation from the player's perspective (positive = player ahead). */
    val playerEvalCp: Int,
    /** Coaching trigger types the player has historically struggled to recognise. */
    val weakTriggerTypes: Set<String>,
) {
    val isVolatile: Boolean
        get() = volatility >= VOLATILITY_HIGH_CP

    val isHighPressure: Boolean
        get() = pressure >= PRESSURE_HIGH

    /** True when the board is "hot" — all Tier 4 positional coaching should be silent. */
    val isSuppressTier4: Boolean
        get() = isVolatile || isHighPressure

    /** True when one move is clearly far superior — strategic planning advice is distracting. */
    val hasForcedBestMove: Boolean
        get() = complexityGap >= FORCED_MOVE_GAP_CP

    /** True when multiple moves are roughly equal — positional planning questions are relevant. */
    val isStrategicChoice: Boolean
        get() = complexityGap <= STRATEGIC_GAP_MAX_CP

    /** True when the player's advantage is growing steadily — they already know the plan. */
    val isCruising: Boolean
        get() = playerEvalSlope >= CRUISING_SLOPE_CP && playerEvalCp > 50 && !isVolatile

    /** True when the player had an advantage but is slowly losing it — they need a plan. */
    val isStumbling: Boolean
        get() = playerEvalSlope <= STUMBLING_SLOPE_CP && playerEvalCp > 50
}
