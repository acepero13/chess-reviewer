package com.acepero13.android.gamereviewer.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Clock time spent on each move, extracted from `{ [%clk HH:MM:SS] }` PGN annotations.
 *
 * Populated during game import by [ClockParser]. Used by:
 * - [TimeAnalyzer] to classify moves as rushed vs. careful.
 * - [GameReportViewModel] to render the Decision Velocity chart (Task 4.2).
 */
@Entity(
    tableName = "move_times",
    indices   = [Index(value = ["gameId", "moveIndex"], unique = true)],
)
data class MoveTimeData(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val moveIndex: Int,               // 1-based half-move number
    val timeSpentSeconds: Int,        // seconds consumed by this move
    val clockRemainingSeconds: Int,   // clock reading immediately after the move
)
