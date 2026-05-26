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
}
