package com.acepero13.android.gamereviewer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.db.CriticalMomentDao
import com.acepero13.android.gamereviewer.data.db.EndgameEncounterDao
import com.acepero13.android.gamereviewer.data.db.MoveTimeDao
import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.data.model.MoveTimeData
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.data.repository.SettingsRepository
import com.acepero13.android.gamereviewer.data.repository.TriggerMasteryRepository
import com.acepero13.android.gamereviewer.domain.BehavioralDiagnostic
import com.acepero13.android.gamereviewer.domain.TrainingPlanRecommender
import com.acepero13.android.gamereviewer.domain.TrainingRecommendation
import com.acepero13.android.gamereviewer.domain.ColorAsymmetryAnalyzer
import com.acepero13.android.gamereviewer.domain.ColorAsymmetry
import com.acepero13.android.gamereviewer.domain.CoachingTrigger
import com.acepero13.android.gamereviewer.domain.ImprovementTrajectory
import com.acepero13.android.gamereviewer.domain.ImprovementTrajectoryAnalyzer
import com.acepero13.android.gamereviewer.domain.PhaseFailureHeatmap
import com.acepero13.android.gamereviewer.domain.PhaseFailureRow
import com.acepero13.android.gamereviewer.domain.SelfAwarenessTrend
import com.acepero13.android.gamereviewer.domain.SelfAwarenessTrendPoint
import com.acepero13.android.gamereviewer.domain.VelocityConsistency
import com.acepero13.android.gamereviewer.domain.VelocityConsistencyAnalyzer
import com.acepero13.android.gamereviewer.ui.components.EcoDeviationRow
import com.acepero13.chess.core.opening.OpeningClassifier
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
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
    val label:    String,
    val typeName: String,
    val streak:   Int,
    val emoji:    String,
    val title:    String,
    val tier:     Int,
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val totalGamesImported: Int = 0,
    val gamesAnalyzed: Int = 0,
    val totalCriticalMoments: Int = 0,
    val trends: List<BehavioralDiagnostic.FailureTrend> = emptyList(),
    val hasWishfulThinking: Boolean = false,
    val habitRows: List<HabitMasteryRow> = emptyList(),
    val endgameWeaknesses: List<EndgameWeaknessRow> = emptyList(),
    val topCoachTrigger: TopCoachTrigger? = null,

    // ── New insight fields ──────────────────────────────────────────────────────
    val selfAwarenessTrend: List<SelfAwarenessTrendPoint> = emptyList(),
    val colorAsymmetry: ColorAsymmetry? = null,
    val phaseFailureHeatmap: List<PhaseFailureRow> = emptyList(),
    val velocityConsistency: VelocityConsistency? = null,
    val improvementTrajectory: ImprovementTrajectory? = null,
    val openingDeviationRows: List<EcoDeviationRow> = emptyList(),
    val trainingRecommendations: List<TrainingRecommendation> = emptyList(),

    val error: String? = null,
)

/**
 * Backs the cross-game behavioural diagnosis dashboard (Task 4.3).
 *
 * Loads all [CriticalMoment] records, runs [BehavioralDiagnostic.diagnose],
 * and exposes the top 3 failure trends plus all new cross-game insights.
 */

class DashboardViewModel(
    private val repo: GameRepository,
    private val criticalMomentDao: CriticalMomentDao,
    private val masteryRepo: TriggerMasteryRepository,
    private val endgameEncounterDao: EndgameEncounterDao,
    private val settingsRepo: SettingsRepository,
    private val moveTimeDao: MoveTimeDao,
    private val openingClassifier: OpeningClassifier,
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
                val allGames      = repo.getAll()
                val username      = settingsRepo.username.first().trim()

                val trends  = BehavioralDiagnostic.diagnose(allMoments, topN = 3)
                val wishful = BehavioralDiagnostic.hasWishfulThinking(allMoments)
                val trainingPlan = TrainingPlanRecommender.recommend(trends)

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

                // ── New insight computations ────────────────────────────────────

                val selfAwarenessTrend = SelfAwarenessTrend.compute(allMoments, allGames)

                val colorAsymmetry = ColorAsymmetryAnalyzer.compute(allMoments, allGames, username)
                    .takeIf { it.hasData }

                val phaseHeatmap = PhaseFailureHeatmap.compute(allMoments)

                val allMoveTimes = moveTimeDao.getAll()
                val timesByGame  = allMoveTimes.groupBy { it.gameId }
                val velocityConsistency = VelocityConsistencyAnalyzer.compute(timesByGame)

                val improvementTrajectory = trends.firstOrNull()?.let { topTrend ->
                    ImprovementTrajectoryAnalyzer.compute(allMoments, allGames, topTrend)
                }

                val deviationRows = computeOpeningDeviationRows(allGames
                    .filter { game ->
                        allMoments.any { it.gameId == game.id &&
                            it.type == com.acepero13.android.gamereviewer.data.model.CriticalMoment.Type.ENGINE_MARKED.name }
                    }
                    .take(30) // cap to avoid slowness on large libraries
                )

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
                        topCoachTrigger        = topCoachTrigger,
                        selfAwarenessTrend     = selfAwarenessTrend,
                        colorAsymmetry         = colorAsymmetry,
                        phaseFailureHeatmap    = phaseHeatmap,
                        velocityConsistency    = velocityConsistency,
                        improvementTrajectory  = improvementTrajectory,
                        openingDeviationRows     = deviationRows,
                        trainingRecommendations  = trainingPlan,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Walks each game's UCI moves through the opening classifier to find the first
     * position not in the ECO database (= the deviation move). Groups by ECO code
     * and returns the average deviation move for the most-played openings.
     *
     * Uses only the stored openingEco; games without a classification are skipped.
     */
    private fun computeOpeningDeviationRows(
        games: List<com.acepero13.android.gamereviewer.data.model.ReviewGame>,
    ): List<EcoDeviationRow> {
        data class GameDeviation(val eco: String, val name: String, val deviationMove: Int)

        val deviations = games.mapNotNull { game ->
            if (game.openingEco.isBlank()) return@mapNotNull null
            val uciMoves = game.movesUci.split(" ").filter { it.isNotBlank() }
            if (uciMoves.isEmpty()) return@mapNotNull null
            val devMove = findDeviationMove(uciMoves)
            GameDeviation(game.openingEco, game.openingName, devMove)
        }

        return deviations
            .groupBy { it.eco }
            .map { (eco, list) ->
                EcoDeviationRow(
                    eco              = eco,
                    openingName      = list.first().name.take(28),
                    avgDeviationMove = list.map { it.deviationMove }.average().toFloat(),
                    gameCount        = list.size,
                    maxDeviationMove = 20,
                )
            }
            .filter { it.gameCount >= 2 }
            .sortedByDescending { it.gameCount }
            .take(5)
    }

    private fun findDeviationMove(uciMoves: List<String>): Int {
        return runCatching {
            val board = Board()
            for ((index, uci) in uciMoves.withIndex()) {
                if (uci.length < 4) break
                val from = Square.valueOf(uci.substring(0, 2).uppercase())
                val to   = Square.valueOf(uci.substring(2, 4).uppercase())
                val move = Move(from, to)
                board.doMove(move)
                if (openingClassifier.classify(board.fen) == null) return index + 1
            }
            uciMoves.size
        }.getOrElse { uciMoves.size / 2 }
    }
}
