package com.acepero13.android.gamereviewer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.db.CriticalMomentDao
import com.acepero13.android.gamereviewer.data.db.GameEvaluationDao
import com.acepero13.android.gamereviewer.data.db.MoveTimeDao
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.domain.SessionDebrief
import com.acepero13.android.gamereviewer.domain.TimeAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionDebriefUiState(
    val isLoading: Boolean = true,
    val summary: SessionDebrief.Summary? = null,
    val error: String? = null,
)

class SessionDebriefViewModel(
    private val repo: GameRepository,
    private val criticalMomentDao: CriticalMomentDao,
    private val evalDao: GameEvaluationDao,
    private val moveTimeDao: MoveTimeDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionDebriefUiState())
    val uiState: StateFlow<SessionDebriefUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cutoff       = SessionDebrief.sessionCutoff()
                val recentGames  = repo.getRecentGames(cutoff)
                val gameIds      = recentGames.map { it.id }

                val allDecisions = gameIds.flatMap { id ->
                    val evals = evalDao.getByGameId(id)
                    val times = moveTimeDao.getByGameId(id)
                    TimeAnalyzer.analyze(evals, times)
                }

                val sessionMoments = gameIds.flatMap { id ->
                    criticalMomentDao.getByGameId(id)
                }

                val summary = SessionDebrief.summarize(
                    gameCount      = recentGames.size,
                    allDecisions   = allDecisions,
                    sessionMoments = sessionMoments,
                )

                _uiState.update { it.copy(isLoading = false, summary = summary) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
