package com.acepero13.android.gamereviewer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.acepero13.android.gamereviewer.data.model.GuessMoveSession

@Dao
interface GuessMoveSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: GuessMoveSession): Long

    @Query("SELECT * FROM guess_move_sessions ORDER BY completedAt DESC")
    suspend fun getAll(): List<GuessMoveSession>

    @Query("SELECT * FROM guess_move_sessions ORDER BY completedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<GuessMoveSession>

    @Query("DELETE FROM guess_move_sessions")
    suspend fun deleteAll()
}
