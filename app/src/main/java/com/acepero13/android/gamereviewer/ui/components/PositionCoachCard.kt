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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acepero13.android.gamereviewer.engine.highlights.GameHighlight
import com.acepero13.chess.core.ui.theme.ChessGold

private val CardBg     = Color(0xFF1A1A2E)
private val CardBorder = Color(0xFF3B4A6B)
private val CardText   = Color(0xFFCDD5F0)

private data class PhaseQuestions(val phase: String, val emoji: String, val questions: List<String>)

private fun phaseQuestionsFor(moveIndex: Int): PhaseQuestions = when {
    moveIndex <= 15 -> PhaseQuestions(
        phase = "Opening",
        emoji = "📖",
        questions = listOf(
            "Are all your minor pieces developed toward active squares?",
            "Is your king safe — have you castled, or can you castle soon?",
            "Do you control or challenge the center?",
            "What was the concrete idea behind this move?",
        ),
    )
    moveIndex <= 35 -> PhaseQuestions(
        phase = "Middlegame",
        emoji = "⚔️",
        questions = listOf(
            "What is your opponent threatening after this move?",
            "Which of your pieces is least active — and how would you improve it?",
            "Is there a forcing move available: check, capture, or threat?",
            "What is your plan for the next 2–3 moves?",
        ),
    )
    else -> PhaseQuestions(
        phase = "Endgame",
        emoji = "♟️",
        questions = listOf(
            "Should your king be activated and pushed toward the center?",
            "Are there any passed pawns, and who controls them?",
            "What is the concrete plan to convert or hold this position?",
        ),
    )
}

/**
 * Compact card shown in Navigate mode when Structured Analysis Prompts are enabled
 * and the current position has a [GameHighlight].
 *
 * Shows phase-appropriate analysis questions and a one-line note from the highlight.
 * The user dismisses it with "Continue reviewing" — the card does not reappear for
 * this position during the current session.
 */
@Composable
fun PositionCoachCard(
    moveIndex: Int,
    highlight: GameHighlight,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pq = phaseQuestionsFor(moveIndex)

    AnimatedVisibility(
        visible = true,
        enter   = fadeIn() + expandVertically(),
        exit    = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        Card(
            shape  = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // ── Header ─────────────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = pq.emoji, fontSize = 20.sp)
                    Column {
                        Text(
                            text       = "Take a moment — ${pq.phase}",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color      = ChessGold,
                        )
                        Text(
                            text  = "${highlight.title}: ${highlight.description}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CardText.copy(alpha = 0.65f),
                        )
                    }
                }

                HorizontalDivider(color = CardBorder.copy(alpha = 0.5f))

                // ── Questions ──────────────────────────────────────────────────
                Text(
                    text       = "Before you continue, consider:",
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = CardText.copy(alpha = 0.7f),
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    pq.questions.forEachIndexed { i, q ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                text       = "${i + 1}.",
                                style      = MaterialTheme.typography.bodySmall,
                                color      = ChessGold,
                                fontWeight = FontWeight.Bold,
                                modifier   = Modifier.width(16.dp),
                            )
                            Text(
                                text  = q,
                                style = MaterialTheme.typography.bodySmall,
                                color = CardText,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                // ── Dismiss ────────────────────────────────────────────────────
                Button(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = CardBorder),
                ) {
                    Text(
                        text       = "Continue reviewing",
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White,
                    )
                }
            }
        }
    }
}
