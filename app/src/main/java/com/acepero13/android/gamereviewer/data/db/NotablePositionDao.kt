package com.acepero13.android.gamereviewer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.acepero13.android.gamereviewer.data.model.NotablePosition

@Dao
interface NotablePositionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(positions: List<NotablePosition>)

    /** All notable positions of a given [NotablePosition.Kind] name, across every game. */
    @Query("SELECT * FROM notable_positions WHERE kind = :kind ORDER BY evalBeforeCp DESC")
    suspend fun getByKind(kind: String): List<NotablePosition>

    @Query("SELECT * FROM notable_positions")
    suspend fun getAll(): List<NotablePosition>

    @Query("DELETE FROM notable_positions WHERE gameId = :gameId")
    suspend fun deleteByGameId(gameId: Long)

    @Query("DELETE FROM notable_positions")
    suspend fun deleteAll()
}
