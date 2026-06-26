package com.acepero13.android.gamereviewer.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.acepero13.android.gamereviewer.data.db.GameStatsDao
import com.acepero13.android.gamereviewer.data.db.MotifTacticStatDao
import com.acepero13.android.gamereviewer.data.db.NotablePositionDao
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.domain.GameStatsCalculator
import com.acepero13.android.gamereviewer.domain.TacticsAnalyzer
import com.acepero13.android.gamereviewer.domain.TacticsReport
import com.acepero13.android.gamereviewer.work.ShallowAnalysisWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TacticsUiState(
    val isLoading: Boolean = true,
    val gamesAnalyzed: Int = 0,
    val gamesPending: Int = 0,
    val report: TacticsReport? = null,
    val analysisInProgress: Boolean = false,
    val progressDone: Int = 0,
    val progressTotal: Int = 0,
)

/**
 * Backs the **Tactics** tab. Reads cached [com.acepero13.android.gamereviewer.data.model.MotifTacticStat]
 * + [com.acepero13.android.gamereviewer.data.model.NotablePosition] (no PGN re-analysis), runs
 * [TacticsAnalyzer], and shares the [ShallowAnalysisWorker] enqueue/observe pattern.
 */
class TacticsViewModel(
    private val gameStatsDao: GameStatsDao,
    private val motifTacticStatDao: MotifTacticStatDao,
    private val notablePositionDao: NotablePositionDao,
    private val repo: GameRepository,
    context: Context,
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)

    private val _uiState = MutableStateFlow(TacticsUiState())
    val uiState: StateFlow<TacticsUiState> = _uiState.asStateFlow()

    init {
        load()
        observeWork()
    }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            val motifStats = motifTacticStatDao.getAll()
            val notable = notablePositionDao.getAll()
            val total = repo.count()
            val pending = gameStatsDao.gameIdsNeedingStats(GameStatsCalculator.STATS_VERSION).size
            val report = TacticsAnalyzer.analyze(motifStats, notable)
            _uiState.update {
                it.copy(
                    isLoading     = false,
                    gamesAnalyzed = (total - pending).coerceAtLeast(0),
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
