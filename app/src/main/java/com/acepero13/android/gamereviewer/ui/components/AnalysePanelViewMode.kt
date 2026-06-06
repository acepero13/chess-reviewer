package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState
import com.acepero13.android.gamereviewer.ui.screens.AnalysisViewModel
import com.acepero13.chess.core.ui.components.MoveTree

@Composable
internal fun ViewSubMode(state: AnalysisUiState, vm: AnalysisViewModel, modifier: Modifier = Modifier) {
    MoveTree(entries = state.treeItems, onNodeClick = vm::onMoveNodeClick, modifier = modifier.fillMaxWidth())
}
