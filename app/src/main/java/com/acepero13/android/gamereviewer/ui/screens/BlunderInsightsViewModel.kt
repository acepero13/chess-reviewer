package com.acepero13.android.gamereviewer.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.acepero13.android.gamereviewer.data.db.CriticalMomentDao
import com.acepero13.android.gamereviewer.data.db.GameStatsDao
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.domain.AnalyticsFilterStore
import com.acepero13.android.gamereviewer.domain.AnalyticsGameFilter
import com.acepero13.android.gamereviewer.domain.BlunderAnalyzer
import com.acepero13.android.gamereviewer.domain.GameStatsCalculator
import com.acepero13.android.gamereviewer.domain.BlunderReport
import com.acepero13.android.gamereviewer.work.ShallowAnalysisWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BlunderUiState(
    val isLoading: Boolean = true,
    val gamesAnalyzed: Int = 0,
    val gamesPending: Int = 0,
    val report: BlunderReport? = null,
    val analysisInProgress: Boolean = false,
    val progressDone: Int = 0,
    val progressTotal: Int = 0,
)

/**
 * Backs the **Blunders** tab. Reads the cached
 * [com.acepero13.android.gamereviewer.data.model.GameStats] +
 * [com.acepero13.android.gamereviewer.data.model.CriticalMoment] tables (no PGN re-analysis),
 * runs [BlunderAnalyzer], and shares the [ShallowAnalysisWorker] enqueue/observe pattern with
 * [InsightsViewModel] so the tab refreshes when the batch pass finishes.
 */
class BlunderInsightsViewModel(
    private val gameStatsDao: GameStatsDao,
    private val criticalMomentDao: CriticalMomentDao,
    private val repo: GameRepository,
    context: Context,
    private val filterStore: AnalyticsFilterStore,
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)

    private val _uiState = MutableStateFlow(BlunderUiState())
    val uiState: StateFlow<BlunderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { filterStore.filter.collect { load() } }
        observeWork()
    }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            val allGames = repo.getAll()
            val ids = AnalyticsGameFilter.eligibleIds(allGames, filterStore.filter.value)
            val stats = gameStatsDao.getAll().filter { it.gameId in ids }
            val moments = criticalMomentDao.getAll().filter { it.gameId in ids }
            val games = allGames.filter { it.id in ids }
            val pending = gameStatsDao.gameIdsNeedingStats(GameStatsCalculator.STATS_VERSION).size
            val report = BlunderAnalyzer.analyze(stats, moments, games)

            _uiState.update {
                it.copy(
                    isLoading     = false,
                    gamesAnalyzed = stats.size,
                    gamesPending  = pending,
                    report        = report,
                )
            }
        }
    }

    /** Enqueue the batch shallow-analysis worker for all un-analyzed games. */
    fun analyzePendingGames() {
        val request = OneTimeWorkRequestBuilder<ShallowAnalysisWorker>().build()
        workManager.enqueueUniqueWork(
            ShallowAnalysisWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private fun observeWork() {
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(ShallowAnalysisWorker.WORK_NAME)
                .collect { infos ->
                    val info = infos.firstOrNull()
                    val running = info?.state == WorkInfo.State.RUNNING ||
                        info?.state == WorkInfo.State.ENQUEUED
                    val done = info?.progress?.getInt(ShallowAnalysisWorker.KEY_DONE, 0) ?: 0
                    val totl = info?.progress?.getInt(ShallowAnalysisWorker.KEY_TOTAL, 0) ?: 0
                    val justFinished = _uiState.value.analysisInProgress &&
                        info?.state == WorkInfo.State.SUCCEEDED
                    _uiState.update {
                        it.copy(analysisInProgress = running, progressDone = done, progressTotal = totl)
                    }
                    if (justFinished) load()
                }
        }
    }
}
