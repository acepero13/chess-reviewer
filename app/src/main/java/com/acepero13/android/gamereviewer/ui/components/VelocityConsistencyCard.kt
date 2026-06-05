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
import com.acepero13.android.gamereviewer.domain.VelocityConsistency
import com.acepero13.android.gamereviewer.domain.VelocityConsistencyAnalyzer
import com.acepero13.chess.core.ui.theme.ChessGold

/**
 * Card surfacing decision-time variance across all analyzed games.
 *
 * A high erratic-game fraction signals that the player allocates time randomly
 * rather than proportionally to position complexity — a clock-management red flag.
 */
@Composable
fun VelocityConsistencyCard(
    consistency: VelocityConsistency,
    modifier:    Modifier = Modifier,
) {
    val erraticFraction = consistency.erraticGameFraction
    val barColor = when {
        erraticFraction < 0.25f -> Color(0xFF4CAF50)
        erraticFraction < 0.5f  -> Color(0xFFFF9800)
        else                     -> Color(0xFFE53935)
    }
    val verdict = when {
        erraticFraction < 0.25f -> "Consistent pacing"
        erraticFraction < 0.5f  -> "Moderate variance"
        else                     -> "Erratic pacing"
    }

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
                        text  = "Decision Velocity Consistency",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text       = verdict,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = barColor,
                    )
                }
                if (consistency.totalGameCount > 1) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text       = "${consistency.erraticGameCount} / ${consistency.totalGameCount}",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = ChessGold,
                        )
                        Text(
                            text  = "erratic games",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Plain-language explanation of what the metric means
            Text(
                text  = "Measures how evenly you spread your thinking time across moves. " +
                        "A low spread (±5–15 s) means you pace yourself well. " +
                        "A high spread (±30 s+) means some moves got 1 second and others several minutes — " +
                        "a sign of panic, autopilot, or poor clock management.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress   = { erraticFraction },
                modifier   = Modifier.fillMaxWidth().height(8.dp),
                color      = barColor,
                trackColor = barColor.copy(alpha = 0.2f),
                strokeCap  = StrokeCap.Round,
            )

            Spacer(Modifier.height(8.dp))

            if (consistency.totalGameCount > 1) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    StatPill(
                        label       = "Typical spread",
                        value       = "±${consistency.avgStdDevSeconds.toInt()}s",
                        explanation = stdDevExplanation(consistency.avgStdDevSeconds),
                        valueColor  = stdDevColor(consistency.avgStdDevSeconds),
                        modifier    = Modifier.weight(1f),
                    )
                    StatPill(
                        label       = "Best game",
                        value       = "±${consistency.mostConsistentStdDev.toInt()}s",
                        explanation = stdDevExplanation(consistency.mostConsistentStdDev),
                        valueColor  = stdDevColor(consistency.mostConsistentStdDev),
                        modifier    = Modifier.weight(1f),
                    )
                    StatPill(
                        label       = "Worst game",
                        value       = "±${consistency.mostErraticStdDev.toInt()}s",
                        explanation = stdDevExplanation(consistency.mostErraticStdDev),
                        valueColor  = stdDevColor(consistency.mostErraticStdDev),
                        modifier    = Modifier.weight(1f),
                    )
                }
            } else {
                // Single-game view: "best/worst" would repeat the same number, show one pill only
                StatPill(
                    label       = "Time spread",
                    value       = "±${consistency.avgStdDevSeconds.toInt()}s",
                    explanation = stdDevExplanation(consistency.avgStdDevSeconds),
                    valueColor  = stdDevColor(consistency.avgStdDevSeconds),
                )
            }

            Spacer(Modifier.height(8.dp))

            val tip = when {
                erraticFraction >= 0.5f ->
                    "High time variance often precedes time-pressure blunders. Try to match thinking time to position complexity."
                erraticFraction >= 0.25f ->
                    "Some games show high time swings. Consider a consistent move-timing target (e.g., ≤ 2 min per non-critical move)."
                else ->
                    "Your time allocation is stable — a strong foundation for avoiding clock disasters."
            }
            Text(
                text  = tip,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
        }
    }
}

private fun stdDevExplanation(stdDev: Float): String = when {
    stdDev < 10f  -> "Very even — almost the same time every move"
    stdDev < 20f  -> "Good — small variation, normal pacing"
    stdDev < VelocityConsistencyAnalyzer.ERRATIC_THRESHOLD_SECONDS -> "Moderate — noticeable swings between moves"
    stdDev < 60f  -> "High — big gaps between fast and slow moves"
    else          -> "Very high — some moves blitzed, others spent minutes"
}

private fun stdDevColor(stdDev: Float): Color = when {
    stdDev < 20f  -> Color(0xFF4CAF50)
    stdDev < VelocityConsistencyAnalyzer.ERRATIC_THRESHOLD_SECONDS -> Color(0xFFFF9800)
    else          -> Color(0xFFE53935)
}

@Composable
private fun StatPill(
    label:       String,
    value:       String,
    explanation: String,
    valueColor:  Color,
    modifier:    Modifier = Modifier,
) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape    = RoundedCornerShape(8.dp),
        modifier = modifier,
    ) {
        Column(
            modifier            = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text       = value,
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color      = valueColor,
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = explanation,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            )
        }
    }
}
