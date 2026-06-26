package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** One spoke of a [RadarChart]: a label and a 0–1 normalised value. */
data class RadarAxis(val label: String, val value: Float)

/**
 * Generic spider/radar chart — the reusable core extracted from `RadarChartCard`. Draws concentric
 * grid rings, labelled spokes and a filled value polygon for arbitrary [axes].
 */
@Composable
fun RadarChart(
    axes: List<RadarAxis>,
    modifier: Modifier = Modifier,
    fillColor: Color = ChessGold,
) {
    val appColors = LocalAppColors.current
    val textMeasurer = rememberTextMeasurer()
    val gridColor = appColors.textTertiary.copy(alpha = 0.3f)
    val labelStyle = TextStyle(color = appColors.textSecondary, fontSize = 9.sp)

    Box(modifier = modifier.fillMaxWidth().padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) / 2f * 0.74f
            val n = axes.size
            if (n == 0) return@Canvas

            fun pointAt(index: Int, frac: Float): Offset {
                val angle = (-90.0 + index * (360.0 / n)) * (Math.PI / 180.0)
                return Offset(
                    x = center.x + (radius * frac * cos(angle)).toFloat(),
                    y = center.y + (radius * frac * sin(angle)).toFloat(),
                )
            }

            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { ring ->
                val ringPath = Path()
                for (i in 0 until n) {
                    val p = pointAt(i, ring)
                    if (i == 0) ringPath.moveTo(p.x, p.y) else ringPath.lineTo(p.x, p.y)
                }
                ringPath.close()
                drawPath(ringPath, color = gridColor, style = Stroke(width = 1f))
            }

            for (i in 0 until n) {
                drawLine(gridColor, center, pointAt(i, 1f), strokeWidth = 1f)
                val measured = textMeasurer.measure(axes[i].label, labelStyle)
                val labelPoint = pointAt(i, 1.18f)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = labelPoint.x - measured.size.width / 2f,
                        y = labelPoint.y - measured.size.height / 2f,
                    ),
                )
            }

            val valuePath = Path()
            for (i in 0 until n) {
                val frac = axes[i].value.coerceIn(0f, 1f)
                val p = pointAt(i, frac)
                if (i == 0) valuePath.moveTo(p.x, p.y) else valuePath.lineTo(p.x, p.y)
            }
            valuePath.close()
            drawPath(valuePath, color = fillColor.copy(alpha = 0.25f))
            drawPath(valuePath, color = fillColor, style = Stroke(width = 2.5f))
        }
    }
}
