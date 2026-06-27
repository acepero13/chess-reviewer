package com.acepero13.android.gamereviewer.di

import androidx.room.Room
import com.acepero13.android.gamereviewer.data.db.AppDatabase
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.data.repository.SettingsRepository
import com.acepero13.android.gamereviewer.data.repository.SnippetRepository
import com.acepero13.android.gamereviewer.data.repository.TriggerMasteryRepository
import com.acepero13.android.gamereviewer.domain.AnalyticsFilterStore
import com.acepero13.android.gamereviewer.domain.EndgameRecognizer
import com.acepero13.android.gamereviewer.domain.MiddlegamePlanDetector
import com.acepero13.android.gamereviewer.domain.OpeningDeviationAnalyzer
import com.acepero13.android.gamereviewer.domain.TruthMapBuilder
import com.acepero13.chess.core.endgame.EndgameClassifier
import com.acepero13.chess.core.middlegame.MiddlegamePlanClassifier
import com.acepero13.android.gamereviewer.ui.screens.AnalysisViewModel
import com.acepero13.android.gamereviewer.ui.screens.DashboardViewModel
import com.acepero13.android.gamereviewer.ui.screens.GameListViewModel
import com.acepero13.android.gamereviewer.ui.screens.GameReportViewModel
import com.acepero13.android.gamereviewer.ui.screens.BlunderInsightsViewModel
import com.acepero13.android.gamereviewer.ui.screens.ConversionViewModel
import com.acepero13.android.gamereviewer.ui.screens.DisciplineViewModel
import com.acepero13.android.gamereviewer.ui.screens.HomeViewModel
import com.acepero13.android.gamereviewer.ui.screens.ImportViewModel
import com.acepero13.android.gamereviewer.ui.screens.InsightsViewModel
import com.acepero13.android.gamereviewer.ui.screens.PreparationViewModel
import com.acepero13.android.gamereviewer.ui.screens.TacticsViewModel
import com.acepero13.android.gamereviewer.ui.screens.GuessTheMoveViewModel
import com.acepero13.android.gamereviewer.ui.screens.SessionDebriefViewModel
import com.acepero13.android.gamereviewer.ui.screens.SettingsViewModel
import com.acepero13.android.gamereviewer.ui.screens.OrphanSnippetViewModel
import com.acepero13.android.gamereviewer.ui.screens.SnippetAnalysisViewModel
import com.acepero13.android.gamereviewer.ui.screens.SnippetLibraryViewModel
import com.acepero13.android.gamereviewer.ui.screens.WeaknessDrillViewModel
import com.acepero13.chess.core.engine.StockfishEngine
import com.acepero13.chess.core.opening.OpeningClassifier
import com.acepero13.chess.core.pgn.PgnImporter
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // ── Database ──────────────────────────────────────────────────────────────
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "game_reviewer.db",
        )
            .addMigrations(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<AppDatabase>().reviewGameDao() }
    single { get<AppDatabase>().annotationDao() }
    single { get<AppDatabase>().criticalMomentDao() }
    single { get<AppDatabase>().gameEvaluationDao() }
    single { get<AppDatabase>().moveTimeDao() }
    single { get<AppDatabase>().endgameEncounterDao() }
    single { get<AppDatabase>().guessMoveSessionDao() }
    single { get<AppDatabase>().snippetDao() }
    single { get<AppDatabase>().guessMoveProgressDao() }
    single { get<AppDatabase>().gameStatsDao() }
    single { get<AppDatabase>().notablePositionDao() }
    single { get<AppDatabase>().motifTacticStatDao() }

    // ── Repositories ──────────────────────────────────────────────────────────
    single { GameRepository(get()) }
    single { SettingsRepository(androidContext()) }
    single { TriggerMasteryRepository(androidContext()) }
    single { SnippetRepository(get()) }

    // ── Chess-core singletons ─────────────────────────────────────────────────
    single { (androidApplication() as com.acepero13.android.gamereviewer.GameReviewerApp).stockfishEngine }
    single { PgnImporter() }
    single { OpeningClassifier(androidContext()) }

    // ── Domain layer ──────────────────────────────────────────────────────────
    single { TruthMapBuilder(get()) }
    single { OpeningDeviationAnalyzer(get()) }
    single { EndgameClassifier() }
    single { EndgameRecognizer(get()) }
    single { MiddlegamePlanClassifier() }
    single { MiddlegamePlanDetector(get()) }
    single { AnalyticsFilterStore() }

    // ── ViewModels ────────────────────────────────────────────────────────────
    viewModel { HomeViewModel(get(), androidContext(), get()) }
    viewModel { GameListViewModel(get(), get()) }
    viewModel { ImportViewModel(get(), get(), get(), get(), get()) }
    viewModel { (gameId: Long) ->
        AnalysisViewModel(
            gameId               = gameId,
            repo                 = get(),
            annotationDao        = get(),
            criticalMomentDao    = get(),
            gameEvaluationDao    = get(),
            moveTimeDao          = get(),
            endgameEncounterDao  = get(),
            engine               = get(),
            opening              = get(),
            truthMapBuilder      = get(),
            settingsRepo         = get(),
            masteryRepo          = get(),
            deviationAnalyzer       = get(),
            endgameRecognizer       = get(),
            middlegamePlanDetector  = get(),
            snippetRepo             = get(),
        )
    }
    viewModel { SnippetLibraryViewModel(get()) }
    viewModel { (snippetId: Long) -> SnippetAnalysisViewModel(snippetId, get()) }
    viewModel { (snippetId: Long) -> OrphanSnippetViewModel(snippetId, get(), get(), get()) }
    viewModel { (gameId: Long) ->
        GameReportViewModel(
            gameId            = gameId,
            repo              = get(),
            evalDao           = get(),
            moveTimeDao       = get(),
            criticalMomentDao = get(),
            annotationDao     = get(),
            settingsRepo      = get(),
            openingClassifier = get(),
        )
    }
    viewModel {
        DashboardViewModel(
            repo              = get(),
            criticalMomentDao = get(),
            masteryRepo       = get(),
            endgameEncounterDao = get(),
            settingsRepo      = get(),
            moveTimeDao       = get(),
            openingClassifier = get(),
            filterStore       = get(),
        )
    }
    viewModel {
        InsightsViewModel(
            gameStatsDao      = get(),
            criticalMomentDao = get(),
            repo              = get(),
            context           = androidContext(),
            filterStore       = get(),
        )
    }
    viewModel {
        BlunderInsightsViewModel(
            gameStatsDao      = get(),
            criticalMomentDao = get(),
            repo              = get(),
            context           = androidContext(),
            filterStore       = get(),
        )
    }
    viewModel {
        ConversionViewModel(
            gameStatsDao       = get(),
            notablePositionDao = get(),
            repo               = get(),
            context            = androidContext(),
            filterStore        = get(),
        )
    }
    viewModel {
        DisciplineViewModel(
            gameStatsDao = get(),
            moveTimeDao  = get(),
            repo         = get(),
            context      = androidContext(),
            filterStore  = get(),
        )
    }
    viewModel {
        PreparationViewModel(
            gameStatsDao = get(),
            repo         = get(),
            context      = androidContext(),
            filterStore  = get(),
        )
    }
    viewModel {
        TacticsViewModel(
            gameStatsDao       = get(),
            motifTacticStatDao = get(),
            notablePositionDao = get(),
            repo               = get(),
            context            = androidContext(),
            filterStore        = get(),
        )
    }
    viewModel { SessionDebriefViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get()) }
    viewModel {
        GuessTheMoveViewModel(
            context       = androidContext(),
            importer      = get(),
            dao           = get(),
            progressDao   = get(),
            annotationDao = get(),
            engine        = get(),
            snippetRepo   = get(),
            settingsRepo  = get(),
        )
    }
    viewModel { (categoryNames: List<String>) ->
        WeaknessDrillViewModel(
            categoryNames     = categoryNames,
            criticalMomentDao = get(),
            repo              = get(),
            engine            = get(),
        )
    }
}
