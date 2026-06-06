package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState
import com.acepero13.android.gamereviewer.domain.TruthMapEntry
import com.acepero13.chess.core.data.model.PositionAnnotation
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Holds all mutable game data shared across analysis handler classes.
 * Injected into every controller so they operate on the same in-memory game state.
 */
internal class GameSession(
    val uiState: MutableStateFlow<AnalysisUiState>,
    val scope: CoroutineScope,
    val gameId: Long,
) {
    var uciMoves: List<String> = emptyList()
    var sanMoves: List<String> = emptyList()
    var fenSequence: List<String> = emptyList()
    var truthMap: List<TruthMapEntry> = emptyList()
    var sandboxEvalCp: Int? = null
    val annotationCache = mutableMapOf<String, PositionAnnotation?>()
    var pgnAnnotations: Map<Int, String> = emptyMap()
    var boardFlippedForBlack: Boolean = false
    var playerSideKnown: Boolean = false
    val gson = Gson()

    val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
}
