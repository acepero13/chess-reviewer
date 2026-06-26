package com.acepero13.android.gamereviewer.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Per-game, per-motif tactic find-rate — one row per (game, motif). Backs the Tactics tab radar
 * and the strongest / weakest motif breakdown.
 *
 * An *opportunity* is a position the user faced where the engine's best move was a tactic of this
 * [motif]; it counts as *found* when the user played that best move. Computed once by
 * [com.acepero13.android.gamereviewer.work.ShallowAnalysisWorker] from cached [GameEvaluation] rows.
 */
@Entity(
    tableName = "motif_tactic_stats",
    indices = [Index(value = ["gameId"]), Index(value = ["gameId", "motif"], unique = true)],
)
data class MotifTacticStat(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    /** MotifClassifier label: fork / hanging / pin / skewer / discovered / mate. */
    val motif: String,
    val opportunities: Int,
    val found: Int,
)
