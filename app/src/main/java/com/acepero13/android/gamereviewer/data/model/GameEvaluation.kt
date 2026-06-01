package com.acepero13.android.gamereviewer.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores the Stockfish evaluation for every position in a game.
 *
 * Populated silently during background analysis (Milestone 1, Task 1.2).
 * Used by:
 * - [AnalysisViewModel] to reconstruct the in-memory truth map for previously
 *   analyzed games, avoiding redundant engine re-runs.
 * - [GameReportViewModel] to render the Decision Velocity chart (Task 4.2).
 *
 * Never shown to the user as raw numbers until they request the full report.
 */
@Entity(
    tableName = "game_evaluations",
    indices   = [Index(value = ["gameId", "moveIndex"], unique = true)],
)
data class GameEvaluation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val moveIndex: Int,          // 1-based half-move number
    val evalCp: Int,             // centipawns from White's perspective (+= White better)
    val evalDelta: Int = 0,      // swing vs. previous position (negative = mover lost eval)
    val motif: String = "mixed", // MotifClassifier label: checkmate/fork/hanging/mixed
    /** Comma-separated [CoachingTrigger] type names detected at this position, e.g. "SAFETY,WORST_PIECE".
     *  Empty string when no triggers fire or when the game predates trigger detection (DB v4+). */
    val coachingTriggers: String = "",
    /** Comma-separated UCI moves from the engine's principal variation (up to 6 half-moves).
     *  Empty when no PV was captured or for games analyzed before DB v6. */
    val pvLine: String = "",
)
