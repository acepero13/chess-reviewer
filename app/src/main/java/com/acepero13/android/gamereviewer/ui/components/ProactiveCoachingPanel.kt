package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiObjects
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acepero13.android.gamereviewer.domain.CoachingTrigger
import com.acepero13.android.gamereviewer.domain.InsightReconciler
import com.acepero13.android.gamereviewer.ui.screens.CoordinationQuizPhase
import com.acepero13.chess.core.ui.theme.AnalyzeBlue
import com.acepero13.chess.core.ui.theme.ChessGold

private val PanelBg     = Color(0xFF1A1A1A)
private val PanelBorder = Color(0xFF2D6A4F)
private val PanelText   = Color(0xFFCCE8D9)

private fun CoachingTrigger.supportsSquareAnswer(): Boolean = when (this) {
    is CoachingTrigger.PreMoveChecklist -> true
    is CoachingTrigger.WorstPiece       -> true
    is CoachingTrigger.Safety           -> true
    is CoachingTrigger.CctCheck         -> true
    else                                -> false
}

/**
 * Proactive coaching panel with a compact default view.
 * Tap the expand icon to open a full-screen dialog with all coaching questions and hints.
 * Interactive board elements (square identification, forcing sequence) stay in the compact card.
 */
@Composable
fun ProactiveCoachingPanel(
    trigger:                  CoachingTrigger,
    visible:                  Boolean,
    onDismiss:                () -> Unit,
    modifier:                 Modifier = Modifier,
    onStartInteraction:       (() -> Unit)? = null,
    proactiveInteractiveMode: Boolean = false,
    proactiveAnswerFeedback:  String? = null,
    proactiveAnswerIsCorrect: Boolean? = null,
    proactiveFoundCount:      Int = 0,
    proactiveTotalCount:      Int = 0,
    coordinationQuizPhase:    CoordinationQuizPhase = CoordinationQuizPhase.ASKING,
    onCoordinationReveal:     (() -> Unit)? = null,
    onTryForcingSequence:     (() -> Unit)? = null,
    onShowForcingSequence:    (() -> Unit)? = null,
    onReplayForcingSequence:  (() -> Unit)? = null,
    forcingSequenceMode:      Boolean = false,
    forcingSequenceAnimating:  Boolean = false,
    forcingSequenceComplete:   Boolean = false,
    forcingSequenceCurrentStep: Int = 0,
    forcingSequenceTotalSteps:  Int = 0,
    isWeakArea:                 Boolean = false,
) {
    val insight   = InsightReconciler.forTrigger(trigger)
    val hasAnswer = trigger.supportsSquareAnswer()

    var showQuestionsDialog  by remember { mutableStateOf(false) }
    var showReflectionDialog by remember { mutableStateOf(false) }
    var draftText            by remember { mutableStateOf("") }
    var savedReflection      by remember { mutableStateOf("") }

    val isForcingMoveTrigger   = trigger is CoachingTrigger.ForcingMove
    val isPunishBlunderTrigger = trigger is CoachingTrigger.PunishBlunder
    val isCoordinationTrigger  = trigger is CoachingTrigger.CoordinatedAttack ||
            trigger is CoachingTrigger.PieceHarmony
    val hasCoordinationGeometry = when (trigger) {
        is CoachingTrigger.CoordinatedAttack -> trigger.attackerSquares.isNotEmpty() && trigger.targetSquare != null
        is CoachingTrigger.PieceHarmony      -> trigger.attackerSquares.isNotEmpty() && trigger.targetSquares.isNotEmpty()
        else                                 -> false
    }

    // ── Full-content dialog ────────────────────────────────────────────────────
    if (showQuestionsDialog) {
        AlertDialog(
            onDismissRequest  = { showQuestionsDialog = false },
            containerColor    = Color(0xFF1E1E1E),
            titleContentColor = PanelText,
            textContentColor  = PanelText,
            title = {
                Row(
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement  = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = insight.emoji, fontSize = 20.sp)
                    Text(
                        text       = insight.title,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text  = insight.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = PanelText.copy(alpha = 0.85f),
                    )

                    HorizontalDivider(color = PanelBorder.copy(alpha = 0.4f))

                    // Coaching questions
                    Text(
                        text       = "Think about these:",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = PanelText.copy(alpha = 0.6f),
                        fontWeight = FontWeight.SemiBold,
                    )
                    insight.questions.forEachIndexed { i, question ->
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text       = "${i + 1}.",
                                style      = MaterialTheme.typography.bodySmall,
                                color      = ChessGold,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text  = question,
                                style = MaterialTheme.typography.bodySmall,
                                color = PanelText,
                            )
                        }
                    }

                    // Conceptual hint
                    HorizontalDivider(color = PanelBorder.copy(alpha = 0.25f))
                    Row(
                        verticalAlignment      = Alignment.Top,
                        horizontalArrangement  = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.EmojiObjects,
                            contentDescription = null,
                            modifier           = Modifier.size(14.dp).padding(top = 2.dp),
                            tint               = ChessGold.copy(alpha = 0.7f),
                        )
                        Text(
                            text      = insight.conceptualHint,
                            style     = MaterialTheme.typography.bodySmall,
                            color     = PanelText.copy(alpha = 0.65f),
                            fontStyle = FontStyle.Italic,
                        )
                    }

                    // Free-text reflection (non-interactive, non-forcing triggers)
                    if (!hasAnswer && !isCoordinationTrigger && !isForcingMoveTrigger) {
                        HorizontalDivider(color = PanelBorder.copy(alpha = 0.25f))
                        if (savedReflection.isNotEmpty()) {
                            Card(
                                shape  = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF163B2A)),
                                border = BorderStroke(1.dp, PanelBorder.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text       = "Your reflection",
                                        style      = MaterialTheme.typography.labelSmall,
                                        color      = ChessGold.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text  = savedReflection,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PanelText,
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick  = { draftText = savedReflection; showReflectionDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(8.dp),
                                border   = BorderStroke(1.dp, PanelBorder.copy(alpha = 0.5f)),
                            ) {
                                Text(
                                    text  = "Edit reflection",
                                    color = PanelText.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick  = { draftText = ""; showReflectionDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(8.dp),
                                border   = BorderStroke(1.dp, ChessGold.copy(alpha = 0.5f)),
                            ) {
                                Text(
                                    text       = "Write your reflection…",
                                    color      = ChessGold.copy(alpha = 0.85f),
                                    style      = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showQuestionsDialog = false; onDismiss() },
                    shape   = RoundedCornerShape(8.dp),
                    colors  = ButtonDefaults.buttonColors(containerColor = PanelBorder),
                ) {
                    Text("Got it — continue", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuestionsDialog = false }) {
                    Text("Back", color = PanelText.copy(alpha = 0.6f))
                }
            },
        )
    }

    // ── Reflection text entry dialog ───────────────────────────────────────────
    if (showReflectionDialog) {
        AlertDialog(
            onDismissRequest  = { showReflectionDialog = false },
            containerColor    = Color(0xFF1E1E1E),
            titleContentColor = PanelText,
            textContentColor  = PanelText,
            title = {
                Text(
                    text       = "Your Reflection",
                    fontWeight = FontWeight.Bold,
                    style      = MaterialTheme.typography.titleMedium,
                )
            },
            text = {
                OutlinedTextField(
                    value         = draftText,
                    onValueChange = { draftText = it },
                    placeholder   = {
                        Text(
                            text  = "What are you thinking? Write your plan, candidate moves, or observations…",
                            style = MaterialTheme.typography.bodySmall,
                            color = PanelText.copy(alpha = 0.4f),
                        )
                    },
                    modifier  = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    minLines  = 4,
                    maxLines  = 10,
                    colors    = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = ChessGold,
                        unfocusedBorderColor = PanelBorder,
                        focusedTextColor     = PanelText,
                        unfocusedTextColor   = PanelText,
                        cursorColor          = ChessGold,
                    ),
                    shape = RoundedCornerShape(8.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    savedReflection      = draftText.trim()
                    showReflectionDialog = false
                }) {
                    Text("Save", color = ChessGold, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReflectionDialog = false }) {
                    Text("Cancel", color = PanelText.copy(alpha = 0.6f))
                }
            },
        )
    }

    // ── Compact card ───────────────────────────────────────────────────────────
    AnimatedVisibility(
        visible  = visible,
        enter    = fadeIn() + expandVertically(),
        exit     = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        Card(
            shape  = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBg),
            border = BorderStroke(1.dp, PanelBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier            = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {

                // ── Weak-area callout ─────────────────────────────────────────
                if (isWeakArea) {
                    Card(
                        shape  = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF8F00).copy(alpha = 0.12f),
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFF8F00).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text     = "⚠️ You've missed this pattern before — take your time.",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = Color(0xFFFF8F00),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                // ── Header row with expand button ──────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Text(text = insight.emoji, fontSize = 22.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = insight.title,
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color      = PanelText,
                        )
                        Text(
                            text     = insight.description,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = PanelText.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick  = { showQuestionsDialog = true },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.ExpandMore,
                            contentDescription = "Read coaching questions",
                            tint               = ChessGold.copy(alpha = 0.8f),
                        )
                    }
                }

                HorizontalDivider(color = PanelBorder.copy(alpha = 0.4f))

                // ── Interactive board answer ────────────────────────────────────
                if (hasAnswer && onStartInteraction != null) {
                    when {
                        proactiveInteractiveMode -> {
                            val hint = when {
                                trigger is CoachingTrigger.CctCheck && proactiveTotalCount > 0 ->
                                    "👆 Tap each square your opponent can Check or Capture (${proactiveFoundCount}/${proactiveTotalCount} found)…"
                                trigger is CoachingTrigger.CctCheck ->
                                    "👆 Tap squares where your opponent has a Check or Capture…"
                                trigger is CoachingTrigger.PreMoveChecklist && proactiveTotalCount > 0 ->
                                    "👆 Tap each hanging piece on the board (${proactiveFoundCount}/${proactiveTotalCount} found)…"
                                trigger is CoachingTrigger.PreMoveChecklist ->
                                    "👆 Tap the undefended piece(s) on the board…"
                                else ->
                                    "👆 Tap the square on the board to answer…"
                            }
                            Card(
                                shape  = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFB45309).copy(alpha = 0.15f),
                                ),
                                border = BorderStroke(1.dp, Color(0xFFB45309).copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text       = hint,
                                    style      = MaterialTheme.typography.bodySmall,
                                    color      = Color(0xFFFBBF24),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier   = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        }

                        proactiveAnswerFeedback != null -> {
                            val feedbackColor = when (proactiveAnswerIsCorrect) {
                                true  -> Color(0xFF22C55E)
                                false -> Color(0xFFEF4444)
                                null  -> PanelText
                            }
                            Text(
                                text       = proactiveAnswerFeedback,
                                style      = MaterialTheme.typography.bodySmall,
                                color      = feedbackColor,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        else -> {
                            OutlinedButton(
                                onClick  = onStartInteraction,
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(8.dp),
                                border   = BorderStroke(1.dp, ChessGold.copy(alpha = 0.7f)),
                            ) {
                                Text(
                                    text       = "Identify on board ↓",
                                    color      = ChessGold,
                                    style      = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }

                // ── Forcing sequence explorer ──────────────────────────────────
                if (isForcingMoveTrigger) {
                    when {
                        forcingSequenceAnimating -> {
                            val progress = if (forcingSequenceTotalSteps > 0)
                                forcingSequenceCurrentStep.toFloat() / forcingSequenceTotalSteps
                            else 0f
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text  = "Watching the forcing sequence…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PanelText.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.SemiBold,
                                )
                                LinearProgressIndicator(
                                    progress   = { progress },
                                    modifier   = Modifier.fillMaxWidth(),
                                    color      = AnalyzeBlue,
                                    trackColor = PanelBorder.copy(alpha = 0.3f),
                                )
                                Text(
                                    text  = "Move $forcingSequenceCurrentStep of $forcingSequenceTotalSteps",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PanelText.copy(alpha = 0.5f),
                                )
                            }
                        }
                        forcingSequenceComplete -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text  = "Sequence complete. You can now explore variations freely.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AnalyzeBlue.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedButton(
                                        onClick  = { onReplayForcingSequence?.invoke() },
                                        modifier = Modifier.weight(1f),
                                        shape    = RoundedCornerShape(8.dp),
                                        border   = BorderStroke(1.dp, AnalyzeBlue.copy(alpha = 0.7f)),
                                    ) {
                                        Text(
                                            text  = "Replay",
                                            color = AnalyzeBlue,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                    OutlinedButton(
                                        onClick  = onDismiss,
                                        modifier = Modifier.weight(1f),
                                        shape    = RoundedCornerShape(8.dp),
                                        border   = BorderStroke(1.dp, PanelBorder.copy(alpha = 0.5f)),
                                    ) {
                                        Text(
                                            text  = "Explore freely",
                                            color = PanelText.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                        forcingSequenceMode -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text      = "You're in sandbox mode. Play what you think is the forcing continuation.",
                                    style     = MaterialTheme.typography.bodySmall,
                                    color     = PanelText.copy(alpha = 0.85f),
                                    fontStyle = FontStyle.Italic,
                                )
                                Button(
                                    onClick  = { onShowForcingSequence?.invoke() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape    = RoundedCornerShape(8.dp),
                                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D2D2D)),
                                ) {
                                    Text(
                                        text       = "Give up — show the sequence",
                                        color      = PanelText.copy(alpha = 0.8f),
                                        style      = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text  = "There's a forcing sequence here. Can you find it?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ChessGold.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Button(
                                        onClick  = {
                                            onTryForcingSequence?.invoke()
                                            onDismiss()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape    = RoundedCornerShape(8.dp),
                                        colors   = ButtonDefaults.buttonColors(containerColor = PanelBorder),
                                    ) {
                                        Text(
                                            text       = "Try it out",
                                            color      = Color.White,
                                            style      = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                    OutlinedButton(
                                        onClick  = { onShowForcingSequence?.invoke() },
                                        modifier = Modifier.weight(1f),
                                        shape    = RoundedCornerShape(8.dp),
                                        border   = BorderStroke(1.dp, PanelBorder.copy(alpha = 0.5f)),
                                    ) {
                                        Text(
                                            text  = "Just show me",
                                            color = PanelText.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Punish Blunder — play the best response ────────────────────
                if (isPunishBlunderTrigger) {
                    when {
                        forcingSequenceAnimating -> {
                            val progress = if (forcingSequenceTotalSteps > 0)
                                forcingSequenceCurrentStep.toFloat() / forcingSequenceTotalSteps
                            else 0f
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text       = "Playing the best response…",
                                    style      = MaterialTheme.typography.bodySmall,
                                    color      = PanelText.copy(alpha = 0.85f),
                                    fontWeight = FontWeight.SemiBold,
                                )
                                LinearProgressIndicator(
                                    progress   = { progress },
                                    modifier   = Modifier.fillMaxWidth(),
                                    color      = AnalyzeBlue,
                                    trackColor = PanelBorder.copy(alpha = 0.3f),
                                )
                            }
                        }
                        forcingSequenceComplete -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text       = "Best response shown. Explore freely from here.",
                                    style      = MaterialTheme.typography.bodySmall,
                                    color      = AnalyzeBlue.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedButton(
                                        onClick  = { onReplayForcingSequence?.invoke() },
                                        modifier = Modifier.weight(1f),
                                        shape    = RoundedCornerShape(8.dp),
                                        border   = BorderStroke(1.dp, AnalyzeBlue.copy(alpha = 0.7f)),
                                    ) {
                                        Text(
                                            text  = "Replay",
                                            color = AnalyzeBlue,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                    OutlinedButton(
                                        onClick  = onDismiss,
                                        modifier = Modifier.weight(1f),
                                        shape    = RoundedCornerShape(8.dp),
                                        border   = BorderStroke(1.dp, PanelBorder.copy(alpha = 0.5f)),
                                    ) {
                                        Text(
                                            text  = "Explore freely",
                                            color = PanelText.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                        forcingSequenceMode -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text      = "You're in sandbox mode. Play what you think punishes the blunder.",
                                    style     = MaterialTheme.typography.bodySmall,
                                    color     = PanelText.copy(alpha = 0.85f),
                                    fontStyle = FontStyle.Italic,
                                )
                                Button(
                                    onClick  = { onShowForcingSequence?.invoke() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape    = RoundedCornerShape(8.dp),
                                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D2D2D)),
                                ) {
                                    Text(
                                        text       = "Show me the best response",
                                        color      = PanelText.copy(alpha = 0.8f),
                                        style      = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text       = "There's a strong response available. Can you find it?",
                                    style      = MaterialTheme.typography.bodySmall,
                                    color      = ChessGold.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Row(
                                    modifier              = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Button(
                                        onClick  = {
                                            onTryForcingSequence?.invoke()
                                            onDismiss()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape    = RoundedCornerShape(8.dp),
                                        colors   = ButtonDefaults.buttonColors(containerColor = PanelBorder),
                                    ) {
                                        Text(
                                            text       = "Try it yourself",
                                            color      = Color.White,
                                            style      = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                    OutlinedButton(
                                        onClick  = { onShowForcingSequence?.invoke() },
                                        modifier = Modifier.weight(1f),
                                        shape    = RoundedCornerShape(8.dp),
                                        border   = BorderStroke(1.dp, PanelBorder.copy(alpha = 0.5f)),
                                    ) {
                                        Text(
                                            text  = "Play the best move",
                                            color = PanelText.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Coordination visual quiz ───────────────────────────────────
                if (isCoordinationTrigger && hasCoordinationGeometry && onCoordinationReveal != null) {
                    HorizontalDivider(color = PanelBorder.copy(alpha = 0.25f))
                    when (coordinationQuizPhase) {
                        CoordinationQuizPhase.ASKING -> {
                            Text(
                                text      = "Can you spot how these pieces are working together? Try to identify the square they're all targeting before I show you.",
                                style     = MaterialTheme.typography.bodySmall,
                                color     = PanelText.copy(alpha = 0.85f),
                                fontStyle = FontStyle.Italic,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick  = onCoordinationReveal,
                                    modifier = Modifier.weight(1f),
                                    shape    = RoundedCornerShape(8.dp),
                                    border   = BorderStroke(1.dp, AnalyzeBlue.copy(alpha = 0.7f)),
                                ) {
                                    Text(
                                        text  = "I see it",
                                        color = AnalyzeBlue,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                OutlinedButton(
                                    onClick  = onCoordinationReveal,
                                    modifier = Modifier.weight(1f),
                                    shape    = RoundedCornerShape(8.dp),
                                    border   = BorderStroke(1.dp, PanelBorder.copy(alpha = 0.7f)),
                                ) {
                                    Text(
                                        text  = "Show me",
                                        color = PanelText.copy(alpha = 0.7f),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                        CoordinationQuizPhase.REVEALING -> {
                            Text(
                                text  = "↑ Arrows show the coordination on the board.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AnalyzeBlue.copy(alpha = 0.85f),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                // ── Dismiss (hidden while forcing sequence is animating) ────────
                if (!forcingSequenceAnimating) {
                    Button(
                        onClick  = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(8.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = PanelBorder),
                    ) {
                        Text(
                            text       = "Got it — continue",
                            fontWeight = FontWeight.SemiBold,
                            color      = Color.White,
                        )
                    }
                }
            }
        }
    }
}
