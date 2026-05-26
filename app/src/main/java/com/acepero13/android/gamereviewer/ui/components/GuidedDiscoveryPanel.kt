package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiObjects
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.InsightReconciler
import com.acepero13.chess.core.ui.theme.ChessGold

/**
 * **Guided Discovery Panel — Task 3.3**
 *
 * Replaces the [MoveTree] and move-comment area when guided discovery mode is active.
 * Navigation is frozen externally (the ViewModel blocks [goToMove] calls).
 *
 * Flow:
 * 1. Targeted questions appear immediately, based on [InsightReconciler.Insight.questions].
 * 2. User can write free-text analysis in the thoughts field.
 * 3. "Get a Hint" → reveals [InsightReconciler.Insight.conceptualHint] (no engine line).
 * 4. "Show Answer" → engine arrow appears on the board + eval shown (REVEALED state).
 * 5. "I found it!" / "Submit" → saves thoughts, exits guided mode.
 */
@Composable
fun GuidedDiscoveryPanel(
    insight: InsightReconciler.Insight,
    thoughts: String,
    hintVisible: Boolean,
    answerRevealed: Boolean,
    engineThinking: Boolean,
    revealedEvalCp: Int?,
    onThoughtsChange: (String) -> Unit,
    onRevealHint: () -> Unit,
    onRevealAnswer: () -> Unit,
    onSubmit: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {

        // ── Header card ────────────────────────────────────────────────────────
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            shape  = RoundedCornerShape(12.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = insight.emoji,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text       = insight.title,
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text  = insight.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                        )
                    }
                }
            }
        }

        // ── Guiding questions ──────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text       = "Think about these questions:",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color      = ChessGold,
            )
            insight.questions.forEachIndexed { i, q ->
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text  = "${i + 1}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(20.dp),
                    )
                    Text(
                        text  = q,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // ── Thoughts input ─────────────────────────────────────────────────────
        OutlinedTextField(
            value         = thoughts,
            onValueChange = onThoughtsChange,
            label         = { Text("Your analysis (optional)") },
            placeholder   = {
                Text(
                    "Write what you see in this position…",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 5,
        )

        // ── Conceptual hint (revealed on demand) ───────────────────────────────
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

        // ── Engine reveal info (shown after answer is revealed) ────────────────
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

        // ── Action buttons ─────────────────────────────────────────────────────
        when {
            answerRevealed -> {
                // Answer shown — user can submit thoughts and exit
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
            engineThinking -> {
                Button(
                    onClick  = {},
                    enabled  = false,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Loading engine answer…")
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
    }
}
