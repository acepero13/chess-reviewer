package com.acepero13.android.gamereviewer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.acepero13.android.gamereviewer.data.model.MoveTimeData

@Dao
interface MoveTimeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(moveTimes: List<MoveTimeData>)

    @Query("SELECT * FROM move_times WHERE gameId = :gameId ORDER BY moveIndex ASC")
    suspend fun getByGameId(gameId: Long): List<MoveTimeData>

    @Query("SELECT COUNT(*) FROM move_times WHERE gameId = :gameId")
    suspend fun countByGameId(gameId: Long): Int

    /** Wipe all move-time records. Used by the "Clear all data" Settings action. */
    @Query("DELETE FROM move_times")
    suspend fun deleteAll()
}
