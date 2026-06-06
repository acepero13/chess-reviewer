package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.screens.AnalyseSubMode
import com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState
import com.acepero13.android.gamereviewer.ui.screens.AnalysisViewModel
import com.acepero13.chess.core.ui.theme.ChessGold

@Composable
private fun EngineToggleChips(
    evalBarVisible: Boolean, bestMoveVisible: Boolean,
    onToggleEval: () -> Unit, onToggleBestMove: () -> Unit,
) {
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor   = ChessGold.copy(alpha = 0.20f),
        selectedLabelColor       = ChessGold,
        selectedLeadingIconColor = ChessGold,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilterChip(selected = evalBarVisible, onClick = onToggleEval, colors = chipColors,
            label = { Text("Eval Bar", style = MaterialTheme.typography.labelMedium) },
            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.TrendingUp, null, Modifier.size(FilterChipDefaults.IconSize)) },
            border = FilterChipDefaults.filterChipBorder(true, evalBarVisible, selectedBorderColor = ChessGold))
        FilterChip(selected = bestMoveVisible, onClick = onToggleBestMove, colors = chipColors,
            label = { Text("Best Move", style = MaterialTheme.typography.labelMedium) },
            leadingIcon = { Icon(Icons.Outlined.Lightbulb, null, Modifier.size(FilterChipDefaults.IconSize)) },
            border = FilterChipDefaults.filterChipBorder(true, bestMoveVisible, selectedBorderColor = ChessGold))
    }
}

@Composable
fun AnalysePanel(state: AnalysisUiState, vm: AnalysisViewModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EngineToggleChips(state.evalBarVisible, state.bestMoveVisible, vm::toggleEvalBar, vm::toggleBestMove)
        when (state.analyseSubMode) {
            AnalyseSubMode.VIEW    -> ViewSubMode(state, vm)
            AnalyseSubMode.EDIT    -> EditSubMode(state, vm)
            AnalyseSubMode.EXPLORE -> ExploreSubMode(state, vm)
        }
    }
}
