package com.acepero13.android.gamereviewer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Null means "all" for result / source filters. */
data class GameFilter(
    val query: String = "",
    val result: String? = null,   // "1-0" | "0-1" | "1/2-1/2" | null
    val source: String? = null,   // "chesscom" | "lichess" | "file" | null
)

class GameListViewModel(private val repo: GameRepository) : ViewModel() {

    val filter = MutableStateFlow(GameFilter())

    val games = combine(repo.observeAll(), filter) { all, f ->
        all.filter { game -> game.matchesFilter(f) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteGame(game: ReviewGame) {
        viewModelScope.launch { repo.deleteById(game.id) }
    }

    fun setQuery(q: String)   = filter.value.let { filter.value = it.copy(query = q) }
    fun setResult(r: String?) = filter.value.let { filter.value = it.copy(result = r) }
    fun setSource(s: String?) = filter.value.let { filter.value = it.copy(source = s) }
    fun clearFilters()        { filter.value = GameFilter() }
}

private fun ReviewGame.matchesFilter(f: GameFilter): Boolean {
    if (f.result != null && result != f.result) return false
    if (f.source != null && sourceType != f.source) return false
    if (f.query.isNotBlank()) {
        val q = f.query.trim().lowercase()
        val hit = whitePlayer.lowercase().contains(q)
            || blackPlayer.lowercase().contains(q)
            || openingName.lowercase().contains(q)
            || event.lowercase().contains(q)
        if (!hit) return false
    }
    return true
}
