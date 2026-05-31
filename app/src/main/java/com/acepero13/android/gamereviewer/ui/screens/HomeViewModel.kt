package com.acepero13.android.gamereviewer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.domain.SessionDebrief
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(repo: GameRepository) : ViewModel() {

    val gameCount = repo.countAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val hasRecentSession = repo.countRecentGames(SessionDebrief.sessionCutoff())
        .map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}
