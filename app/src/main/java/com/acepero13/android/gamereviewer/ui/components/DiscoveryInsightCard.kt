package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.InsightReconciler
import com.acepero13.chess.core.ui.theme.ChessGold

@Composable
private fun InsightQuestionsSection(questions: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Think about these questions:", style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold, color = ChessGold)
        questions.forEachIndexed { i, q ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text("${i + 1}.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.width(18.dp))
                Text(q, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 3,
                    modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun InsightHeaderCard(
    insight: InsightReconciler.Insight, insightRevealed: Boolean,
    expanded: Boolean, onToggleExpanded: () -> Unit,
) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape    = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
            .then(if (insightRevealed) Modifier.clickable(onClick = onToggleExpanded) else Modifier),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(if (insightRevealed) insight.emoji else "🎯", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = if (insightRevealed) insight.title else "Find the best move",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier   = Modifier.weight(1f),
                )
                if (insightRevealed) {
                    Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp))
                }
            }
            AnimatedVisibility(insightRevealed && expanded,
                enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (insight.description.isNotBlank())
                        Text(insight.description, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                    if (insight.questions.isNotEmpty())
                        InsightQuestionsSection(insight.questions)
                }
            }
        }
    }
}
