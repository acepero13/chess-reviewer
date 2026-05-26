package com.acepero13.android.gamereviewer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import com.acepero13.chess.core.data.db.PositionAnnotationDao
import com.acepero13.chess.core.data.model.PositionAnnotation

/**
 * Single Room database for Game Reviewer.
 *
 * Entities:
 * - [ReviewGame]          — imported games (app-specific)
 * - [PositionAnnotation]  — board arrows + move comments (from chess-core)
 * - [CriticalMoment]      — engine-detected / user-marked key positions
 *
 * Version history:
 *   1 → initial schema (ReviewGame, PositionAnnotation)
 *   2 → added CriticalMoment table
 */
@Database(
    entities = [
        ReviewGame::class,
        PositionAnnotation::class,  // from chess-core library
        CriticalMoment::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reviewGameDao(): ReviewGameDao
    abstract fun annotationDao(): PositionAnnotationDao  // from chess-core
    abstract fun criticalMomentDao(): CriticalMomentDao
}
