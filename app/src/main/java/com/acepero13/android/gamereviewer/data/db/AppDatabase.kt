package com.acepero13.android.gamereviewer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.data.model.MoveTimeData
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import com.acepero13.chess.core.data.db.PositionAnnotationDao
import com.acepero13.chess.core.data.model.PositionAnnotation

/**
 * Single Room database for Game Reviewer.
 *
 * Entities:
 * - [ReviewGame]          — imported games
 * - [PositionAnnotation]  — arrows + move comments (from chess-core)
 * - [CriticalMoment]      — engine-detected / user-marked key positions
 * - [GameEvaluation]      — full per-move Stockfish eval (hidden truth map storage)
 * - [MoveTimeData]        — clock time per move (from PGN [%clk ...] annotations)
 *
 * Version history:
 *   1 → initial (ReviewGame, PositionAnnotation)
 *   2 → added critical_moments
 *   3 → added game_evaluations, move_times
 *   4 → added coachingTriggers column to game_evaluations (Board Scan triggers)
 */
@Database(
    entities    = [
        ReviewGame::class,
        PositionAnnotation::class,
        CriticalMoment::class,
        GameEvaluation::class,
        MoveTimeData::class,
    ],
    version     = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reviewGameDao(): ReviewGameDao
    abstract fun annotationDao(): PositionAnnotationDao
    abstract fun criticalMomentDao(): CriticalMomentDao
    abstract fun gameEvaluationDao(): GameEvaluationDao
    abstract fun moveTimeDao(): MoveTimeDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE game_evaluations ADD COLUMN coachingTriggers TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }
}
