package com.acepero13.android.gamereviewer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persists one endgame encounter per game: which chapter was reached and whether the
 * player made a critical mistake inside that endgame.
 *
 * One row per game — only the first recognised endgame position in the game is recorded.
 */
@Entity(tableName = "endgame_encounters")
data class EndgameEncounter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val moveIndex: Int,
    val chapter: Int,
    val category: String,
    val name: String,
    val fen: String,
    val hadMistake: Boolean = false,
)
