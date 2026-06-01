package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acepero13.android.gamereviewer.domain.CoachingTrigger
import com.acepero13.android.gamereviewer.domain.InsightReconciler
import com.acepero13.android.gamereviewer.ui.screens.CoordinationQuizPhase
import com.acepero13.chess.core.ui.theme.AnalyzeBlue
import com.acepero13.chess.core.ui.theme.ChessGold

private val PanelBg     = Color(0xFF1A1A1A)
private val PanelBorder = Color(0xFF2D6A4F)   // muted green — distinct from the gold Mentor panel
private val PanelText   = Color(0xFFCCE8D9)

/** Returns true for triggers that support board-tap identification. */
private fun CoachingTrigger.supportsSquareAnswer(): Boolean = when (this) {
    is CoachingTrigger.PreMoveChecklist -> true  // tap hanging pieces
    is CoachingTrigger.WorstPiece       -> true  // tap the restricted piece
    is CoachingTrigger.Safety           -> true  // tap the exposed king
    is CoachingTrigger.CctCheck         -> true  // tap squares targeted by opponent CCT moves
    else                                -> false
}

/**
 * Lightweight proactive coaching panel for Board Scan triggers.
 *
 * For triggers that have a concrete board answer (PreMoveChecklist, WorstPiece, Safety)
 * an "Identify on board" button lets the user tap the relevant square and receive
 * immediate visual feedback.  Other triggers show only the Socratic questions.
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
) {
    val insight = InsightReconciler.forTrigger(trigger)
    val hasAnswer = trigger.supportsSquareAnswer()

    var showReflectionDialog by remember { mutableStateOf(false) }
    var draftText             by remember { mutableStateOf("") }
    var savedReflection       by remember { mutableStateOf("") }

    if (showReflectionDialog) {
        AlertDialog(
            onDismissRequest = { showReflectionDialog = false },
            containerColor   = Color(0xFF1E1E1E),
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
                    savedReflection    = draftText.trim()
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

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn() + expandVertically(),
        exit    = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        Card(
            shape  = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBg),
            border = BorderStroke(1.dp, PanelBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {

                // ── Header ─────────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text     = insight.emoji,
                        fontSize = 22.sp,
                    )
                    Column {
                        Text(
                            text       = insight.title,
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color      = PanelText,
                        )
                        Text(
                            text  = insight.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = PanelText.copy(alpha = 0.75f),
                        )
                    }
                }

                HorizontalDivider(color = PanelBorder.copy(alpha = 0.4f))

                // ── Interactive board answer — shown first so it's always visible ──
                if (hasAnswer && onStartInteraction != null) {
                    when {
                        // Waiting for user to tap a square
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

                        // Feedback after the user answered
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

                        // Prompt to start
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
                    HorizontalDivider(color = PanelBorder.copy(alpha = 0.25f))
                }

                // ── Coaching questions ─────────────────────────────────────────
                Text(
                    text       = "Think about these:",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = PanelText.copy(alpha = 0.6f),
                    fontWeight = FontWeight.SemiBold,
                )
                insight.questions.forEachIndexed { i, question ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text  = "${i + 1}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChessGold,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text  = question,
                            style = MaterialTheme.typography.bodySmall,
                            color = PanelText,
                        )
                    }
                }

                // ── Conceptual hint ────────────────────────────────────────────
                Spacer(Modifier.height(2.dp))
                Text(
                    text      = insight.conceptualHint,
                    style     = MaterialTheme.typography.bodySmall,
                    color     = PanelText.copy(alpha = 0.6f),
                    fontStyle = FontStyle.Italic,
                )

                // ── Coordination visual quiz ───────────────────────────────────
                val isCoordinationTrigger = trigger is CoachingTrigger.CoordinatedAttack ||
                        trigger is CoachingTrigger.PieceHarmony
                val hasCoordinationGeometry = when (trigger) {
                    is CoachingTrigger.CoordinatedAttack -> trigger.attackerSquares.isNotEmpty() && trigger.targetSquare != null
                    is CoachingTrigger.PieceHarmony      -> trigger.attackerSquares.isNotEmpty() && trigger.targetSquares.isNotEmpty()
                    else                                 -> false
                }
                if (isCoordinationTrigger && hasCoordinationGeometry && onCoordinationReveal != null) {
                    HorizontalDivider(color = PanelBorder.copy(alpha = 0.25f))
                    when (coordinationQuizPhase) {
                        CoordinationQuizPhase.ASKING -> {
                            Text(
                                text  = "Can you spot how these pieces are working together? Try to identify the square they're all targeting before I show you.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PanelText.copy(alpha = 0.85f),
                                fontStyle = FontStyle.Italic,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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

                // ── Free-text reflection (Socratic-only triggers) ──────────────
                if (!hasAnswer && !isCoordinationTrigger) {
                    HorizontalDivider(color = PanelBorder.copy(alpha = 0.25f))

                    if (savedReflection.isNotEmpty()) {
                        Card(
                            shape  = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF163B2A),
                            ),
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

                // ── Dismiss ────────────────────────────────────────────────────
                Spacer(Modifier.height(2.dp))
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
