package com.acepero13.android.gamereviewer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.data.model.EndgameEncounter
import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.data.model.GameStats
import com.acepero13.android.gamereviewer.data.model.GuessMoveProgress
import com.acepero13.android.gamereviewer.data.model.GuessMoveSession
import com.acepero13.android.gamereviewer.data.model.MoveTimeData
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import com.acepero13.android.gamereviewer.data.model.Snippet
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
 *   5 → added endgame_encounters table (endgame chapter recognition)
 *   6 → added pvLine column to game_evaluations (forcing sequence PV storage)
 *   7 → added guess_move_sessions table (Guess the Move training feature)
 *   8 → added snippets table (Snippet Library feature)
 *   9 → added whitePlayer, blackPlayer columns to snippets
 *  10 → added lastReviewedMoveIndex to review_games; added guess_move_progress table
 *  11 → added game_stats table (Insights: precomputed per-game Chess.com-style stats)
 */
@Database(
    entities    = [
        ReviewGame::class,
        PositionAnnotation::class,
        CriticalMoment::class,
        GameEvaluation::class,
        MoveTimeData::class,
        EndgameEncounter::class,
        GuessMoveSession::class,
        Snippet::class,
        GuessMoveProgress::class,
        GameStats::class,
    ],
    version     = 11,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reviewGameDao(): ReviewGameDao
    abstract fun annotationDao(): PositionAnnotationDao
    abstract fun criticalMomentDao(): CriticalMomentDao
    abstract fun gameEvaluationDao(): GameEvaluationDao
    abstract fun moveTimeDao(): MoveTimeDao
    abstract fun endgameEncounterDao(): EndgameEncounterDao
    abstract fun guessMoveSessionDao(): GuessMoveSessionDao
    abstract fun snippetDao(): SnippetDao
    abstract fun guessMoveProgressDao(): GuessMoveProgressDao
    abstract fun gameStatsDao(): GameStatsDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE game_evaluations ADD COLUMN coachingTriggers TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS endgame_encounters (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        gameId INTEGER NOT NULL,
                        moveIndex INTEGER NOT NULL,
                        chapter INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        name TEXT NOT NULL,
                        fen TEXT NOT NULL,
                        hadMistake INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE game_evaluations ADD COLUMN pvLine TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS guess_move_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        gameDescription TEXT NOT NULL,
                        sourceLabel TEXT NOT NULL,
                        totalMoves INTEGER NOT NULL,
                        exactMatches INTEGER NOT NULL,
                        guessingSide TEXT NOT NULL,
                        completedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS snippets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        fen TEXT NOT NULL,
                        sourceGameId INTEGER,
                        moveIndex INTEGER NOT NULL DEFAULT 0,
                        tags TEXT NOT NULL DEFAULT '',
                        notes TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE snippets ADD COLUMN whitePlayer TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE snippets ADD COLUMN blackPlayer TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE review_games ADD COLUMN lastReviewedMoveIndex INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS guess_move_progress (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        gameIndex INTEGER NOT NULL,
                        gameDescription TEXT NOT NULL,
                        sourceLabel TEXT NOT NULL,
                        currentMoveIndex INTEGER NOT NULL,
                        totalMoves INTEGER NOT NULL,
                        exactMatches INTEGER NOT NULL,
                        totalPresented INTEGER NOT NULL,
                        guessingSide TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS game_stats (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        gameId INTEGER NOT NULL,
                        playerIsWhite INTEGER NOT NULL,
                        accuracy REAL NOT NULL,
                        acpl INTEGER NOT NULL,
                        blunders INTEGER NOT NULL,
                        mistakes INTEGER NOT NULL,
                        inaccuracies INTEGER NOT NULL,
                        openingAccuracy REAL NOT NULL,
                        middlegameAccuracy REAL NOT NULL,
                        endgameAccuracy REAL NOT NULL,
                        middlegameAttackAccuracy REAL NOT NULL,
                        middlegameDefenseAccuracy REAL NOT NULL,
                        conversionAccuracy REAL NOT NULL,
                        accuracyStdDev REAL NOT NULL,
                        rushedBlunderRate REAL NOT NULL,
                        openingEco TEXT NOT NULL DEFAULT '',
                        openingName TEXT NOT NULL DEFAULT '',
                        analysisDepth INTEGER NOT NULL,
                        analyzedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_game_stats_gameId ON game_stats(gameId)"
                )
            }
        }
    }
}
