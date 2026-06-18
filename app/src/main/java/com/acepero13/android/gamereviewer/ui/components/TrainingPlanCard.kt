package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.TrainingRecommendation
import com.acepero13.chess.core.ui.theme.ChessGold

@Composable
fun TrainingPlanCard(
    recommendation: TrainingRecommendation,
    modifier:       Modifier = Modifier,
) {
    val accentColor = if (recommendation.isHighlighted) ChessGold
                      else MaterialTheme.colorScheme.primary

    Card(
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (recommendation.isHighlighted) accentColor.copy(alpha = 0.08f)
                             else MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (recommendation.isHighlighted) 3.dp else 1.dp,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            TitleRow(recommendation, accentColor = if (recommendation.isHighlighted) ChessGold else null)

            Spacer(Modifier.height(6.dp))

            Text(
                text  = recommendation.category.shortDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            FooterRow(recommendation, accentColor)
        }
    }
}

@Composable
private fun TitleRow(
    recommendation: TrainingRecommendation,
    accentColor: androidx.compose.ui.graphics.Color?,
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier              = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(recommendation.category.emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Text(
                text       = recommendation.category.displayName,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (accentColor != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = accentColor.copy(alpha = 0.15f),
            ) {
                Text(
                    text     = "Recommended",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = accentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun FooterRow(
    recommendation: TrainingRecommendation,
    accentColor: androidx.compose.ui.graphics.Color,
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier              = Modifier.fillMaxWidth(),
    ) {
        Text(
            text     = recommendation.reason,
            style    = MaterialTheme.typography.labelSmall,
            color    = accentColor.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f),
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Text(
                text     = "Coming soon",
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}
