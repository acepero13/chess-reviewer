package com.acepero13.android.gamereviewer.ui.components

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
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.InsightReconciler
import com.acepero13.android.gamereviewer.domain.MiddlegamePlanClassification
import com.acepero13.chess.core.ui.theme.ChessGold

/**
 * Middlegame Plan Coach panel. Compact header always visible; per-plan questions
 * and the plan guide hint open in a dialog on tap.
 */
@Composable
fun MiddlegamePlanPanel(
    classification: MiddlegamePlanClassification,
    insights: List<InsightReconciler.Insight>,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val planCount = classification.plans.size
    val subtitle  = "$planCount pawn structure${if (planCount == 1) "" else "s"} detected"

    var showDialog by remember { mutableStateOf(false) }

    // ── Full-content dialog ────────────────────────────────────────────────────
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("♟")
                    Text(
                        text       = "Middlegame Plans",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            text = {
                Column(
                    modifier            = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text  = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    classification.plans.forEachIndexed { planIdx, plan ->
                        val insight = insights.getOrNull(planIdx)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text       = "${insight?.emoji ?: "♟"} ${plan.title}",
                                style      = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text  = plan.planAdvice,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (insight != null) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text       = "Consider:",
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
                                            text       = "${i + 1}.",
                                            style      = MaterialTheme.typography.bodySmall,
                                            color      = ChessGold,
                                            fontWeight = FontWeight.Bold,
                                            modifier   = Modifier.width(18.dp),
                                        )
                                        Text(
                                            text     = q,
                                            style    = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }
                        if (planIdx < classification.plans.lastIndex) {
                            HorizontalDivider()
                        }
                    }

                    // Plan guide hint (top-priority plan)
                    val hint = insights.firstOrNull()?.conceptualHint.orEmpty()
                    if (hint.isNotEmpty()) {
                        HorizontalDivider()
                        Row(
                            verticalAlignment     = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector        = Icons.Outlined.EmojiObjects,
                                contentDescription = null,
                                modifier           = Modifier.size(14.dp).padding(top = 2.dp),
                                tint               = ChessGold.copy(alpha = 0.7f),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text       = "Plan Guide",
                                    style      = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color      = ChessGold.copy(alpha = 0.85f),
                                )
                                Text(
                                    text      = hint,
                                    style     = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDialog = false; onContinue() },
                    colors  = ButtonDefaults.buttonColors(containerColor = ChessGold),
                ) {
                    Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Continue review", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Back")
                }
            },
        )
    }

    // ── Compact card ───────────────────────────────────────────────────────────
    Column(
        modifier            = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape    = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("♟", style = MaterialTheme.typography.titleMedium)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = "Middlegame Plans Detected",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text     = subtitle,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick  = { showDialog = true },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.ExpandMore,
                        contentDescription = "Read coaching questions",
                        tint               = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        modifier           = Modifier.size(18.dp),
                    )
                }
            }
        }

        Button(
            onClick  = onContinue,
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(containerColor = ChessGold),
        ) {
            Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Continue review", color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
