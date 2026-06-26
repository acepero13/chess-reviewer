package com.acepero13.android.gamereviewer.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Precomputed Chess.com-style statistics for a single analyzed game, from the
 * **user's** perspective. Populated once by the shallow background analysis pass
 * ([com.acepero13.android.gamereviewer.work.ShallowAnalysisWorker]) and read back
 * via instant SQL aggregates on the Insights tab — never recomputed from PGNs.
 *
 * One row per game (unique [gameId]). All accuracy fields are 0–100 percentages
 * using the Lichess accuracy formula (see
 * [com.acepero13.android.gamereviewer.domain.PlayerStatsCalculator]).
 */
@Entity(
    tableName = "game_stats",
    indices = [Index(value = ["gameId"], unique = true)],
)
data class GameStats(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    /** Which side the user played (true = White). Defaults to White when the username can't be matched. */
    val playerIsWhite: Boolean,
    val accuracy: Float,
    /** Average centipawn loss across the user's moves. */
    val acpl: Int,
    val blunders: Int,
    val mistakes: Int,
    val inaccuracies: Int,
    val openingAccuracy: Float,
    val middlegameAccuracy: Float,
    val endgameAccuracy: Float,
    /** Middlegame accuracy on moves where the user was pressing an advantage. */
    val middlegameAttackAccuracy: Float,
    /** Middlegame accuracy on moves where the user was worse / defending. */
    val middlegameDefenseAccuracy: Float,
    /** Accuracy on moves played from a clearly winning position (conversion). */
    val conversionAccuracy: Float,
    /** Standard deviation of per-move accuracy — a tilt / consistency proxy. */
    val accuracyStdDev: Float,
    /** Rate of rushed blunders (fast move that lost material), 0–1. */
    val rushedBlunderRate: Float,

    // ── Move Time Distribution (winning vs losing) ──────────────────────────────
    /** Average seconds spent per move while in a clearly winning position (0 = no clock data). */
    val avgTimeWinningSec: Float = 0f,
    /** Average seconds spent per move while in a clearly losing position (0 = no clock data). */
    val avgTimeLosingSec: Float = 0f,

    // ── Engine correlation in sharp vs quiet positions ──────────────────────────
    val sharpMoveCount: Int = 0,
    val sharpBestMoves: Int = 0,
    val quietMoveCount: Int = 0,
    val quietBestMoves: Int = 0,

    // ── Recurring motif frequency (engine-motif blunders) ───────────────────────
    val forkBlunders: Int = 0,
    val hangingBlunders: Int = 0,

    // ── Oversight recovery (best moves immediately after an inaccuracy+) ─────────
    val oversightCount: Int = 0,
    val oversightRecovered: Int = 0,

    /** Dominant middlegame pawn-structure label (e.g. "Isolated Queen's Pawn"), "" if none. */
    val pawnStructure: String = "",

    // ── Conversion: ahead / behind outcomes (Conversion tab) ────────────────────
    /** The user reached a clearly winning position (mover eval ≥ +200cp) at some point. */
    val reachedWinning: Boolean = false,
    /** Reached a winning position **and** went on to win the game. */
    val convertedWin: Boolean = false,
    /** The user fell into a clearly losing position (mover eval ≤ -200cp) at some point. */
    val reachedLosing: Boolean = false,
    /** Reached a losing position but did **not** lose (draw or win). */
    val savedLoss: Boolean = false,
    /** Peak winning advantage reached, mover-perspective cp (0 if never winning). */
    val peakWinningCp: Int = 0,
    /** Deepest losing disadvantage reached, mover-perspective cp ≤ 0 (0 if never losing). */
    val peakLosingCp: Int = 0,

    // ── Discipline: time pressure (Discipline tab) ──────────────────────────────
    /** The user's clock dropped into time pressure (< 30s remaining) during the game. */
    val inTimePressure: Boolean = false,
    /** The user effectively flagged: clock ran to ~0 and the game was lost. */
    val flaggedOnTime: Boolean = false,
    /** Blunders the user made while under time pressure (low clock). */
    val blundersUnderPressure: Int = 0,
    /** Catastrophic, game-deciding blunders (loss ≥ 300cp). */
    val decisiveBlunders: Int = 0,

    // ── Preparation: opening book depth (Preparation tab) ───────────────────────
    /** How many half-moves the game stayed in opening theory (deepest matched ECO line). */
    val bookDepthPly: Int = 0,

    val openingEco: String = "",
    val openingName: String = "",
    val analysisDepth: Int,
    /** Schema version of the computed metrics; bumped to force a (free) recompute. */
    val statsVersion: Int = 0,
    val analyzedAt: Long = System.currentTimeMillis(),
)
