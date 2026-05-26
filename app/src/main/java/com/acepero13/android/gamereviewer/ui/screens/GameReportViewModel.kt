package com.acepero13.android.gamereviewer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.db.GameEvaluationDao
import com.acepero13.android.gamereviewer.data.db.MoveTimeDao
import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.data.model.MoveTimeData
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.domain.TimeAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameReportUiState(
    val isLoading: Boolean = true,
    val gameTitle: String = "",
    val decisions: List<TimeAnalyzer.MoveDecision> = emptyList(),
    val evaluations: List<GameEvaluation> = emptyList(),
    val moveTimes: List<MoveTimeData> = emptyList(),

    // Aggregate stats
    val totalMoves: Int = 0,
    val blunderCount: Int = 0,
    val rushedBlunders: Int = 0,
    val carefulBlunders: Int = 0,
    val avgTimeOnBlunders: Float = 0f,
    val avgTimeOnGoodMoves: Float = 0f,
    val hasTimeData: Boolean = false,

    val error: String? = null,
)

/**
 * Backs the per-game report screen (Task 4.2).
 *
 * Loads [GameEvaluation] and [MoveTimeData] from Room, runs [TimeAnalyzer.analyze],
 * and exposes the result for the [GameReportScreen] to render as a Decision Velocity chart.
 */
class GameReportViewModel(
    private val gameId: Long,
    private val repo: GameRepository,
    private val evalDao: GameEvaluationDao,
    private val moveTimeDao: MoveTimeDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameReportUiState())
    val uiState: StateFlow<GameReportUiState> = _uiState.asStateFlow()

    init { loadReport() }

    private fun loadReport() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val game   = repo.findById(gameId)
                val evals  = evalDao.getByGameId(gameId)
                val times  = moveTimeDao.getByGameId(gameId)
                val decisions = TimeAnalyzer.analyze(evals, times)

                val title  = game?.let { "${it.whitePlayer} vs ${it.blackPlayer}" } ?: "Game #$gameId"
                val blunders = decisions.count { it.isBlunder }

                _uiState.update {
                    it.copy(
                        isLoading       = false,
                        gameTitle       = title,
                        decisions       = decisions,
                        evaluations     = evals,
                        moveTimes       = times,
                        totalMoves      = decisions.size,
                        blunderCount    = blunders,
                        rushedBlunders  = TimeAnalyzer.countRushedBlunders(decisions),
                        carefulBlunders = TimeAnalyzer.countCarefulBlunders(decisions),
                        avgTimeOnBlunders  = TimeAnalyzer.avgTimeOnBlunders(decisions),
                        avgTimeOnGoodMoves = TimeAnalyzer.avgTimeOnGoodMoves(decisions),
                        hasTimeData     = times.isNotEmpty(),
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
