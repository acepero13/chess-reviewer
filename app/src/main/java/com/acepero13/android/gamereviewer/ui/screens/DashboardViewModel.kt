package com.acepero13.android.gamereviewer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.db.CriticalMomentDao
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.domain.BehavioralDiagnostic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val totalGamesImported: Int = 0,
    val gamesAnalyzed: Int = 0,
    val totalCriticalMoments: Int = 0,
    val trends: List<BehavioralDiagnostic.FailureTrend> = emptyList(),
    val hasWishfulThinking: Boolean = false,
    val error: String? = null,
)

/**
 * Backs the cross-game behavioural diagnosis dashboard (Task 4.3).
 *
 * Loads all [com.acepero13.android.gamereviewer.data.model.CriticalMoment] records,
 * runs [BehavioralDiagnostic.diagnose], and exposes the top 3 failure trends.
 */
class DashboardViewModel(
    private val repo: GameRepository,
    private val criticalMomentDao: CriticalMomentDao,
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

                _uiState.update {
                    it.copy(
                        isLoading              = false,
                        totalGamesImported     = totalGames,
                        gamesAnalyzed          = gamesAnalyzed,
                        totalCriticalMoments   = allMoments.size,
                        trends                 = trends,
                        hasWishfulThinking     = wishful,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
