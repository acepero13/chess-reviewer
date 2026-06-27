package com.acepero13.android.gamereviewer.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.acepero13.android.gamereviewer.data.db.GameStatsDao
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.domain.AnalyticsFilterStore
import com.acepero13.android.gamereviewer.domain.AnalyticsGameFilter
import com.acepero13.android.gamereviewer.domain.GameStatsCalculator
import com.acepero13.android.gamereviewer.domain.PreparationAnalyzer
import com.acepero13.android.gamereviewer.domain.PreparationReport
import com.acepero13.android.gamereviewer.work.ShallowAnalysisWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PreparationUiState(
    val isLoading: Boolean = true,
    val gamesAnalyzed: Int = 0,
    val gamesPending: Int = 0,
    val report: PreparationReport? = null,
    val analysisInProgress: Boolean = false,
    val progressDone: Int = 0,
    val progressTotal: Int = 0,
)

/**
 * Backs the **Preparation** tab. Reads cached [com.acepero13.android.gamereviewer.data.model.GameStats]
 * + game results (no PGN re-analysis), runs [PreparationAnalyzer], and shares the
 * [ShallowAnalysisWorker] enqueue/observe pattern.
 */
class PreparationViewModel(
    private val gameStatsDao: GameStatsDao,
    private val repo: GameRepository,
    context: Context,
    private val filterStore: AnalyticsFilterStore,
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)

    private val _uiState = MutableStateFlow(PreparationUiState())
    val uiState: StateFlow<PreparationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { filterStore.filter.collect { load() } }
        observeWork()
    }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            val games = repo.getAll()
            val ids = AnalyticsGameFilter.eligibleIds(games, filterStore.filter.value)
            val stats = gameStatsDao.getAll().filter { it.gameId in ids }
            val resultByGame = games.associate { it.id to it.result }
            val pending = gameStatsDao.gameIdsNeedingStats(GameStatsCalculator.STATS_VERSION).size
            val report = PreparationAnalyzer.analyze(stats, resultByGame)
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

    fun analyzePendingGames() {
        val request = OneTimeWorkRequestBuilder<ShallowAnalysisWorker>().build()
        workManager.enqueueUniqueWork(ShallowAnalysisWorker.WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    private fun observeWork() {
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(ShallowAnalysisWorker.WORK_NAME).collect { infos ->
                val info = infos.firstOrNull()
                val running = info?.state == WorkInfo.State.RUNNING || info?.state == WorkInfo.State.ENQUEUED
                val done = info?.progress?.getInt(ShallowAnalysisWorker.KEY_DONE, 0) ?: 0
                val totl = info?.progress?.getInt(ShallowAnalysisWorker.KEY_TOTAL, 0) ?: 0
                val justFinished = _uiState.value.analysisInProgress && info?.state == WorkInfo.State.SUCCEEDED
                _uiState.update { it.copy(analysisInProgress = running, progressDone = done, progressTotal = totl) }
                if (justFinished) load()
            }
        }
    }
}
