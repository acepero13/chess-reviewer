package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acepero13.android.gamereviewer.domain.GameClassCounts
import com.acepero13.chess.core.ui.theme.LocalAppColors

private val SpotlessColor = Color(0xFF66BB6A)
private val CleanColor    = Color(0xFFAED581)
private val FlawedColor   = Color(0xFFFFB74D)
private val ThrowingColor = Color(0xFFE57373)

/** Donut of game quality (Spotless / Clean / Flawed / Game-throwing) with the clean-rate % at the center. */
@Composable
fun CleanRateDonutCard(
    counts: GameClassCounts,
    cleanRatePct: Int,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    if (counts.total == 0) return

    val segments = listOf(
        "Spotless" to (counts.spotless to SpotlessColor),
        "Clean" to (counts.clean to CleanColor),
        "Flawed" to (counts.flawed to FlawedColor),
        "Game-throwing" to (counts.gameThrowing to ThrowingColor),
    )

    InsightCard(title = "Clean rate", modifier = modifier) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    val stroke = 16.dp.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val topLeft = Offset(inset, inset)
                    var start = -90f
                    segments.forEach { (_, valueColor) ->
                        val (count, color) = valueColor
                        val sweep = 360f * count / counts.total
                        if (sweep > 0f) {
                            drawArc(
                                color       = color,
                                startAngle  = start,
                                sweepAngle  = sweep,
                                useCenter   = false,
                                topLeft     = topLeft,
                                size        = arcSize,
                                style       = Stroke(width = stroke),
                            )
                            start += sweep
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = "$cleanRatePct%",
                        fontSize   = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color      = appColors.textPrimary,
                    )
                    Text(
                        text  = "clean",
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.textTertiary,
                    )
                }
            }

            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                segments.forEach { (label, valueColor) ->
                    val (count, color) = valueColor
                    val pct = count * 100 / counts.total
                    LegendRow(label = label, count = count, pct = pct, color = color)
                }
            }
        }
    }
}

@Composable
private fun LegendRow(label: String, count: Int, pct: Int, color: Color) {
    val appColors = LocalAppColors.current
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodySmall,
            color    = appColors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text       = "$count ($pct%)",
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color      = color,
        )
    }
}
