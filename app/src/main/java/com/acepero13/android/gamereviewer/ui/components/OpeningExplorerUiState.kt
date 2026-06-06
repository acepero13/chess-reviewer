package com.acepero13.android.gamereviewer.ui.components

import com.acepero13.chess.core.opening.OpeningExplorerResult
import com.acepero13.chess.core.ui.board.Arrow

data class OpeningExplorerUiState(
    val loading: Boolean = false,
    val result: OpeningExplorerResult? = null,
    val error: String? = null,
    val arrows: List<Arrow> = emptyList(),
)
