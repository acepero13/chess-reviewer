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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.BehavioralDiagnostic
import com.acepero13.android.gamereviewer.domain.ColorAsymmetry
import com.acepero13.chess.core.ui.theme.ChessGold

/**
 * Two-column card comparing the player's top failure patterns as White vs. Black.
 *
 * Many players have asymmetric profiles: over-aggressive as White, over-passive as
 * Black, or vice versa. Surfacing this asymmetry gives targeted coaching value.
 */
@Composable
fun ColorAsymmetryCard(
    asymmetry: ColorAsymmetry,
    modifier:  Modifier = Modifier,
) {
    if (!asymmetry.hasData) return

    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape    = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text       = "Color Asymmetry",
                style      = MaterialTheme.typography.labelMedium,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text       = "Your failure patterns differ by side",
                style      = MaterialTheme.typography.bodySmall,
                color      = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ColorColumn(
                    label    = "As White",
                    total    = asymmetry.totalAsWhite,
                    trends   = asymmetry.asWhite,
                    modifier = Modifier.weight(1f),
                )
                ColorColumn(
                    label    = "As Black",
                    total    = asymmetry.totalAsBlack,
                    trends   = asymmetry.asBlack,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ColorColumn(
    label:    String,
    total:    Int,
    trends:   List<BehavioralDiagnostic.FailureTrend>,
    modifier: Modifier = Modifier,
) {
    Card(
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape    = RoundedCornerShape(8.dp),
        modifier = modifier,
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text       = label,
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color      = ChessGold,
            )
            Text(
                text  = "$total mistakes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )

            if (trends.isEmpty()) {
                Text(
                    text  = "No pattern yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            } else {
                trends.take(2).forEachIndexed { i, trend ->
                    if (i > 0) HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Row {
                        Text(trend.emoji, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text  = trend.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
