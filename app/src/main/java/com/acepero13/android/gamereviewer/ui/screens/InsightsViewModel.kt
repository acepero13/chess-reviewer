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
import com.acepero13.android.gamereviewer.domain.AnalyticsFilterStore
import com.acepero13.android.gamereviewer.domain.AnalyticsGameFilter
import com.acepero13.android.gamereviewer.domain.GameStatsCalculator
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
    // Move time distribution (avg seconds; 0 = no clock data).
    val avgTimeWinningSec: Float = 0f,
    val avgTimeLosingSec: Float = 0f,
    // Engine correlation in sharp vs quiet positions (0–100% best-move rate).
    val sharpCorrelation: Float = 0f,
    val quietCorrelation: Float = 0f,
    // Oversight recovery (0–100% of moves right after an inaccuracy that were best).
    val recoveryRate: Float = 0f,
    val oversightCount: Int = 0,
    val motifWeaknesses: List<MotifWeakness> = emptyList(),
    val pawnStructures: List<StructureAccuracy> = emptyList(),
    val profile: PlayerProfile? = null,
    val phaseBreakdown: PhaseBreakdown? = null,
    val analysisInProgress: Boolean = false,
    val progressDone: Int = 0,
    val progressTotal: Int = 0,
)

/** A recurring weakness type and how often it has cost the user, for the motif-frequency card. */
data class MotifWeakness(val label: String, val count: Int)

/** Average accuracy in games of a given pawn structure. */
data class StructureAccuracy(val label: String, val accuracy: Float, val games: Int)

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

/** Human-readable label for a critical-moment reason category, for the weakness breakdown. */
private fun CriticalMoment.ReasonCategory.label(): String = when (this) {
    CriticalMoment.ReasonCategory.MISSED_TACTIC     -> "Missed tactics"
    CriticalMoment.ReasonCategory.OPENING_DEVIATION -> "Opening deviations"
    CriticalMoment.ReasonCategory.HANGING_PIECE     -> "Hanging pieces"
    CriticalMoment.ReasonCategory.KING_SAFETY       -> "King safety"
    CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE -> "Endgame technique"
    CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE -> "Strategic errors"
    CriticalMoment.ReasonCategory.TIME_PRESSURE     -> "Time pressure"
    CriticalMoment.ReasonCategory.MISSED_WIN        -> "Missed wins"
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
    private val filterStore: AnalyticsFilterStore,
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { filterStore.filter.collect { load() } }
        observeWork()
    }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            val ids = AnalyticsGameFilter.eligibleIds(repo.getAll(), filterStore.filter.value)
            val stats = gameStatsDao.getAll().filter { it.gameId in ids }   // newest first
            val total = repo.count()
            val pending = gameStatsDao.gameIdsNeedingStats(GameStatsCalculator.STATS_VERSION).size
            val moments = criticalMomentDao.getAll().filter { it.gameId in ids }
            val profile = PlayerProfileBuilder.build(stats, moments)
            val phaseBreakdown = PhaseBreakdown(
                opening    = moments.count { it.gamePhase() == "opening" },
                middlegame = moments.count { it.gamePhase() == "middlegame" },
                endgame    = moments.count { it.gamePhase() == "endgame" },
            )

            fun phaseAvg(selector: (com.acepero13.android.gamereviewer.data.model.GameStats) -> Float): Float =
                stats.map(selector).filter { it > 0f }.let { if (it.isEmpty()) 0f else it.average().toFloat() }

            // ── New Insights metrics ────────────────────────────────────────────
            // Engine correlation (weighted across games by move counts).
            val sharpTotal = stats.sumOf { it.sharpMoveCount }
            val quietTotal = stats.sumOf { it.quietMoveCount }
            val sharpCorrelation = if (sharpTotal == 0) 0f else 100f * stats.sumOf { it.sharpBestMoves } / sharpTotal
            val quietCorrelation = if (quietTotal == 0) 0f else 100f * stats.sumOf { it.quietBestMoves } / quietTotal

            // Oversight recovery (weighted by number of oversights).
            val oversightTotal = stats.sumOf { it.oversightCount }
            val recoveryRate = if (oversightTotal == 0) 0f else 100f * stats.sumOf { it.oversightRecovered } / oversightTotal

            // Recurring weaknesses: critical-moment categories merged with engine-motif blunders.
            val weaknessCounts = linkedMapOf<String, Int>()
            moments.groupingBy { it.toReason().label() }.eachCount()
                .forEach { (label, n) -> weaknessCounts.merge(label, n, Int::plus) }
            stats.sumOf { it.forkBlunders }.takeIf { it > 0 }?.let { weaknessCounts.merge("Forks", it, Int::plus) }
            stats.sumOf { it.hangingBlunders }.takeIf { it > 0 }
                ?.let { weaknessCounts.merge("Hanging pieces", it, Int::plus) }
            val motifWeaknesses = weaknessCounts.entries
                .sortedByDescending { it.value }.take(6)
                .map { MotifWeakness(it.key, it.value) }

            // Pawn structure accuracy.
            val pawnStructures = stats
                .filter { it.pawnStructure.isNotBlank() }
                .groupBy { it.pawnStructure }
                .map { (label, gs) -> StructureAccuracy(label, gs.map { g -> g.accuracy }.average().toFloat(), gs.size) }
                .sortedByDescending { it.games }

            _uiState.update {
                it.copy(
                    isLoading          = false,
                    totalGames         = total,
                    gamesAnalyzed      = stats.size,
                    gamesPending       = pending,
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
                    avgTimeWinningSec  = phaseAvg { s -> s.avgTimeWinningSec },
                    avgTimeLosingSec   = phaseAvg { s -> s.avgTimeLosingSec },
                    sharpCorrelation   = sharpCorrelation,
                    quietCorrelation   = quietCorrelation,
                    recoveryRate       = recoveryRate,
                    oversightCount     = oversightTotal,
                    motifWeaknesses    = motifWeaknesses,
                    pawnStructures     = pawnStructures,
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
