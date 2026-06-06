package com.acepero13.android.gamereviewer.ui.screens.analysis

import android.util.Log
import com.acepero13.android.gamereviewer.ui.screens.OrphanSnippetUiState
import com.acepero13.chess.core.data.model.ChessConstants
import com.acepero13.chess.core.engine.StockfishEngine
import com.github.bhlangonijr.chesslib.Board
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "OrphanEngineCtrl"

internal class OrphanEngineController(
    private val state: MutableStateFlow<OrphanSnippetUiState>,
    private val engine: StockfishEngine,
    private val scope: CoroutineScope,
) {

    fun fetchBestMove() {
        val fen = state.value.boardState.fen
        state.update { it.copy(engineThinking = true) }
        scope.launch(Dispatchers.Default) {
            val result = runCatching { engine.analyzePosition(fen, depth = 10) }.getOrNull()
                ?: run { Log.w(TAG, "null best-move for ${fen.take(30)}"); state.update { it.copy(engineThinking = false) }; return@launch }
            val arrow = result.toArrow()
            state.update { s ->
                val active = s.boardState.fen == fen && s.engineVisible
                s.copy(
                    engineThinking = false,
                    boardState     = if (active && arrow != null)
                        s.boardState.copy(arrows = listOf(arrow))
                    else
                        s.boardState.copy(arrows = emptyList()),
                )
            }
        }
    }

    fun fetchEval() {
        val fen = state.value.boardState.fen
        scope.launch(Dispatchers.Default) {
            val result = runCatching {
                engine.analyzePosition(fen, depth = ChessConstants.DEFAULT_ANALYSIS_DEPTH)
            }.getOrNull() ?: run { Log.w(TAG, "null eval for ${fen.take(30)}"); return@launch }
            val evalCp = result.toWhitePerspective(Board().apply { loadFromFen(fen) })
            if (state.value.boardState.fen == fen && state.value.evalBarVisible) {
                state.update { it.copy(evalCp = evalCp) }
            }
        }
    }
}
