package com.acepero13.android.gamereviewer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.db.CriticalMomentDao
import com.acepero13.android.gamereviewer.data.db.GameEvaluationDao
import com.acepero13.android.gamereviewer.data.db.MoveTimeDao
import com.acepero13.android.gamereviewer.data.db.ReviewGameDao
import com.acepero13.android.gamereviewer.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: SettingsRepository,
    private val reviewGameDao: ReviewGameDao,
    private val criticalMomentDao: CriticalMomentDao,
    private val gameEvaluationDao: GameEvaluationDao,
    private val moveTimeDao: MoveTimeDao,
) : ViewModel() {

    val boardTheme = repo.boardTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Classic")

    val pieceStyle = repo.pieceStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Classic")

    val themeMode = repo.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "dark")

    /** Player's username for board auto-orientation. */
    val username = repo.username
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** When true, structured analysis prompts appear at flagged positions in Navigate mode. */
    val positionCoachEnabled = repo.positionCoachEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** When true, a "Copy LLM Prompt" button appears next to active coaching panels. */
    val developerModeEnabled = repo.developerModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Lichess personal access token for the opening explorer. */
    val lichessApiToken = repo.lichessApiToken
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** Tracks whether the clear-all operation has just completed (for snackbar feedback). */
    private val _clearDone = MutableStateFlow(false)
    val clearDone = _clearDone.asStateFlow()

    fun setBoardTheme(theme: String)           = viewModelScope.launch { repo.setBoardTheme(theme) }
    fun setPieceStyle(style: String)           = viewModelScope.launch { repo.setPieceStyle(style) }
    fun setThemeMode(mode: String)             = viewModelScope.launch { repo.setThemeMode(mode) }
    fun setUsername(name: String)              = viewModelScope.launch { repo.setUsername(name) }
    fun setPositionCoachEnabled(on: Boolean)   = viewModelScope.launch { repo.setPositionCoachEnabled(on) }
    fun setDeveloperModeEnabled(on: Boolean)   = viewModelScope.launch { repo.setDeveloperModeEnabled(on) }
    fun setLichessApiToken(token: String)      = viewModelScope.launch { repo.setLichessApiToken(token) }

    /**
     * Deletes all imported games and their associated analysis data:
     * - review_games
     * - critical_moments
     * - game_evaluations
     * - move_times
     *
     * Position annotations (FEN-keyed) are intentionally kept — they would be
     * re-attached automatically if the same games are re-imported.
     */
    fun clearAllGames() {
        viewModelScope.launch(Dispatchers.IO) {
            reviewGameDao.deleteAll()
            criticalMomentDao.deleteAll()
            gameEvaluationDao.deleteAll()
            moveTimeDao.deleteAll()
            _clearDone.value = true
        }
    }

    fun onClearDoneConsumed() {
        _clearDone.value = false
    }
}
