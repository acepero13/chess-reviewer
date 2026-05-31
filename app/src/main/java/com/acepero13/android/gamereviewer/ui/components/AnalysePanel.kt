package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.screens.AnalyseSubMode
import com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState
import com.acepero13.android.gamereviewer.ui.screens.AnalysisViewModel
import com.acepero13.chess.core.ui.components.MoveTree
import com.acepero13.chess.core.ui.components.TreeDisplayItem
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

/** Human-readable labels for each color in [ANNOTATION_COLORS] (same order). */
val ANNOTATION_COLOR_LABELS: List<String> = listOf("Gold", "Green", "Red", "Orange", "Blue")

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
        // Compact left-aligned chips — no weight so they wrap their content
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FilterChip(
                selected    = state.evalBarVisible,
                onClick     = vm::toggleEvalBar,
                label       = { Text("Eval Bar", style = MaterialTheme.typography.labelMedium) },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Outlined.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor   = ChessGold.copy(alpha = 0.20f),
                    selectedLabelColor       = ChessGold,
                    selectedLeadingIconColor = ChessGold,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled             = true,
                    selected            = state.evalBarVisible,
                    selectedBorderColor = ChessGold,
                ),
            )
            FilterChip(
                selected    = state.bestMoveVisible,
                onClick     = vm::toggleBestMove,
                label       = { Text("Best Move", style = MaterialTheme.typography.labelMedium) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor   = ChessGold.copy(alpha = 0.20f),
                    selectedLabelColor       = ChessGold,
                    selectedLeadingIconColor = ChessGold,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled             = true,
                    selected            = state.bestMoveVisible,
                    selectedBorderColor = ChessGold,
                ),
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

/**
 * Shows the [MoveTree] only.
 *
 * Engine highlights have been moved to the Stats sheet (human-first philosophy:
 * the user analyses freely here without being guided by the engine's opinion).
 */
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
    // Local dialog state — draft is initialised from the saved comment each time
    // the dialog opens so Cancel truly discards changes.
    var showDialog  by remember { mutableStateOf(false) }
    var draftComment by remember { mutableStateOf(state.currentComment) }

    // Build a human-readable move label for the dialog title (e.g. "14. Nf6")
    val moveLabel = (state.treeItems.filterIsInstance<TreeDisplayItem.MoveItem>()
        .firstOrNull { it.isCurrentMove })
        ?.let { "${it.moveNumber}${if (it.isWhiteMove) "." else "…"} ${it.san}" }
        ?: ""

    if (showDialog) {
        CommentDialog(
            initial   = draftComment,
            moveLabel = moveLabel,
            onSave    = { text ->
                vm.updateMoveComment(text)
                draftComment = text
                showDialog   = false
            },
            onDismiss = { showDialog = false },
        )
    }

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

        // Compact comment chip — tapping opens the dialog
        CommentChip(
            comment = state.currentComment,
            onClick = {
                draftComment = state.currentComment   // sync draft before opening
                showDialog   = true
            },
            modifier = Modifier.fillMaxWidth(),
        )

        // MoveTree remains navigable in Edit sub-mode
        MoveTree(
            entries     = state.treeItems,
            onNodeClick = vm::onMoveNodeClick,
            modifier    = Modifier.fillMaxWidth(),
        )
    }
}

// ── Comment chip ──────────────────────────────────────────────────────────────

/**
 * A single-line tappable row that previews the saved comment (or a placeholder).
 * Tapping it opens the [CommentDialog].
 */
@Composable
private fun CommentChip(
    comment:  String,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasComment = comment.isNotBlank()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = if (hasComment) ChessGold.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector        = Icons.Outlined.Comment,
            contentDescription = "Add comment",
            tint               = if (hasComment) ChessGold
                                 else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier           = Modifier.size(18.dp),
        )
        Text(
            text     = if (hasComment) comment
                       else "Add a comment…",
            style    = MaterialTheme.typography.bodySmall,
            color    = if (hasComment) MaterialTheme.colorScheme.onSurface
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle= if (hasComment) FontStyle.Normal else FontStyle.Italic,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

// ── Comment dialog ─────────────────────────────────────────────────────────────

/**
 * Full-screen-width [AlertDialog] with a multi-line text field for writing
 * or editing the move comment. Save commits; Cancel discards.
 *
 * @param moveLabel Human-readable move notation shown as a subtitle (e.g. "14. Nf6").
 *                  Pass empty string to omit.
 */
@Composable
private fun CommentDialog(
    initial:   String,
    moveLabel: String = "",
    onSave:    (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    "Move comment",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (moveLabel.isNotBlank()) {
                    Text(
                        moveLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        text = {
            OutlinedTextField(
                value         = text,
                onValueChange = { text = it },
                placeholder   = {
                    Text(
                        "What were you thinking here?",
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text("Save", color = ChessGold, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
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
        // Blunder reflection panel (Task 3.1) — animated entry/exit
        AnimatedVisibility(
            visible = state.blunderReflectionMode && state.blunderReflectionInsight != null,
            enter   = fadeIn(tween(250)) + expandVertically(),
            exit    = fadeOut(tween(150)) + shrinkVertically(),
        ) {
            if (state.blunderReflectionInsight != null) {
                BlunderReflectionPanel(
                    insight    = state.blunderReflectionInsight,
                    cpLoss     = state.blunderCpLoss,
                    onRetry    = vm::retryAfterBlunder,
                    onContinue = vm::continueAfterBlunder,
                    modifier   = Modifier.fillMaxWidth(),
                )
            }
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
 * Each circle has a text label below it so the color's meaning is immediately clear.
 * The currently selected color is highlighted with a [ChessGold] border.
 *
 * @param labels Human-readable names for each color (same order as [colors]).
 */
@Composable
private fun ColorPaletteRow(
    colors:          List<Color>,
    labels:          List<String> = ANNOTATION_COLOR_LABELS,
    selectedColor:   Color,
    onColorSelected: (Color) -> Unit,
    modifier:        Modifier = Modifier,
) {
    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        colors.forEachIndexed { index, color ->
            val isSelected = color == selectedColor
            val label      = labels.getOrElse(index) { "" }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.clickable { onColorSelected(color) },
            ) {
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
                        ),
                )
                if (label.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text     = label,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = color,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
