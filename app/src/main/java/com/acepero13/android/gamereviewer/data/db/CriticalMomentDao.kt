package com.acepero13.android.gamereviewer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import kotlinx.coroutines.flow.Flow

@Dao
interface CriticalMomentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(moments: List<CriticalMoment>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(moment: CriticalMoment): Long

    @Update
    suspend fun update(moment: CriticalMoment)

    @Query("SELECT * FROM critical_moments WHERE gameId = :gameId ORDER BY moveIndex ASC")
    suspend fun getByGameId(gameId: Long): List<CriticalMoment>

    @Query("SELECT * FROM critical_moments WHERE gameId = :gameId ORDER BY moveIndex ASC")
    fun observeByGameId(gameId: Long): Flow<List<CriticalMoment>>

    /** Remove stale engine analysis before re-running. */
    @Query("DELETE FROM critical_moments WHERE gameId = :gameId AND type = 'ENGINE_MARKED'")
    suspend fun deleteEngineMarkedForGame(gameId: Long)

    @Query("SELECT COUNT(*) FROM critical_moments WHERE gameId = :gameId AND type = 'ENGINE_MARKED'")
    suspend fun countEngineMarked(gameId: Long): Int

    /** All moments across every game — used by the behavioral diagnosis dashboard. */
    @Query("SELECT * FROM critical_moments ORDER BY gameId ASC, moveIndex ASC")
    suspend fun getAll(): List<CriticalMoment>

    /** Number of distinct games that have at least one ENGINE_MARKED moment. */
    @Query("SELECT COUNT(DISTINCT gameId) FROM critical_moments WHERE type = 'ENGINE_MARKED'")
    suspend fun countGamesAnalyzed(): Int

    /** Wipe all critical moments. Used by the "Clear all data" Settings action. */
    @Query("DELETE FROM critical_moments")
    suspend fun deleteAll()
}
