package com.acepero13.android.gamereviewer

import android.app.Application
import com.acepero13.android.gamereviewer.di.appModule
import com.acepero13.chess.core.engine.StockfishEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class GameReviewerApp : Application() {

    /** Application-scoped Stockfish instance — shared across all screens. */
    lateinit var stockfishEngine: StockfishEngine
        private set

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@GameReviewerApp)
            modules(appModule)
        }

        stockfishEngine = StockfishEngine(this)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { stockfishEngine.start() }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        stockfishEngine.quit()
    }
}
