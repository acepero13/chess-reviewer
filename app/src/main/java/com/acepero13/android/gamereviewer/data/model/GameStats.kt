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
    val openingEco: String = "",
    val openingName: String = "",
    val analysisDepth: Int,
    val analyzedAt: Long = System.currentTimeMillis(),
)
