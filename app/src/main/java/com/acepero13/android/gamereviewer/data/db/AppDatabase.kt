package com.acepero13.android.gamereviewer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import com.acepero13.chess.core.data.db.PositionAnnotationDao
import com.acepero13.chess.core.data.model.PositionAnnotation

/**
 * Single Room database for Game Reviewer.
 *
 * Includes:
 * - [ReviewGame] — imported games (app-specific)
 * - [PositionAnnotation] — board arrows + move comments (from chess-core library)
 */
@Database(
    entities = [
        ReviewGame::class,
        PositionAnnotation::class,  // from chess-core library
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reviewGameDao(): ReviewGameDao
    abstract fun annotationDao(): PositionAnnotationDao  // from chess-core
}
