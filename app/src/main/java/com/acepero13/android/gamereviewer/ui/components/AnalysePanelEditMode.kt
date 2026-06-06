package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState
import com.acepero13.android.gamereviewer.ui.screens.AnalysisViewModel
import com.acepero13.chess.core.ui.components.MoveTree
import com.acepero13.chess.core.ui.components.TreeDisplayItem

@Composable
private fun AnnotationToolbar(hasArrows: Boolean, onUndo: () -> Unit, onClear: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilledTonalButton(onClick = onUndo, enabled = hasArrows, modifier = Modifier.weight(1f)) {
            Icon(Icons.AutoMirrored.Outlined.Undo, "Undo last arrow", modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Undo", style = MaterialTheme.typography.labelMedium)
        }
        FilledTonalButton(onClick = onClear, enabled = hasArrows, modifier = Modifier.weight(1f)) {
            Icon(Icons.Outlined.ClearAll, "Clear all arrows", modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Clear", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun EditSubMode(state: AnalysisUiState, vm: AnalysisViewModel, modifier: Modifier = Modifier) {
    var showDialog   by remember { mutableStateOf(false) }
    var draftComment by remember { mutableStateOf(state.currentComment) }
    val moveLabel = (state.treeItems.filterIsInstance<TreeDisplayItem.MoveItem>()
        .firstOrNull { it.isCurrentMove })
        ?.let { "${it.moveNumber}${if (it.isWhiteMove) "." else "…"} ${it.san}" } ?: ""
    if (showDialog) {
        CommentDialog(draftComment, moveLabel,
            onSave    = { text -> vm.updateMoveComment(text); draftComment = text; showDialog = false },
            onDismiss = { showDialog = false })
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Annotation color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ColorPaletteRow(ANNOTATION_COLORS, selectedColor = state.currentArrowColor, onColorSelected = vm::setArrowColor)
        AnnotationToolbar(state.boardState.userArrows.isNotEmpty(), vm::undoLastArrow, vm::clearArrows)
        CommentChip(state.currentComment, onClick = { draftComment = state.currentComment; showDialog = true }, Modifier.fillMaxWidth())
        MoveTree(state.treeItems, vm::onMoveNodeClick, Modifier.fillMaxWidth())
    }
}
