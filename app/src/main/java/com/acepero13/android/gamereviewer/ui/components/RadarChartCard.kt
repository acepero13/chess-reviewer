package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acepero13.android.gamereviewer.domain.PlayerProfile
import com.acepero13.android.gamereviewer.domain.ProfileAxis
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val PositiveColor = Color(0xFF81C784)
private val NegativeColor = Color(0xFFE57373)

/**
 * Radar (spider) chart of the [PlayerProfile] axes, with a per-axis list below showing each
 * score and its trend delta vs the recent-games window.
 */
@Composable
fun RadarChartCard(
    profile: PlayerProfile,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    val textMeasurer = rememberTextMeasurer()
    val axes = profile.axes
    val gridColor = appColors.textTertiary.copy(alpha = 0.3f)
    val labelStyle = TextStyle(color = appColors.textSecondary, fontSize = 9.sp)

    Card(
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = appColors.surface),
        border   = BorderStroke(1.dp, appColors.border),
        modifier = modifier,
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text  = "Profile Summary",
                style = MaterialTheme.typography.labelMedium,
                color = appColors.textSecondary,
            )

            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = min(size.width, size.height) / 2f * 0.78f
                    val n = axes.size
                    if (n == 0) return@Canvas

                    fun pointAt(index: Int, frac: Float): Offset {
                        val angle = (-90.0 + index * (360.0 / n)) * (Math.PI / 180.0)
                        return Offset(
                            x = center.x + (radius * frac * cos(angle)).toFloat(),
                            y = center.y + (radius * frac * sin(angle)).toFloat(),
                        )
                    }

                    // Concentric grid rings.
                    listOf(0.25f, 0.5f, 0.75f, 1f).forEach { ring ->
                        val ringPath = Path()
                        for (i in 0 until n) {
                            val p = pointAt(i, ring)
                            if (i == 0) ringPath.moveTo(p.x, p.y) else ringPath.lineTo(p.x, p.y)
                        }
                        ringPath.close()
                        drawPath(ringPath, color = gridColor, style = Stroke(width = 1f))
                    }

                    // Spokes + labels.
                    for (i in 0 until n) {
                        val outer = pointAt(i, 1f)
                        drawLine(gridColor, center, outer, strokeWidth = 1f)

                        val labelText = axes[i].name
                        val measured = textMeasurer.measure(labelText, labelStyle)
                        val labelPoint = pointAt(i, 1.16f)
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(
                                x = labelPoint.x - measured.size.width / 2f,
                                y = labelPoint.y - measured.size.height / 2f,
                            ),
                        )
                    }

                    // Value polygon.
                    val valuePath = Path()
                    for (i in 0 until n) {
                        val frac = (axes[i].value / 100f).coerceIn(0f, 1f)
                        val p = pointAt(i, frac)
                        if (i == 0) valuePath.moveTo(p.x, p.y) else valuePath.lineTo(p.x, p.y)
                    }
                    valuePath.close()
                    drawPath(valuePath, color = ChessGold.copy(alpha = 0.25f))
                    drawPath(valuePath, color = ChessGold, style = Stroke(width = 2.5f))
                }
            }

            // Per-axis values + deltas.
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                axes.forEach { AxisRow(it) }
            }
        }
    }
}

@Composable
private fun AxisRow(axis: ProfileAxis) {
    val appColors = LocalAppColors.current
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text     = axis.name.replaceFirstChar { it.uppercase() },
            style    = MaterialTheme.typography.bodySmall,
            color    = appColors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text       = "${axis.value}",
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color      = appColors.textPrimary,
            modifier   = Modifier.width(36.dp),
        )
        val delta = axis.delta
        Text(
            text  = if (delta == 0f) "" else (if (delta > 0) "+%.1f".format(delta) else "%.1f".format(delta)),
            style = MaterialTheme.typography.labelSmall,
            color = if (delta >= 0f) PositiveColor else NegativeColor,
            modifier = Modifier.width(44.dp),
        )
    }
}
