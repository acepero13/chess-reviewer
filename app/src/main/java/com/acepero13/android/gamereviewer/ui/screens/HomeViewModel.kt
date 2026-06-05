package com.acepero13.android.gamereviewer.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.domain.SessionDebrief
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

data class MasterGamePreview(
    val index: Int,
    val white: String,
    val black: String,
    val event: String,
    val year: String,
    val eco: String,
)

class HomeViewModel(repo: GameRepository, private val context: Context) : ViewModel() {

    val gameCount = repo.countAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val hasRecentSession = repo.countRecentGames(SessionDebrief.sessionCutoff())
        .map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val recentGames: kotlinx.coroutines.flow.StateFlow<List<ReviewGame>> = repo.observeAll()
        .map { it.take(6) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val masterGamePreviews = flow {
        val previews = withContext(Dispatchers.IO) { loadMasterGamePreviews() }
        emit(previews)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun loadMasterGamePreviews(): List<MasterGamePreview> = runCatching {
        val pgn = context.assets.open("master_games.pgn").bufferedReader().readText()
        val headerRegex = Regex("""\[(\w+)\s+"([^"]*)"]""")
        pgn.split(Regex("(?=\\[Event )"))
            .filter { it.trimStart().startsWith("[") }
            .mapIndexed { idx, block ->
                val h = headerRegex.findAll(block).associate { it.groupValues[1] to it.groupValues[2] }
                MasterGamePreview(
                    index = idx,
                    white = h["White"] ?: "?",
                    black = h["Black"] ?: "?",
                    event = h["Event"] ?: "",
                    year  = h["Date"]?.substringBefore(".") ?: "",
                    eco   = h["ECO"] ?: "",
                )
            }
    }.getOrDefault(emptyList())
}
