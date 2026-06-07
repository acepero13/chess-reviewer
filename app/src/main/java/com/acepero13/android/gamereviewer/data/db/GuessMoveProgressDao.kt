package com.acepero13.android.gamereviewer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.acepero13.android.gamereviewer.data.model.GuessMoveProgress

@Dao
interface GuessMoveProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: GuessMoveProgress): Long

    @Query("SELECT * FROM guess_move_progress WHERE gameIndex = :gameIndex LIMIT 1")
    suspend fun findByGameIndex(gameIndex: Int): GuessMoveProgress?

    @Query("DELETE FROM guess_move_progress WHERE gameIndex = :gameIndex")
    suspend fun deleteByGameIndex(gameIndex: Int)

    @Query("SELECT * FROM guess_move_progress ORDER BY updatedAt DESC")
    suspend fun getAll(): List<GuessMoveProgress>
}
