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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.acepero13.android.gamereviewer.domain.MiddlegamePlanClassification
import com.acepero13.chess.core.ui.theme.ChessGold

/**
 * Middlegame Plan Coach panel — shown when the user navigates to the first out-of-book
 * position where structural pawn patterns (IQP, open files, majorities, etc.) are detected.
 *
 * Collapsed by default. Up to 3 plans are shown when expanded, each with targeted
 * guiding questions from InsightReconciler. An optional Plan Guide hint box reveals
 * the top-priority plan's conceptual coaching hint.
 */
@Composable
fun MiddlegamePlanPanel(
    classification: MiddlegamePlanClassification,
    insights: List<InsightReconciler.Insight>,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded    by remember { mutableStateOf(false) }
    var hintVisible by remember { mutableStateOf(false) }

    val planCount = classification.plans.size
    val subtitle  = "$planCount pawn structure${if (planCount == 1) "" else "s"} detected"

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── Main card ────────────────────────────────────────────────────────────
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
                        text  = "♟",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = "Middlegame Plans Detected",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text  = subtitle,
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

                // Expandable body: one section per detected plan
                AnimatedVisibility(
                    visible = expanded,
                    enter   = fadeIn() + expandVertically(),
                    exit    = fadeOut() + shrinkVertically(),
                ) {
                    Column(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        classification.plans.forEachIndexed { planIdx, plan ->
                            val insight = insights.getOrNull(planIdx)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text       = "${insight?.emoji ?: "♟"} ${plan.title}",
                                        style      = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                                Text(
                                    text  = plan.planAdvice,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                                )
                                if (insight != null) {
                                    Spacer(Modifier.height(2.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                }
            }
        }

        // ── Plan Guide hint (top-priority plan's conceptual hint) ─────────────────
        AnimatedVisibility(
            visible = hintVisible,
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically(),
        ) {
            val hint = insights.firstOrNull()?.conceptualHint.orEmpty()
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
                        text       = "Plan Guide",
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text      = hint,
                    style     = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color     = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }

        // ── Action buttons ───────────────────────────────────────────────────────
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
                    Text("Plan guide")
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
