package com.acepero13.android.gamereviewer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.db.CriticalMomentDao
import com.acepero13.android.gamereviewer.data.db.EndgameEncounterDao
import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.data.repository.TriggerMasteryRepository
import com.acepero13.android.gamereviewer.domain.BehavioralDiagnostic
import com.acepero13.android.gamereviewer.domain.CoachingTrigger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Per-trigger mastery display row for the Habit Progress card. */
data class HabitMasteryRow(
    val label: String,
    val typeName: String,
    val streak: Int,
    val mastered: Boolean,
)

/** Cross-game summary of one endgame chapter the player has struggled with. */
data class EndgameWeaknessRow(
    val chapter: Int,
    val name: String,
    val category: String,
    val gamesEncountered: Int,
    val gamesWithMistake: Int,
)

/** The single coaching habit the user should focus on next. */
data class TopCoachTrigger(
    val label:    String,  // display label, e.g. "Forcing Move"
    val typeName: String,  // e.g. "FORCING_MOVE"
    val streak:   Int,
    val emoji:    String,
    val title:    String,
    val tier:     Int,
)

data class PhaseBreakdown(
    val opening:    Int,
    val middlegame: Int,
    val endgame:    Int,
) {
    val total: Int = opening + middlegame + endgame
    fun fraction(count: Int): Float = if (total == 0) 0f else count.toFloat() / total
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val totalGamesImported: Int = 0,
    val gamesAnalyzed: Int = 0,
    val totalCriticalMoments: Int = 0,
    val trends: List<BehavioralDiagnostic.FailureTrend> = emptyList(),
    val hasWishfulThinking: Boolean = false,
    val habitRows: List<HabitMasteryRow> = emptyList(),
    val endgameWeaknesses: List<EndgameWeaknessRow> = emptyList(),
    val phaseBreakdown: PhaseBreakdown? = null,
    val topCoachTrigger: TopCoachTrigger? = null,
    val error: String? = null,
)

/**
 * Backs the cross-game behavioural diagnosis dashboard (Task 4.3).
 *
 * Loads all [com.acepero13.android.gamereviewer.data.model.CriticalMoment] records,
 * runs [BehavioralDiagnostic.diagnose], and exposes the top 3 failure trends.
 */
// Classifies a moment into one of three phases.
// Explicit reason categories take priority; move index is the fallback.
private fun CriticalMoment.gamePhase(): String = when (toReason()) {
    CriticalMoment.ReasonCategory.OPENING_DEVIATION -> "opening"
    CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE -> "endgame"
    else -> when {
        moveIndex <= 15 -> "opening"
        moveIndex <= 40 -> "middlegame"
        else            -> "endgame"
    }
}

class DashboardViewModel(
    private val repo: GameRepository,
    private val criticalMomentDao: CriticalMomentDao,
    private val masteryRepo: TriggerMasteryRepository,
    private val endgameEncounterDao: EndgameEncounterDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { loadDashboard() }

    fun refresh() { loadDashboard() }

    private fun loadDashboard() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val totalGames    = repo.count()
                val gamesAnalyzed = criticalMomentDao.countGamesAnalyzed()
                val allMoments    = criticalMomentDao.getAll()
                val trends        = BehavioralDiagnostic.diagnose(allMoments, topN = 3)
                val wishful       = BehavioralDiagnostic.hasWishfulThinking(allMoments)

                val streaks   = masteryRepo.streaks.first()
                val habitRows = CoachingTrigger.ALL_LABELS.map { label ->
                    val typeName = masteryRepo.labelToTypeName(label)
                    val streak   = streaks[label] ?: 0
                    HabitMasteryRow(
                        label    = label,
                        typeName = typeName,
                        streak   = streak,
                        mastered = streak >= TriggerMasteryRepository.MASTERY_THRESHOLD,
                    )
                }

                // Lowest streak among non-mastered, with tier as tiebreaker (tier 1 = most critical).
                val topCoachTrigger = habitRows
                    .filter { !it.mastered }
                    .mapNotNull { row ->
                        val stub = CoachingTrigger.fromTypeName(row.typeName, 0)
                            ?: return@mapNotNull null
                        Triple(row, stub.tier(), stub)
                    }
                    .sortedWith(compareBy({ it.first.streak }, { it.second }))
                    .firstOrNull()
                    ?.let { (row, _, stub) ->
                        TopCoachTrigger(
                            label    = row.label,
                            typeName = row.typeName,
                            streak   = row.streak,
                            emoji    = stub.emoji(),
                            title    = stub.title(),
                            tier     = stub.tier(),
                        )
                    }

                val phaseBreakdown = PhaseBreakdown(
                    opening    = allMoments.count { it.gamePhase() == "opening" },
                    middlegame = allMoments.count { it.gamePhase() == "middlegame" },
                    endgame    = allMoments.count { it.gamePhase() == "endgame" },
                )

                val allEncounters = endgameEncounterDao.getAll()
                val endgameWeaknesses = allEncounters
                    .groupBy { it.chapter }
                    .map { (chapter, rows) ->
                        EndgameWeaknessRow(
                            chapter          = chapter,
                            name             = rows.first().name,
                            category         = rows.first().category,
                            gamesEncountered = rows.size,
                            gamesWithMistake = rows.count { it.hadMistake },
                        )
                    }
                    .filter { it.gamesWithMistake > 0 }
                    .sortedByDescending { it.gamesWithMistake }
                    .take(3)

                _uiState.update {
                    it.copy(
                        isLoading              = false,
                        totalGamesImported     = totalGames,
                        gamesAnalyzed          = gamesAnalyzed,
                        totalCriticalMoments   = allMoments.size,
                        trends                 = trends,
                        hasWishfulThinking     = wishful,
                        habitRows              = habitRows,
                        endgameWeaknesses      = endgameWeaknesses,
                        phaseBreakdown         = phaseBreakdown,
                        topCoachTrigger        = topCoachTrigger,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
