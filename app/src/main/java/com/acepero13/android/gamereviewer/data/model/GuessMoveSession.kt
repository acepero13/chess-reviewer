package com.acepero13.android.gamereviewer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "guess_move_sessions")
data class GuessMoveSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameDescription: String,        // "Kasparov vs Topalov · Wijk aan Zee · 1999"
    val sourceLabel: String,            // "Offline" | "Lichess: DrNykterstein" | "Chess.com: ..."
    val totalMoves: Int,
    val exactMatches: Int,
    val guessingSide: String,           // "BOTH" | "WHITE_ONLY" | "BLACK_ONLY"
    val completedAt: Long = System.currentTimeMillis(),
)
