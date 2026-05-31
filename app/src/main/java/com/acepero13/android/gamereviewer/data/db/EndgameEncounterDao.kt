package com.acepero13.android.gamereviewer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.acepero13.android.gamereviewer.data.model.EndgameEncounter

@Dao
interface EndgameEncounterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(encounter: EndgameEncounter)

    @Query("SELECT * FROM endgame_encounters")
    suspend fun getAll(): List<EndgameEncounter>

    @Query("SELECT * FROM endgame_encounters WHERE gameId = :gameId LIMIT 1")
    suspend fun getByGameId(gameId: Long): EndgameEncounter?

    @Query("UPDATE endgame_encounters SET hadMistake = 1 WHERE gameId = :gameId")
    suspend fun markMistake(gameId: Long)
}
