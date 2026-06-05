package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.ImprovementTrajectory
import com.acepero13.chess.core.ui.theme.ChessGold

/**
 * Card comparing first-half vs second-half game frequency for the player's
 * top weakness. Answers: "Are you actually getting better at this?"
 */
@Composable
fun ImprovementTrajectoryCard(
    trajectory: ImprovementTrajectory,
    modifier:   Modifier = Modifier,
) {
    val trendColor = if (trajectory.isImproving) Color(0xFF4CAF50) else Color(0xFFFF9800)
    val trendLabel = if (trajectory.isImproving) "Improving" else "Not yet improving"
    val trendEmoji = if (trajectory.isImproving) "📈" else "📊"

    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape    = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text  = "Weakness Trajectory",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row {
                        Text(trajectory.emoji, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text  = " ${trajectory.patternTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChessGold,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Text(
                    text  = "$trendEmoji $trendLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = trendColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(12.dp))

            TrajectoryBar(
                label        = "First ${trajectory.firstHalfGames} games",
                count        = trajectory.firstHalfCount,
                total        = trajectory.firstHalfGames,
                barColor     = Color(0xFFE53935).copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(6.dp))
            TrajectoryBar(
                label        = "Recent ${trajectory.secondHalfGames} games",
                count        = trajectory.secondHalfCount,
                total        = trajectory.secondHalfGames,
                barColor     = trendColor,
            )

            Spacer(Modifier.height(8.dp))

            val delta = trajectory.deltaPercent
            val deltaText = when {
                delta < -5  -> "${-delta}% fewer games affected — keep it up"
                delta > 5   -> "${delta}% more games affected — needs focused work"
                else        -> "Pattern frequency is stable across games"
            }
            Text(
                text  = deltaText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun TrajectoryBar(
    label:    String,
    count:    Int,
    total:    Int,
    barColor: Color,
) {
    val fraction = if (total == 0) 0f else count.toFloat() / total
    Column {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text  = "$count / $total  (${(fraction * 100).toInt()}%)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress   = { fraction },
            modifier   = Modifier.fillMaxWidth().height(6.dp),
            color      = barColor,
            trackColor = barColor.copy(alpha = 0.2f),
            strokeCap  = StrokeCap.Round,
        )
    }
}
