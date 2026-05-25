package com.acepero13.android.gamereviewer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameListViewModel(private val repo: GameRepository) : ViewModel() {

    val games = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteGame(game: ReviewGame) {
        viewModelScope.launch { repo.deleteById(game.id) }
    }
}
