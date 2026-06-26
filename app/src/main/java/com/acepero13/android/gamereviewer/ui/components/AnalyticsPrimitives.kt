package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.acepero13.chess.core.ui.theme.LocalAppColors

/** A single headline metric tile (emoji + big number + caption). */
data class StatTile(val emoji: String, val value: String, val label: String, val tint: Color)

/** A responsive 2-column grid of [StatTile]s — the "GAMES / MOVES" header blocks. */
@Composable
fun StatTileGrid(
    tiles: List<StatTile>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { tile ->
                    TileCard(tile, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TileCard(tile: StatTile, modifier: Modifier = Modifier) {
    val appColors = LocalAppColors.current
    Card(
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.surfaceVariant),
        border = BorderStroke(1.dp, appColors.border),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(tile.emoji, style = MaterialTheme.typography.titleLarge)
            Column {
                Text(
                    text = tile.value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = tile.tint,
                )
                Text(
                    text = tile.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.textSecondary,
                )
            }
        }
    }
}

/** One slice of a [DonutChart]. */
data class DonutSegment(val value: Float, val color: Color, val label: String)

/**
 * Generic donut chart with an optional centred label — the simplification of
 * [CleanRateDonutCard]'s canvas, parameterised by arbitrary [segments].
 */
@Composable
fun DonutChart(
    segments: List<DonutSegment>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 16.dp,
    centerLabel: String? = null,
    centerSub: String? = null,
) {
    val appColors = LocalAppColors.current
    val total = segments.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.0001f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            // Track.
            drawArc(
                color = appColors.border,
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(width = stroke),
            )
            var start = -90f
            segments.forEach { seg ->
                val sweep = seg.value / total * 360f
                drawArc(
                    color = seg.color,
                    startAngle = start, sweepAngle = sweep, useCenter = false,
                    topLeft = topLeft, size = arcSize, style = Stroke(width = stroke),
                )
                start += sweep
            }
        }
        if (centerLabel != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary,
                )
                if (centerSub != null) {
                    Text(centerSub, style = MaterialTheme.typography.labelSmall, color = appColors.textSecondary)
                }
            }
        }
    }
}

/** A single-value progress ring with a centred percentage — used for find-rate / win-rate gauges. */
@Composable
fun RingGauge(
    pct: Int,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    strokeWidth: Dp = 8.dp,
    centerText: String = "$pct%",
) {
    val appColors = LocalAppColors.current
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = color.copy(alpha = 0.18f),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(width = stroke),
            )
            drawArc(
                color = color,
                startAngle = -90f, sweepAngle = pct.coerceIn(0, 100) / 100f * 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(width = stroke),
            )
        }
        Text(
            text = centerText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = appColors.textPrimary,
        )
    }
}
