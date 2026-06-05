package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.SelfAwarenessTrendPoint
import com.acepero13.chess.core.ui.theme.ChessGold

/**
 * Sparkline card showing self-awareness score per game over time.
 *
 * Self-awareness = how many of the engine's critical positions the player
 * independently flagged before seeing feedback.
 */
@Composable
fun SelfAwarenessTrendCard(
    points:   List<SelfAwarenessTrendPoint>,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return

    val avgScore    = points.map { it.score }.average().toFloat()
    val isImproving = run {
        if (points.size < 4) false
        else {
            val mid    = points.size / 2
            val first  = points.subList(0, mid).map { it.score }.average()
            val second = points.subList(mid, points.size).map { it.score }.average()
            second > first + 0.05
        }
    }
    val trendLabel = when {
        points.size < 2   -> ""
        isImproving        -> "Improving"
        else               -> "Stable"
    }
    val trendColor = if (isImproving) Color(0xFF4CAF50) else ChessGold

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
                        text       = "Self-Awareness",
                        style      = MaterialTheme.typography.labelMedium,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text       = "${(avgScore * 100).toInt()}% avg",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = ChessGold,
                    )
                }
                if (trendLabel.isNotEmpty()) {
                    Text(
                        text  = trendLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = trendColor,
                    )
                }
            }

            Text(
                text  = "% of critical positions you flagged independently, per game",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )

            Spacer(Modifier.height(12.dp))

            if (points.size >= 2) {
                SparkLine(
                    values   = points.map { it.score },
                    color    = ChessGold,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text  = "Game 1",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                    Text(
                        text  = "Game ${points.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            } else {
                Text(
                    text  = "${(points.first().score * 100).toInt()}% — play more games to show trend",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SparkLine(
    values:   List<Float>,
    color:    Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val w     = size.width
        val h     = size.height
        val min   = values.min()
        val max   = values.max().coerceAtLeast(min + 0.01f)
        val stepX = w / (values.size - 1)

        fun y(v: Float) = h - ((v - min) / (max - min)) * h * 0.85f - h * 0.075f
        fun x(i: Int)   = i * stepX

        // Area fill
        val area = Path().apply {
            moveTo(x(0), h)
            lineTo(x(0), y(values[0]))
            for (i in 1 until values.size) lineTo(x(i), y(values[i]))
            lineTo(x(values.size - 1), h)
            close()
        }
        drawPath(area, color.copy(alpha = 0.15f))

        // Line
        val line = Path().apply {
            moveTo(x(0), y(values[0]))
            for (i in 1 until values.size) lineTo(x(i), y(values[i]))
        }
        drawPath(line, color, style = Stroke(width = 2.dp.toPx()))

        // Dots
        for (i in values.indices) {
            drawCircle(color, radius = 3.dp.toPx(), center = Offset(x(i), y(values[i])))
        }
    }
}
