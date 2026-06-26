package com.acepero13.android.gamereviewer.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single position worth showing as a board thumbnail on the analytics tabs — a missed
 * simplification (Conversion) or a tactical chance found / missed (Tactics).
 *
 * Computed once by [com.acepero13.android.gamereviewer.work.ShallowAnalysisWorker] from the
 * cached [GameEvaluation] rows and the game's UCI move list, then read back for the
 * `NotablePositionCarousel`. Cleared per-game (via [Kind]-agnostic `deleteByGameId`) on recompute.
 */
@Entity(
    tableName = "notable_positions",
    indices = [Index(value = ["gameId"])],
)
data class NotablePosition(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val moveIndex: Int,
    /** FEN of the position the user faced (before their move). */
    val fen: String,
    /** One of [Kind] names. */
    val kind: String,
    /** The move the user actually played, in UCI. */
    val playedMove: String,
    /** The engine's best move at that position, in UCI ("" if unknown). */
    val bestMove: String,
    /** Mover-perspective eval before the move, cp. */
    val evalBeforeCp: Int,
) {
    enum class Kind { MISSED_SIMPLIFICATION, TACTIC_FOUND, TACTIC_MISSED }

    fun kindEnum(): Kind = runCatching { Kind.valueOf(kind) }.getOrDefault(Kind.TACTIC_MISSED)
}
