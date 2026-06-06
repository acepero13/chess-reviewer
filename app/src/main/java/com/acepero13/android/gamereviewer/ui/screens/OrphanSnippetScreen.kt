package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.data.model.Snippet
import com.acepero13.android.gamereviewer.ui.components.ANNOTATION_COLORS
import com.acepero13.android.gamereviewer.ui.components.ColorPaletteRow
import com.acepero13.chess.core.ui.board.ChessBoard
import com.acepero13.chess.core.ui.components.EvalBar
import com.acepero13.chess.core.ui.components.MoveTree
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrphanSnippetScreen(
    snippet: Snippet,
    onBack:  () -> Unit,
    vm:      OrphanSnippetViewModel = koinViewModel(parameters = { parametersOf(snippet.id) }),
) {
    val state     by vm.uiState.collectAsState()
    val appColors  = LocalAppColors.current

    Scaffold(
        containerColor = appColors.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Bookmark, null, tint = ChessGold, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text       = snippet.title,
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = ChessGold,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, "Back", tint = ChessGold)
                    }
                },
                actions = {
                    IconButton(onClick = vm::toggleEditMode) {
                        Icon(
                            imageVector        = Icons.Outlined.Edit,
                            contentDescription = "Toggle edit mode",
                            tint               = if (state.isEditMode) ChessGold else appColors.iconSubtle,
                        )
                    }
                    IconButton(onClick = vm::toggleEngine) {
                        Icon(
                            imageVector        = Icons.Outlined.Lightbulb,
                            contentDescription = "Toggle engine move",
                            tint               = if (state.engineVisible) ChessGold else appColors.iconSubtle,
                        )
                    }
                    IconButton(onClick = vm::toggleEvalBar) {
                        Icon(
                            imageVector        = Icons.Outlined.BarChart,
                            contentDescription = "Toggle eval bar",
                            tint               = if (state.evalBarVisible) ChessGold else appColors.iconSubtle,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appColors.background),
            )
        },
    ) { padding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnimatedVisibility(state.evalBarVisible, enter = fadeIn(), exit = fadeOut()) {
                EvalBar(evalCp = state.evalCp, thinking = state.engineThinking, modifier = Modifier.fillMaxWidth())
            }
            ChessBoard(
                boardState     = state.boardState,
                onSquareTap    = { sq -> if (!state.isEditMode) vm.onSquareTap(sq) },
                onArrowDrawn   = vm::onArrowDrawn,
                onSquareMarked = vm::onSquareMarked,
                modifier       = Modifier.fillMaxWidth(),
                thumbnailMode  = false,
            )
            if (state.engineThinking) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ChessGold.copy(alpha = 0.6f))
                )
            }
            AnimatedVisibility(state.isEditMode, enter = fadeIn(), exit = fadeOut()) {
                AnnotationEditControls(state, vm)
            }
            MoveTree(
                entries     = state.treeItems,
                onNodeClick = vm::onMoveNodeClick,
                modifier    = Modifier.fillMaxWidth(),
            )
            SnippetNavigationRow(
                canGoBack   = state.canGoBack,
                onStepBack  = vm::stepBack,
                onReset     = vm::resetToStart,
            )
            OutlinedTextField(
                value         = state.currentComment,
                onValueChange = vm::updateComment,
                label         = { Text("Comment", color = appColors.textSecondary) },
                modifier      = Modifier.fillMaxWidth(),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = ChessGold,
                    unfocusedBorderColor = appColors.border,
                    focusedTextColor     = appColors.textPrimary,
                    unfocusedTextColor   = appColors.textPrimary,
                ),
                maxLines = 4,
            )
        }
    }
}

@Composable
private fun AnnotationEditControls(state: OrphanSnippetUiState, vm: OrphanSnippetViewModel) {
    val hasAnnotations = state.boardState.userArrows.isNotEmpty() || state.boardState.markedSquares.isNotEmpty()
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text  = "Annotation color",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ColorPaletteRow(
            colors          = ANNOTATION_COLORS,
            selectedColor   = state.currentArrowColor,
            onColorSelected = vm::setArrowColor,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(onClick = vm::undoLastArrow, enabled = state.boardState.userArrows.isNotEmpty(), modifier = Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Outlined.Undo, "Undo last arrow", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Undo", style = MaterialTheme.typography.labelMedium)
            }
            FilledTonalButton(onClick = vm::clearAnnotations, enabled = hasAnnotations, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.ClearAll, "Clear all annotations", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Clear", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SnippetNavigationRow(canGoBack: Boolean, onStepBack: () -> Unit, onReset: () -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = ChessGold.copy(alpha = if (canGoBack) 1f else 0.38f)
        IconButton(onClick = onReset, enabled = canGoBack) {
            Icon(Icons.Outlined.FastRewind, "Reset to start", tint = tint)
        }
        IconButton(onClick = onStepBack, enabled = canGoBack) {
            Icon(Icons.Outlined.SkipPrevious, "Step back", tint = tint)
        }
    }
}
