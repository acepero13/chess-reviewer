package com.acepero13.android.gamereviewer.di

import androidx.room.Room
import com.acepero13.android.gamereviewer.data.db.AppDatabase
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.domain.TruthMapBuilder
import com.acepero13.android.gamereviewer.ui.screens.AnalysisViewModel
import com.acepero13.android.gamereviewer.ui.screens.GameListViewModel
import com.acepero13.android.gamereviewer.ui.screens.HomeViewModel
import com.acepero13.android.gamereviewer.ui.screens.ImportViewModel
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
            // Development convenience: discard data on schema upgrades rather than
            // writing explicit migrations (switch to addMigrations() before release).
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<AppDatabase>().reviewGameDao() }
    single { get<AppDatabase>().annotationDao() }
    single { get<AppDatabase>().criticalMomentDao() }

    // ── Repositories ──────────────────────────────────────────────────────────
    single { GameRepository(get()) }

    // ── Chess-core singletons ─────────────────────────────────────────────────
    single { (androidApplication() as com.acepero13.android.gamereviewer.GameReviewerApp).stockfishEngine }
    single { PgnImporter() }
    single { OpeningClassifier(androidContext()) }

    // ── Domain layer ──────────────────────────────────────────────────────────
    single { TruthMapBuilder(get()) }

    // ── ViewModels ────────────────────────────────────────────────────────────
    viewModel { HomeViewModel(get()) }
    viewModel { GameListViewModel(get()) }
    viewModel { ImportViewModel(get(), get(), get(), get()) }
    viewModel { (gameId: Long) ->
        AnalysisViewModel(
            gameId         = gameId,
            repo           = get(),
            annotationDao  = get(),
            criticalMomentDao = get(),
            engine         = get(),
            opening        = get(),
            truthMapBuilder = get(),
        )
    }
}
