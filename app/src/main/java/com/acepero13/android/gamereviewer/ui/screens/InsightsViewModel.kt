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
import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.domain.PlayerProfile
import com.acepero13.android.gamereviewer.domain.PlayerProfileBuilder
import com.acepero13.android.gamereviewer.work.ShallowAnalysisWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InsightsUiState(
    val isLoading: Boolean = true,
    val totalGames: Int = 0,
    val gamesAnalyzed: Int = 0,
    val gamesPending: Int = 0,
    val avgAccuracy: Float = 0f,
    val avgAcpl: Int = 0,
    val totalBlunders: Int = 0,
    val totalMistakes: Int = 0,
    val totalInaccuracies: Int = 0,
    val openingAccuracy: Float = 0f,
    val middlegameAccuracy: Float = 0f,
    val endgameAccuracy: Float = 0f,
    val attackAccuracy: Float = 0f,
    val defenseAccuracy: Float = 0f,
    val profile: PlayerProfile? = null,
    val phaseBreakdown: PhaseBreakdown? = null,
    val analysisInProgress: Boolean = false,
    val progressDone: Int = 0,
    val progressTotal: Int = 0,
)

/** Distribution of critical moments across the three game phases. */
data class PhaseBreakdown(
    val opening:    Int,
    val middlegame: Int,
    val endgame:    Int,
) {
    val total: Int = opening + middlegame + endgame
    fun fraction(count: Int): Float = if (total == 0) 0f else count.toFloat() / total
}

/** Bucket a critical moment into a game phase by reason category, falling back to move index. */
private fun CriticalMoment.gamePhase(): String = when (toReason()) {
    CriticalMoment.ReasonCategory.OPENING_DEVIATION -> "opening"
    CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE -> "endgame"
    else -> when {
        moveIndex <= 15 -> "opening"
        moveIndex <= 40 -> "middlegame"
        else            -> "endgame"
    }
}

/**
 * Backs the Insights tab: reads cached [com.acepero13.android.gamereviewer.data.model.GameStats]
 * (no PGN re-analysis), aggregates them, builds the [PlayerProfile], and enqueues / observes the
 * [ShallowAnalysisWorker] batch pass.
 */
class InsightsViewModel(
    private val gameStatsDao: GameStatsDao,
    private val criticalMomentDao: CriticalMomentDao,
    private val repo: GameRepository,
    context: Context,
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        load()
        observeWork()
    }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            val stats = gameStatsDao.getAll()           // newest first
            val total = repo.count()
            val moments = criticalMomentDao.getAll()
            val profile = PlayerProfileBuilder.build(stats, moments)
            val phaseBreakdown = PhaseBreakdown(
                opening    = moments.count { it.gamePhase() == "opening" },
                middlegame = moments.count { it.gamePhase() == "middlegame" },
                endgame    = moments.count { it.gamePhase() == "endgame" },
            )

            fun phaseAvg(selector: (com.acepero13.android.gamereviewer.data.model.GameStats) -> Float): Float =
                stats.map(selector).filter { it > 0f }.let { if (it.isEmpty()) 0f else it.average().toFloat() }

            _uiState.update {
                it.copy(
                    isLoading          = false,
                    totalGames         = total,
                    gamesAnalyzed      = stats.size,
                    gamesPending       = (total - stats.size).coerceAtLeast(0),
                    avgAccuracy        = if (stats.isEmpty()) 0f else stats.map { s -> s.accuracy }.average().toFloat(),
                    avgAcpl            = if (stats.isEmpty()) 0 else stats.map { s -> s.acpl }.average().toInt(),
                    totalBlunders      = stats.sumOf { s -> s.blunders },
                    totalMistakes      = stats.sumOf { s -> s.mistakes },
                    totalInaccuracies  = stats.sumOf { s -> s.inaccuracies },
                    openingAccuracy    = phaseAvg { s -> s.openingAccuracy },
                    middlegameAccuracy = phaseAvg { s -> s.middlegameAccuracy },
                    endgameAccuracy    = phaseAvg { s -> s.endgameAccuracy },
                    attackAccuracy     = phaseAvg { s -> s.middlegameAttackAccuracy },
                    defenseAccuracy    = phaseAvg { s -> s.middlegameDefenseAccuracy },
                    profile            = profile,
                    phaseBreakdown     = phaseBreakdown,
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
