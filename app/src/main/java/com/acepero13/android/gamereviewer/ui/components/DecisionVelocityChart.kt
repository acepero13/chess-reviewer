package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acepero13.android.gamereviewer.domain.TimeAnalyzer

/**
 * Decision Velocity chart (Task 4.2).
 *
 * Two overlaid visualisations on a single scrollable canvas:
 *
 * **Area chart** (white translucent):
 *   Eval delta per half-move (centipawn loss, clamped to 600 cp) on Y-axis.
 *   Big spikes = bad moves.
 *
 * **Time dots**:
 *   Each half-move is a circle whose colour encodes [TimeAnalyzer.DecisionType].
 */
@Composable
fun DecisionVelocityChart(
    decisions: List<TimeAnalyzer.MoveDecision>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 200.dp,
    onMoveClick: ((moveIndex: Int) -> Unit)? = null,
) {
    if (decisions.isEmpty()) {
        Text(
            text  = "No time data available for this game.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(8.dp),
        )
        return
    }

    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier) {
        Text(
            text     = "Decision Velocity",
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        Text(
            text     = "Y-axis: centipawn loss per move (higher = worse). Dot colour shows time vs. quality.",
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )

        val colWidth   = 28.dp
        val totalWidth = colWidth * decisions.size + 52.dp  // + left axis area

        val density     = LocalDensity.current
        val colWidthPx  = with(density) { colWidth.toPx() }
        val padLeftPx   = with(density) { 44.dp.toPx() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .horizontalScroll(rememberScrollState()),
        ) {
            Canvas(
                modifier = Modifier
                    .width(totalWidth)
                    .height(chartHeight)
                    .then(
                        if (onMoveClick != null) Modifier.pointerInput(decisions) {
                            detectTapGestures { offset ->
                                val closestIdx = decisions.indices.minByOrNull { i ->
                                    val nodeX = padLeftPx + i * colWidthPx + colWidthPx / 2f
                                    kotlin.math.abs(offset.x - nodeX)
                                }
                                if (closestIdx != null) {
                                    val nodeX = padLeftPx + closestIdx * colWidthPx + colWidthPx / 2f
                                    if (kotlin.math.abs(offset.x - nodeX) <= colWidthPx) {
                                        onMoveClick(decisions[closestIdx].moveIndex)
                                    }
                                }
                            }
                        } else Modifier
                    ),
            ) {
                val padLeft   = 44.dp.toPx()   // wider to fit Y-axis labels
                val padTop    = 12.dp.toPx()
                val padBottom = 28.dp.toPx()
                val drawH     = size.height - padTop - padBottom
                val colW      = colWidth.toPx()
                val maxCp     = 600f

                // ── Y-axis grid lines at 0, 200, 400, 600 cp ────────────────
                val gridLevels = listOf(0f, 200f, 400f, 600f)
                val gridLabels = listOf("0", "200", "400", "600+")

                gridLevels.forEachIndexed { idx, cp ->
                    val fraction = cp / maxCp
                    val y = padTop + drawH - fraction * drawH

                    // faint horizontal rule
                    drawLine(
                        color       = Color.White.copy(alpha = if (cp == 0f) 0.20f else 0.08f),
                        start       = Offset(padLeft, y),
                        end         = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )

                    // Y-axis label
                    val label    = gridLabels[idx]
                    val measured = textMeasurer.measure(label, TextStyle(fontSize = 8.sp))
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            x = padLeft - measured.size.width - 4.dp.toPx(),
                            y = y - measured.size.height / 2f,
                        ),
                        color = Color.White.copy(alpha = 0.55f),
                    )
                }

                // ── Y-axis title drawn vertically ────────────────────────────
                // We approximate this by drawing "cp" at the top-left corner
                val axisTitleMeasured = textMeasurer.measure("cp", TextStyle(fontSize = 7.sp))
                drawText(
                    textLayoutResult = axisTitleMeasured,
                    topLeft = Offset(x = 2.dp.toPx(), y = padTop),
                    color   = Color.White.copy(alpha = 0.4f),
                )

                // ── Eval area fill ───────────────────────────────────────────
                val evalPath = Path()
                evalPath.moveTo(padLeft, padTop + drawH)

                decisions.forEachIndexed { i, d ->
                    val x   = padLeft + i * colW + colW / 2f
                    val cpN = (d.evalDeltaCp.coerceIn(0, maxCp.toInt()).toFloat() / maxCp)
                    val y   = padTop + drawH - cpN * drawH
                    if (i == 0) evalPath.lineTo(x, y) else evalPath.lineTo(x, y)
                }
                val lastX = padLeft + (decisions.size - 1) * colW + colW / 2f
                evalPath.lineTo(lastX, padTop + drawH)
                evalPath.close()
                drawPath(path = evalPath, color = Color.White.copy(alpha = 0.12f))

                // ── Eval area outline ────────────────────────────────────────
                val linePath = Path()
                decisions.forEachIndexed { i, d ->
                    val x   = padLeft + i * colW + colW / 2f
                    val cpN = (d.evalDeltaCp.coerceIn(0, maxCp.toInt()).toFloat() / maxCp)
                    val y   = padTop + drawH - cpN * drawH
                    if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                }
                drawPath(
                    path        = linePath,
                    color       = Color.White.copy(alpha = 0.35f),
                    style       = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
                )

                // ── Move dots ────────────────────────────────────────────────
                decisions.forEachIndexed { i, d ->
                    val x        = padLeft + i * colW + colW / 2f
                    val cpN      = (d.evalDeltaCp.coerceIn(0, maxCp.toInt()).toFloat() / maxCp)
                    val y        = padTop + drawH - cpN * drawH
                    val dotColor = decisionColor(d.decisionType)

                    drawCircle(
                        color  = dotColor.copy(alpha = 0.9f),
                        radius = 6.dp.toPx(),
                        center = Offset(x, y),
                    )
                    if (d.isBlunder) {
                        drawCircle(
                            color  = Color.Red.copy(alpha = 0.5f),
                            radius = 9.dp.toPx(),
                            center = Offset(x, y),
                            style  = Stroke(width = 1.5.dp.toPx()),
                        )
                    }

                    // Move-number label on white moves only (even half-move index)
                    if (i % 2 == 0) {
                        val label    = "${i / 2 + 1}."
                        val measured = textMeasurer.measure(label, TextStyle(fontSize = 8.sp))
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(
                                x = x - measured.size.width / 2f,
                                y = size.height - padBottom + 4.dp.toPx(),
                            ),
                            color = Color.White.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }

        // ── Legend ────────────────────────────────────────────────────────
        Spacer(Modifier.height(8.dp))
        ChartLegend()
        if (onMoveClick != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "Tap a dot to open that position",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun ChartLegend() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendItem(color = decisionColor(TimeAnalyzer.DecisionType.RUSHED_BLUNDER),  label = "Rushed blunder")
            LegendItem(color = decisionColor(TimeAnalyzer.DecisionType.RUSHED_OK),       label = "Quick & ok")
            LegendItem(color = decisionColor(TimeAnalyzer.DecisionType.CAREFUL_BLUNDER), label = "Slow blunder")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendItem(color = decisionColor(TimeAnalyzer.DecisionType.CAREFUL_OK), label = "Careful & ok")
            LegendItem(color = decisionColor(TimeAnalyzer.DecisionType.NORMAL),     label = "Normal")
            LegendItem(color = Color.Red.copy(alpha = 0.5f), label = "Blunder ring", isRing = true)
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, isRing: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(
            modifier = Modifier
                .width(12.dp)
                .height(12.dp),
        ) {
            val r = size.minDimension / 2f
            if (isRing) {
                drawCircle(color = color, radius = r, style = Stroke(width = 1.5.dp.toPx()))
            } else {
                drawCircle(color = color, radius = r)
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun decisionColor(type: TimeAnalyzer.DecisionType): Color = when (type) {
    TimeAnalyzer.DecisionType.RUSHED_BLUNDER  -> Color(0xFFE53935)  // red
    TimeAnalyzer.DecisionType.RUSHED_OK       -> Color(0xFFFF9800)  // orange
    TimeAnalyzer.DecisionType.CAREFUL_BLUNDER -> Color(0xFF9C27B0)  // purple
    TimeAnalyzer.DecisionType.CAREFUL_OK      -> Color(0xFF26A69A)  // teal
    TimeAnalyzer.DecisionType.NORMAL          -> Color(0xFF90A4AE)  // blue-grey
}
