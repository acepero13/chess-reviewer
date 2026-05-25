package com.acepero13.android.gamereviewer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(repo: GameRepository) : ViewModel() {

    /** Total number of imported games — shown on the home screen CTA card. */
    val gameCount = repo.countAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
