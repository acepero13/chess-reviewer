package com.acepero13.android.gamereviewer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "guess_move_progress")
data class GuessMoveProgress(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameIndex: Int,
    val gameDescription: String,
    val sourceLabel: String,
    val currentMoveIndex: Int,
    val totalMoves: Int,
    val exactMatches: Int,
    val totalPresented: Int,
    val guessingSide: String,
    val updatedAt: Long = System.currentTimeMillis(),
)
