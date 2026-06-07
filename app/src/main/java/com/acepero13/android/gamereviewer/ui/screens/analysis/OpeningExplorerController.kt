package com.acepero13.android.gamereviewer.ui.screens.analysis

import android.util.Log
import com.acepero13.android.gamereviewer.ui.components.OpeningExplorerUiState
import com.acepero13.chess.core.opening.ChessDbExplorer
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

private const val TAG = "OpeningExplorerCtrl"

private val ARROW_COLORS = listOf(
    androidx.compose.ui.graphics.Color(0xCC2196F3.toInt()),
    androidx.compose.ui.graphics.Color(0xCC9C27B0.toInt()),
    androidx.compose.ui.graphics.Color(0xCC607D8B.toInt()),
)

internal class OpeningExplorerController(
    private val scope: CoroutineScope,
    private val onArrowsChanged: (List<Arrow>) -> Unit,
    private val lichessToken: () -> String? = { null },
) {
    private val _state = MutableStateFlow(OpeningExplorerUiState())
    val state: StateFlow<OpeningExplorerUiState> = _state.asStateFlow()

    private var activeJob: Job? = null

    fun load(fen: String) {
        activeJob?.cancel()
        Log.d(TAG, "load fen='$fen'")
        activeJob = scope.launch(Dispatchers.IO) {
            _state.update { it.copy(loading = true, error = null) }
            val result = queryWithFallback(fen)
            val arrows = result?.moves
                ?.take(3)
                ?.mapIndexedNotNull { i, m -> m.toArrow(i) }
                ?: emptyList()
            Log.d(TAG, "load complete — result=${result != null} moves=${result?.moves?.size} arrows=${arrows.size}")
            _state.update { it.copy(loading = false, result = result, arrows = arrows) }
            onArrowsChanged(arrows)
        }
    }

    private suspend fun queryWithFallback(fen: String): com.acepero13.chess.core.opening.OpeningExplorerResult? {
        val lichessResult = runCatching { LichessMastersExplorer.query(fen, lichessToken()) }
        if (lichessResult.isFailure) {
            Log.w(TAG, "Lichess threw exception: ${lichessResult.exceptionOrNull()?.javaClass?.simpleName} — ${lichessResult.exceptionOrNull()?.message}")
        } else if (lichessResult.getOrNull() == null) {
            Log.w(TAG, "Lichess returned null for fen='$fen', falling back to ChessDB")
        } else {
            return lichessResult.getOrNull()
        }
        val chessDbResult = runCatching { ChessDbExplorer.query(fen) }
        if (chessDbResult.isFailure) {
            Log.e(TAG, "ChessDB also threw exception: ${chessDbResult.exceptionOrNull()?.javaClass?.simpleName} — ${chessDbResult.exceptionOrNull()?.message}")
        } else if (chessDbResult.getOrNull() == null) {
            Log.w(TAG, "ChessDB returned null for fen='$fen'")
        }
        return chessDbResult.getOrNull()
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
