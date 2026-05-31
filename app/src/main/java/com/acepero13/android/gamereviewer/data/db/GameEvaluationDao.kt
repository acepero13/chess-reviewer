package com.acepero13.android.gamereviewer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.acepero13.android.gamereviewer.data.model.GameEvaluation

@Dao
interface GameEvaluationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(evaluations: List<GameEvaluation>)

    @Query("SELECT * FROM game_evaluations WHERE gameId = :gameId ORDER BY moveIndex ASC")
    suspend fun getByGameId(gameId: Long): List<GameEvaluation>

    /** True if this game's full evaluation has already been stored. */
    @Query("SELECT COUNT(*) FROM game_evaluations WHERE gameId = :gameId")
    suspend fun countByGameId(gameId: Long): Int

    @Query("DELETE FROM game_evaluations WHERE gameId = :gameId")
    suspend fun deleteByGameId(gameId: Long)

    /** Wipe all evaluation records. Used by the "Clear all data" Settings action. */
    @Query("DELETE FROM game_evaluations")
    suspend fun deleteAll()

    // ── Coaching trigger columns (DB v4+) ─────────────────────────────────────

    /** Returns only rows that have at least one coaching trigger stored. */
    @Query("SELECT * FROM game_evaluations WHERE gameId = :gameId AND coachingTriggers != '' ORDER BY moveIndex ASC")
    suspend fun getTriggerPositions(gameId: Long): List<GameEvaluation>

    @Query("UPDATE game_evaluations SET coachingTriggers = :triggers WHERE gameId = :gameId AND moveIndex = :moveIndex")
    suspend fun updateTriggers(gameId: Long, moveIndex: Int, triggers: String)
}
