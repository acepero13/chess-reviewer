package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.BehavioralDiagnostic

/**
 * Displays one [BehavioralDiagnostic.FailureTrend] as a ranked profile card for
 * the Dashboard's "Top Failure Trends" section (Task 4.3).
 *
 * Layout:
 * ```
 * ┌────────────────────────────────────────────┐
 * │  [#1]  🎯  Tactical Blindspot              │
 * │       "You consistently miss forcing…"      │
 * │       Occurred 7× across 3 games           │
 * └────────────────────────────────────────────┘
 * ```
 */
@Composable
fun BehavioralProfileCard(
    trend: BehavioralDiagnostic.FailureTrend,
    modifier: Modifier = Modifier,
) {
    val rankColor = when (trend.rank) {
        1 -> Color(0xFFFFD700)  // gold
        2 -> Color(0xFFB0BEC5)  // silver
        else -> Color(0xFFBF8970) // bronze
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Rank badge
            Surface(
                shape = CircleShape,
                color = rankColor.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp),
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Text(
                        text       = "#${trend.rank}",
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color      = rankColor,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Emoji + title row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = trend.emoji,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text       = trend.title,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Coaching description
                Text(
                    text  = trend.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(8.dp))

                // Frequency pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = rankColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        text     = "Occurred ${trend.totalCount}× across ${trend.frequency} " +
                            "${if (trend.frequency == 1) "game" else "games"}",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = rankColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}
