package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.ui.components.OpeningExplorerUiState
import com.acepero13.chess.core.opening.LichessMastersExplorer
import com.acepero13.chess.core.opening.MoveFrequency
import com.acepero13.chess.core.ui.board.Arrow
import com.github.bhlangonijr.chesslib.Square
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val ARROW_COLORS = listOf(
    androidx.compose.ui.graphics.Color(0xCC2196F3.toInt()),
    androidx.compose.ui.graphics.Color(0xCC9C27B0.toInt()),
    androidx.compose.ui.graphics.Color(0xCC607D8B.toInt()),
)

internal class OpeningExplorerController(
    private val scope: CoroutineScope,
    private val onArrowsChanged: (List<Arrow>) -> Unit,
) {
    private val _state = MutableStateFlow(OpeningExplorerUiState())
    val state: StateFlow<OpeningExplorerUiState> = _state.asStateFlow()

    private var activeJob: Job? = null

    fun load(fen: String) {
        activeJob?.cancel()
        activeJob = scope.launch(Dispatchers.IO) {
            _state.update { it.copy(loading = true, error = null) }
            val result = runCatching { LichessMastersExplorer.query(fen) }.getOrNull()
            val arrows = result?.moves
                ?.take(3)
                ?.mapIndexedNotNull { i, m -> m.toArrow(i) }
                ?: emptyList()
            _state.update { it.copy(loading = false, result = result, arrows = arrows) }
            onArrowsChanged(arrows)
        }
    }

    fun clear() {
        activeJob?.cancel()
        _state.value = OpeningExplorerUiState(arrows = emptyList())
        onArrowsChanged(emptyList())
    }
}

private fun MoveFrequency.toArrow(rank: Int): Arrow? {
    if (uci.length < 4) return null
    return runCatching {
        Arrow(
            from  = Square.valueOf(uci.substring(0, 2).uppercase()),
            to    = Square.valueOf(uci.substring(2, 4).uppercase()),
            color = ARROW_COLORS.getOrElse(rank) { ARROW_COLORS.last() },
        )
    }.getOrNull()
}
