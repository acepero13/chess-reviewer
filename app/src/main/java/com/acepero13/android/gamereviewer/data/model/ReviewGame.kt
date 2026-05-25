package com.acepero13.android.gamereviewer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single imported chess game stored for review.
 *
 * `movesUci` is a space-separated list of UCI moves from the starting position.
 * `sourceType` is one of: "file", "chesscom", "lichess".
 */
@Entity(tableName = "review_games")
data class ReviewGame(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val whitePlayer: String,
    val blackPlayer: String,
    val result: String,                 // "1-0" | "0-1" | "1/2-1/2" | "*"
    val date: String,                   // "YYYY.MM.DD" or empty
    val event: String,
    val movesUci: String,               // space-separated UCI moves
    val pgn: String,                    // original PGN text
    val openingEco: String = "",        // populated after classification
    val openingName: String = "",
    val sourceType: String,             // "file" | "chesscom" | "lichess"
    val sourceId: String = "",          // game ID on the platform if available
    val importedAt: Long = System.currentTimeMillis(),
)
