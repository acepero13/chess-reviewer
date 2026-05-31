package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.InsightReconciler
import com.acepero13.android.gamereviewer.domain.OpeningDeviation
import com.acepero13.chess.core.ui.theme.ChessGold

/**
 * Opening Theory Coach panel — shown when the user navigates to the exact position where
 * their game left opening theory.
 *
 * Collapsed by default (just the header visible). User can expand to read the questions
 * and optionally reveal the conceptual hint, then continue the review.
 */
@Composable
fun OpeningDeviationPanel(
    deviation: OpeningDeviation,
    insight: InsightReconciler.Insight,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded    by remember { mutableStateOf(false) }
    var hintVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── Main card ───────────────────────────────────────────────────────────
        Card(
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            shape    = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
        ) {
            Column(modifier = Modifier.padding(12.dp)) {

                // Always-visible header row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text  = "📖",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = "You left theory here",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text  = "${deviation.openingEco} · ${deviation.openingName}  →  ${deviation.moveLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                        )
                    }
                    Icon(
                        imageVector        = if (expanded) Icons.Outlined.ExpandLess
                                             else Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint               = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        modifier           = Modifier.size(18.dp),
                    )
                }

                // Expandable body: context + questions
                AnimatedVisibility(
                    visible = expanded,
                    enter   = fadeIn() + expandVertically(),
                    exit    = fadeOut() + shrinkVertically(),
                ) {
                    Column(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text  = "You stayed in ${deviation.openingName} for ${deviation.theoreticalDepth} move${if (deviation.theoreticalDepth != 1) "s" else ""}, then played ${deviation.moveLabel} which is outside the book. Take a moment to reflect on what the opening principles demand here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                        )

                        // Questions
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
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Conceptual hint (expandable) ────────────────────────────────────────
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
                        text       = "Opening Principles",
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

        // ── Action buttons ──────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!hintVisible) {
                OutlinedButton(
                    onClick  = { hintVisible = true; expanded = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.EmojiObjects, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Opening principles")
                }
            }
            Button(
                onClick  = onContinue,
                modifier = Modifier.weight(1f),
                colors   = ButtonDefaults.buttonColors(containerColor = ChessGold),
            ) {
                Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Continue review", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
