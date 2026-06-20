package com.acepero13.android.gamereviewer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.acepero13.android.gamereviewer.data.model.GameStats

@Dao
interface GameStatsDao {

    /** Replaces any existing row for the same gameId (unique index). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: GameStats)

    @Query("SELECT * FROM game_stats WHERE gameId = :gameId")
    suspend fun getByGameId(gameId: Long): GameStats?

    /** All cached stats, most-recently analyzed first (for recent/prior trend windows). */
    @Query("SELECT * FROM game_stats ORDER BY analyzedAt DESC")
    suspend fun getAll(): List<GameStats>

    /** Ids of imported games that have not been through the shallow stats pass yet. */
    @Query("SELECT id FROM review_games WHERE id NOT IN (SELECT gameId FROM game_stats)")
    suspend fun gameIdsWithoutStats(): List<Long>

    /**
     * Ids of imported games whose stats are missing **or** stale (computed by an older metric
     * schema). Stale-but-already-evaluated games recompute for free (no engine cost).
     */
    @Query(
        """
        SELECT id FROM review_games
        WHERE id NOT IN (SELECT gameId FROM game_stats)
           OR id IN (SELECT gameId FROM game_stats WHERE statsVersion < :version)
        """
    )
    suspend fun gameIdsNeedingStats(version: Int): List<Long>

    @Query("SELECT COUNT(*) FROM game_stats")
    suspend fun count(): Int

    @Query("SELECT AVG(accuracy) FROM game_stats")
    suspend fun avgAccuracy(): Float?

    @Query("DELETE FROM game_stats")
    suspend fun deleteAll()
}
