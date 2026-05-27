package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.screens.AnalyseSubMode
import com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState
import com.acepero13.android.gamereviewer.ui.screens.AnalysisViewModel
import com.acepero13.chess.core.ui.components.MoveTree
import com.acepero13.chess.core.ui.theme.ChessGold

// ── Annotation color palette ──────────────────────────────────────────────────

/** Five annotation colors available for arrows and square marks in Edit sub-mode. */
val ANNOTATION_COLORS: List<Color> = listOf(
    Color(0xCCF0A500.toInt()),  // ChessGold
    Color(0xCC48BB78.toInt()),  // CorrectGreen
    Color(0xCCF56565.toInt()),  // WrongRed
    Color(0xCCFF9800.toInt()),  // WarningOrange
    Color(0xCC64B5F6.toInt()),  // AnalyzeBlue
)

// ── AnalysePanel ─────────────────────────────────────────────────────────────

/**
 * Content panel for **Analyse mode**.
 *
 * Layout:
 * ```
 * [Eval Bar chip] [Best Move chip]
 * ────────────────────────────────
 * Sub-mode content:
 *   VIEW    → MoveTree
 *   EDIT    → Color pickers + comment field + MoveTree
 *   EXPLORE → BlunderReflectionPanel (if active) + progress + Exit button
 * ```
 */
@Composable
fun AnalysePanel(
    state:    AnalysisUiState,
    vm:       AnalysisViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier          = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── Engine toggle chips ────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FilterChip(
                selected = state.evalBarVisible,
                onClick  = vm::toggleEvalBar,
                label    = { Text("Eval Bar") },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = state.bestMoveVisible,
                onClick  = vm::toggleBestMove,
                label    = { Text("Best Move") },
                modifier = Modifier.weight(1f),
            )
        }

        // ── Sub-mode content ───────────────────────────────────────────────────
        when (state.analyseSubMode) {
            AnalyseSubMode.VIEW    -> ViewSubMode(state, vm)
            AnalyseSubMode.EDIT    -> EditSubMode(state, vm)
            AnalyseSubMode.EXPLORE -> ExploreSubMode(state, vm)
        }
    }
}

// ── VIEW sub-mode ─────────────────────────────────────────────────────────────

@Composable
private fun ViewSubMode(
    state:    AnalysisUiState,
    vm:       AnalysisViewModel,
    modifier: Modifier = Modifier,
) {
    MoveTree(
        entries     = state.treeItems,
        onNodeClick = vm::onMoveNodeClick,
        modifier    = modifier.fillMaxWidth(),
    )
}

// ── EDIT sub-mode ─────────────────────────────────────────────────────────────

@Composable
private fun EditSubMode(
    state:    AnalysisUiState,
    vm:       AnalysisViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier            = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Single annotation color picker (applies to both arrows and square marks)
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

        // Move comment field
        OutlinedTextField(
            value         = state.currentComment,
            onValueChange = { vm.updateMoveComment(it) },
            label         = { Text("Move comment") },
            placeholder   = {
                Text(
                    "What were you thinking here?",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
        )

        // MoveTree remains navigable in Edit sub-mode
        MoveTree(
            entries     = state.treeItems,
            onNodeClick = vm::onMoveNodeClick,
            modifier    = Modifier.fillMaxWidth(),
        )
    }
}

// ── EXPLORE sub-mode ──────────────────────────────────────────────────────────

@Composable
private fun ExploreSubMode(
    state:    AnalysisUiState,
    vm:       AnalysisViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier            = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Blunder reflection panel (Task 3.1)
        if (state.blunderReflectionMode && state.blunderReflectionInsight != null) {
            BlunderReflectionPanel(
                insight    = state.blunderReflectionInsight,
                cpLoss     = state.blunderCpLoss,
                onRetry    = vm::retryAfterBlunder,
                onContinue = vm::continueAfterBlunder,
                modifier   = Modifier.fillMaxWidth(),
            )
        }

        // Engine-thinking progress bar
        if (state.sandboxEngineThinking) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
            )
        }

        // Exit Explore mode
        FilledTonalButton(
            onClick  = vm::exitSandboxMode,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Exit Explore Mode")
        }
    }
}

// ── Color palette row ─────────────────────────────────────────────────────────

/**
 * A horizontal row of five colored circles for annotation color selection.
 * The currently selected color is highlighted with a [ChessGold] border.
 */
@Composable
private fun ColorPaletteRow(
    colors:          List<Color>,
    selectedColor:   Color,
    onColorSelected: (Color) -> Unit,
    modifier:        Modifier = Modifier,
) {
    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        colors.forEach { color ->
            val isSelected = color == selectedColor
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (isSelected)
                            Modifier.border(2.dp, ChessGold, CircleShape)
                        else
                            Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    )
                    .clickable { onColorSelected(color) },
            )
        }
    }
}
