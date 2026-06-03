package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiObjects
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.InsightReconciler
import com.acepero13.android.gamereviewer.ui.screens.ClassificationOption
import com.acepero13.android.gamereviewer.ui.screens.MentorMoveResult
import com.acepero13.chess.core.ui.theme.ChessGold

// Design tokens
private val CorrectGreen   = Color(0xFF2E7D32)
private val CorrectOnGreen = Color(0xFFC8E6C9)
private val CloseAmber     = Color(0xFF1E1600)
private val CloseOnAmber   = Color(0xFFE8C882)
private val WrongRed       = Color(0xFF4E0808)
private val WrongOnRed     = Color(0xFFFFCDD2)

/**
 * **Guided Discovery Panel** — compact-first, expandable layout.
 *
 * By default only the **insight headline** (emoji + title) and the **action buttons**
 * are visible — no scrolling required, board taps work normally above this panel.
 * The user can tap the header to expand it and reveal the description and questions.
 *
 * Interaction flow:
 * 1. Header (collapsed) — tap to expand questions & description.
 * 2. "Play your answer" — board becomes interactive; move checked by engine.
 * 3. After any move result → 4-option classification quiz appears.
 * 4. "Get a Hint" / "Show Answer" → conceptual hint / engine arrow.
 * 5. "Got it — continue" → saves thoughts and advances or exits.
 */
@Composable
fun GuidedDiscoveryPanel(
    insight: InsightReconciler.Insight,
    insightRevealed: Boolean,
    thoughts: String,
    hintVisible: Boolean,
    answerRevealed: Boolean,
    engineThinking: Boolean,
    revealedEvalCp: Int?,
    // ── Mentor move-input ──────────────────────────────────────────────────────
    mentorMoveInputActive: Boolean,
    mentorMoveChecking: Boolean,
    mentorMoveResult: MentorMoveResult?,
    mentorMoveFeedback: String,
    // ── Classification quiz ────────────────────────────────────────────────────
    showClassificationQuiz: Boolean,
    classificationOptions: List<ClassificationOption>,
    classificationCorrectIndex: Int,
    classificationSelectedIndex: Int,
    onSelectClassification: (Int) -> Unit,
    // ── Callbacks ─────────────────────────────────────────────────────────────
    onThoughtsChange: (String) -> Unit,
    onRevealHint: () -> Unit,
    onRevealAnswer: () -> Unit,
    onSubmit: () -> Unit,
    onExit: () -> Unit,
    onToggleMoveInput: () -> Unit,
    onRetryMove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Expand/collapse the description + questions section. Collapsed by default
    // so only the headline and action buttons are visible without scrolling.
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {

        // ── Collapsible header (emoji · title · description · questions) ────────
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            shape    = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (insightRevealed) Modifier.clickable { expanded = !expanded } else Modifier),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Always-visible title row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text  = if (insightRevealed) insight.emoji else "🎯",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = if (insightRevealed) insight.title else "Find the best move",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier   = Modifier.weight(1f),
                    )
                    if (insightRevealed) {
                        Icon(
                            imageVector        = if (expanded) Icons.Outlined.ExpandLess
                                                else Icons.Outlined.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint               = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            modifier           = Modifier.size(18.dp),
                        )
                    }
                }

                // Expandable: description + questions — only available after insight is revealed
                AnimatedVisibility(
                    visible = insightRevealed && expanded,
                    enter   = fadeIn() + expandVertically(),
                    exit    = fadeOut() + shrinkVertically(),
                ) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Description
                        if (insight.description.isNotBlank()) {
                            Text(
                                text  = insight.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                            )
                        }

                        // Questions
                        if (insight.questions.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text       = "Think about these questions:",
                                    style      = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = ChessGold,
                                )
                                insight.questions.forEachIndexed { i, q ->
                                    Row(
                                        modifier          = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Text(
                                            text     = "${i + 1}.",
                                            style    = MaterialTheme.typography.bodySmall,
                                            color    = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.width(18.dp),
                                        )
                                        Text(
                                            text     = q,
                                            style    = MaterialTheme.typography.bodySmall,
                                            color    = MaterialTheme.colorScheme.onSecondaryContainer,
                                            maxLines = 3,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── "Play your answer" section ─────────────────────────────────────────
        MoveInputSection(
            active         = mentorMoveInputActive,
            checking       = mentorMoveChecking,
            result         = mentorMoveResult,
            feedback       = mentorMoveFeedback,
            answerRevealed = answerRevealed,
            onToggle       = onToggleMoveInput,
            onRetry        = onRetryMove,
        )

        // ── Classification quiz (after any move attempt or Show Answer) ─────────
        AnimatedVisibility(
            visible = showClassificationQuiz,
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically(),
        ) {
            ClassificationQuizSection(
                options       = classificationOptions,
                correctIndex  = classificationCorrectIndex,
                selectedIndex = classificationSelectedIndex,
                onSelect      = onSelectClassification,
            )
        }

        // ── Conceptual hint ────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = hintVisible,
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = Icons.Outlined.EmojiObjects,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier           = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text       = "Conceptual Hint",
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text      = insight.conceptualHint,
                    style     = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color     = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }

        // ── Engine reveal info ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = answerRevealed,
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = Icons.Outlined.Visibility,
                        contentDescription = null,
                        tint               = ChessGold,
                        modifier           = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text       = "Engine Answer Revealed",
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color      = ChessGold,
                    )
                }
                if (revealedEvalCp != null) {
                    Spacer(Modifier.height(4.dp))
                    val evalText = when {
                        revealedEvalCp > 9000  -> "Forced checkmate for White"
                        revealedEvalCp < -9000 -> "Forced checkmate for Black"
                        revealedEvalCp > 0     -> "+%.2f (White is better)".format(revealedEvalCp / 100f)
                        revealedEvalCp < 0     -> "%.2f (Black is better)".format(revealedEvalCp / 100f)
                        else                   -> "Equal position (0.00)"
                    }
                    Text(
                        text  = evalText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "The green arrow on the board shows the engine's best move.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
            }
        }

        // ── Action buttons (always visible) ───────────────────────────────────
        when {
            answerRevealed ||
                mentorMoveResult == MentorMoveResult.CORRECT ||
                mentorMoveResult == MentorMoveResult.CLOSE -> {
                Button(
                    onClick  = onSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = ChessGold),
                ) {
                    Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Got it — continue review", color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            engineThinking || mentorMoveChecking -> {
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text  = if (mentorMoveChecking) "Checking your move…" else "Loading engine answer…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!hintVisible) {
                        OutlinedButton(
                            onClick  = onRevealHint,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                Icons.Outlined.EmojiObjects, null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Get a Hint")
                        }
                    }
                    Button(
                        onClick  = onRevealAnswer,
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.buttonColors(containerColor = ChessGold),
                    ) {
                        Icon(
                            Icons.Outlined.Visibility, null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Show Answer", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                OutlinedButton(
                    onClick  = onExit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Exit Guided Review")
                }
            }
        }

        // ── Optional notes field (collapsed behind toggle) ─────────────────────
        ThoughtsInput(
            thoughts         = thoughts,
            onThoughtsChange = onThoughtsChange,
            visible          = !mentorMoveInputActive,
        )
    }
}

