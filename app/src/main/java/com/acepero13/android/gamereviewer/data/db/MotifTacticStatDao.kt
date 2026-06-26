package com.acepero13.android.gamereviewer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.acepero13.android.gamereviewer.data.model.MotifTacticStat

@Dao
interface MotifTacticStatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stats: List<MotifTacticStat>)

    @Query("SELECT * FROM motif_tactic_stats")
    suspend fun getAll(): List<MotifTacticStat>

    @Query("DELETE FROM motif_tactic_stats WHERE gameId = :gameId")
    suspend fun deleteByGameId(gameId: Long)

    @Query("DELETE FROM motif_tactic_stats")
    suspend fun deleteAll()
}
