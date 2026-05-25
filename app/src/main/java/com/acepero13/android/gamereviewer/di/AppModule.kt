package com.acepero13.android.gamereviewer.di

import androidx.room.Room
import com.acepero13.android.gamereviewer.data.db.AppDatabase
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.ui.screens.AnalysisViewModel
import com.acepero13.android.gamereviewer.ui.screens.GameListViewModel
import com.acepero13.android.gamereviewer.ui.screens.HomeViewModel
import com.acepero13.android.gamereviewer.ui.screens.ImportViewModel
import com.acepero13.chess.core.engine.StockfishEngine
import com.acepero13.chess.core.opening.OpeningClassifier
import com.acepero13.chess.core.pgn.ChessComFetcher
import com.acepero13.chess.core.pgn.LichessFetcher
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
        ).build()
    }
    single { get<AppDatabase>().reviewGameDao() }
    single { get<AppDatabase>().annotationDao() }

    // ── Repositories ──────────────────────────────────────────────────────────
    single { GameRepository(get()) }

    // ── Chess-core singletons ─────────────────────────────────────────────────
    single { (androidApplication() as com.acepero13.android.gamereviewer.GameReviewerApp).stockfishEngine }
    single { PgnImporter() }
    single { OpeningClassifier(androidContext()) }

    // ── ViewModels ────────────────────────────────────────────────────────────
    viewModel { HomeViewModel(get()) }
    viewModel { GameListViewModel(get()) }
    viewModel { ImportViewModel(get(), get(), get(), get()) }
    viewModel { (gameId: Long) -> AnalysisViewModel(gameId, get(), get(), get()) }
}