// ── Classification quiz ───────────────────────────────────────────────────────

@Composable
private fun ClassificationQuizSection(
    options:       List<ClassificationOption>,
    correctIndex:  Int,
    selectedIndex: Int,
    onSelect:      (Int) -> Unit,
    modifier:      Modifier = Modifier,
) {
    val answered = selectedIndex >= 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text       = "Why was this moment critical?",
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color      = ChessGold,
        )
        options.forEachIndexed { idx, option ->
            val isCorrect  = idx == correctIndex
            val isSelected = idx == selectedIndex
            val bgColor = when {
                !answered  -> MaterialTheme.colorScheme.surface
                isCorrect  -> CorrectGreen
                isSelected -> WrongRed
                else       -> MaterialTheme.colorScheme.surface
            }
            val borderColor = when {
                !answered  -> MaterialTheme.colorScheme.outline
                isCorrect  -> CorrectGreen
                isSelected -> WrongRed
                else       -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            }
            val textColor = when {
                !answered  -> MaterialTheme.colorScheme.onSurface
                isCorrect  -> CorrectOnGreen
                isSelected -> WrongOnRed
                else       -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .then(if (!answered) Modifier.clickable { onSelect(idx) } else Modifier)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = option.label,
                        style      = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = textColor,
                    )
                    if (option.description.isNotBlank()) {
                        Text(
                            text  = option.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.8f),
                        )
                    }
                }
                if (answered && isCorrect) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector        = Icons.Outlined.CheckCircle,
                        contentDescription = "Correct",
                        tint               = CorrectOnGreen,
                        modifier           = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

// ── Notes input (dialog) ──────────────────────────────────────────────────────

@Composable
private fun ThoughtsInput(
    thoughts:         String,
    onThoughtsChange: (String) -> Unit,
    visible:          Boolean,
    modifier:         Modifier = Modifier,
) {
    var showDialog    by remember { mutableStateOf(false) }
    var draftThoughts by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    "Analysis notes",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                OutlinedTextField(
                    value         = draftThoughts,
                    onValueChange = { draftThoughts = it },
                    placeholder   = {
                        Text(
                            "Write what you see in this position…",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                )
            },
            confirmButton = {
                TextButton(onClick = { onThoughtsChange(draftThoughts); showDialog = false }) {
                    Text("Save", color = ChessGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn() + expandVertically(),
        exit    = fadeOut() + shrinkVertically(),
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable { draftThoughts = thoughts; showDialog = true }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector        = Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint               = if (thoughts.isNotBlank()) ChessGold
                                     else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text  = if (thoughts.isNotBlank()) "Edit analysis notes" else "Add analysis notes (optional)",
                style = MaterialTheme.typography.labelSmall,
                color = if (thoughts.isNotBlank()) ChessGold
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Move-input section ────────────────────────────────────────────────────────

@Composable
private fun MoveInputSection(
    active:         Boolean,
    checking:       Boolean,
    result:         MentorMoveResult?,
    feedback:       String,
    answerRevealed: Boolean,
    onToggle:       () -> Unit,
    onRetry:        () -> Unit,
    modifier:       Modifier = Modifier,
) {
    if (answerRevealed) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // Feedback card
        if (result != null && feedback.isNotBlank()) {
            val (bg, fg) = when (result) {
                MentorMoveResult.CORRECT   -> CorrectGreen to CorrectOnGreen
                MentorMoveResult.CLOSE     -> CloseAmber   to CloseOnAmber
                MentorMoveResult.INCORRECT -> WrongRed     to WrongOnRed
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text       = feedback,
                    style      = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = fg,
                )
                if (result == MentorMoveResult.INCORRECT) {
                    FilledTonalButton(
                        onClick  = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Try a different move")
                    }
                }
            }
        }

        // Toggle / instruction strip
        if (result == null || result == MentorMoveResult.INCORRECT) {
            if (!active) {
                OutlinedButton(
                    onClick  = onToggle,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Outlined.SportsEsports, null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Play your answer on the board")
                }
            } else if (!checking) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text  = "Tap a piece, then tap its destination square.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick  = onToggle,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
