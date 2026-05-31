package com.acepero13.android.gamereviewer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewGameDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(games: List<ReviewGame>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(game: ReviewGame): Long

    @Query("SELECT * FROM review_games ORDER BY date DESC, importedAt DESC")
    fun observeAll(): Flow<List<ReviewGame>>

    @Query("SELECT * FROM review_games WHERE id = :id")
    suspend fun findById(id: Long): ReviewGame?

    @Query("SELECT COUNT(*) FROM review_games")
    fun countAll(): Flow<Int>

    @Query("DELETE FROM review_games WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM review_games")
    suspend fun count(): Int

    /** Prevent duplicate imports from the same platform. */
    @Query("SELECT * FROM review_games WHERE sourceType = :sourceType AND sourceId = :sourceId LIMIT 1")
    suspend fun findBySourceId(sourceType: String, sourceId: String): ReviewGame?

    /** Games imported on or after [since] (epoch ms). Used for session debrief. */
    @Query("SELECT * FROM review_games WHERE importedAt >= :since ORDER BY importedAt DESC")
    suspend fun getRecentGames(since: Long): List<ReviewGame>

    /** Count of games imported on or after [since] — reactive, drives debrief banner. */
    @Query("SELECT COUNT(*) FROM review_games WHERE importedAt >= :since")
    fun countRecentGames(since: Long): Flow<Int>

    /** Wipe all imported games. Used by the "Clear all data" Settings action. */
    @Query("DELETE FROM review_games")
    suspend fun deleteAll()
}
