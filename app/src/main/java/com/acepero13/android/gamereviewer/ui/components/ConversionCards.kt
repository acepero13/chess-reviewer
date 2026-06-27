package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.ConversionPoint
import com.acepero13.android.gamereviewer.domain.SideStats
import com.acepero13.chess.core.ui.theme.LocalAppColors

private val AheadColor = Color(0xFF81C784)
private val BehindColor = Color(0xFF64B5F6)
private val WonColor = Color(0xFF81C784)
private val LostColor = Color(0xFFE57373)

/** Side-by-side "When Ahead" / "When Behind" headline panels. */
@Composable
fun WhenAheadBehindCard(
    ahead: SideStats,
    behind: SideStats,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SidePanel(
            title = "When Ahead",
            rateLabel = "Conversion rate",
            rate = ahead.ratePct,
            secondaryValue = ahead.secondaryCount.toString(),
            secondaryLabel = "Throws",
            games = ahead.games,
            accuracy = ahead.accuracy,
            accent = AheadColor,
            modifier = Modifier.weight(1f),
        )
        SidePanel(
            title = "When Behind",
            rateLabel = "Save rate",
            rate = behind.ratePct,
            secondaryValue = behind.secondaryCount.toString(),
            secondaryLabel = "Saves",
            games = behind.games,
            accuracy = behind.accuracy,
            accent = BehindColor,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SidePanel(
    title: String,
    rateLabel: String,
    rate: Int,
    secondaryValue: String,
    secondaryLabel: String,
    games: Int,
    accuracy: Float,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.surface),
        border = BorderStroke(1.dp, appColors.border),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$rate%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                Column {
                    Text(secondaryValue, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
                    Text(secondaryLabel.uppercase(), style = MaterialTheme.typography.labelSmall, color = appColors.textSecondary)
                }
            }
            Text(rateLabel, style = MaterialTheme.typography.labelSmall, color = appColors.textSecondary)
            RingRow(games = games, accuracy = accuracy, accent = accent)
        }
    }
}

@Composable
private fun RingRow(games: Int, accuracy: Float, accent: Color) {
    val appColors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$games games", style = MaterialTheme.typography.labelSmall, color = appColors.textSecondary, modifier = Modifier.weight(1f))
        if (accuracy > 0f) {
            Text("${"%.0f".format(accuracy)}% acc", style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Per-game accuracy split into two outcome columns. Each dot is one game positioned by its accuracy
 * (Y); games are grouped into the good-outcome column ([goodLabel]) and the bad-outcome column
 * ([badLabel]) and jittered horizontally so individual games stay visible. A bold bar marks each
 * group's median accuracy, making the gap between good and bad outcomes the headline.
 *
 * Plotting accuracy by outcome (rather than against peak eval, which barely varies) is what surfaces
 * the actual story: how inaccurate your play was in the games you threw vs the ones you converted.
 */
@Composable
fun ConversionScatterChart(
    title: String,
    points: List<ConversionPoint>,
    goodLabel: String,
    badLabel: String,
    readingHint: String,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    InsightCard(title = title, modifier = modifier) {
        val plotted = points.filter { it.accuracy > 0f }
        if (plotted.isEmpty()) {
            Text("No games in this category yet.", style = MaterialTheme.typography.bodySmall, color = appColors.textSecondary)
            return@InsightCard
        }
        val good = plotted.filter { it.success }
        val bad = plotted.filter { !it.success }
        OutcomeAccuracyPlot(good = good, bad = bad)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
            ColumnLabel("$goodLabel (${good.size})", WonColor, Modifier.weight(1f))
            ColumnLabel("$badLabel (${bad.size})", LostColor, Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        Text("Bars mark each group's median accuracy.", style = MaterialTheme.typography.labelSmall, color = appColors.textSecondary)
        Text(readingHint, style = MaterialTheme.typography.labelSmall, color = appColors.textSecondary)
    }
}

/** Two jittered dot columns (good / bad) over a labelled accuracy axis. */
@Composable
private fun OutcomeAccuracyPlot(good: List<ConversionPoint>, bad: List<ConversionPoint>) {
    val appColors = LocalAppColors.current
    val axisLabel = MaterialTheme.typography.labelSmall
    Row(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        // Accuracy axis labels, top (100%) to bottom (0%).
        Column(
            modifier = Modifier.fillMaxHeight().padding(end = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            listOf(100, 75, 50, 25, 0).forEach { pct ->
                Text("$pct%", style = axisLabel, color = appColors.textSecondary)
            }
        }
        val grid = appColors.textTertiary.copy(alpha = 0.30f)
        val axis = appColors.textTertiary.copy(alpha = 0.55f)
        Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
            val w = size.width
            val h = size.height
            val dashed = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
            listOf(0.25f, 0.5f, 0.75f).forEach { f ->
                val y = h * (1f - f)
                drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1f, pathEffect = dashed)
            }
            drawLine(axis, Offset(0f, h), Offset(w, h), strokeWidth = 2f)
            drawLine(axis, Offset(0f, 0f), Offset(0f, h), strokeWidth = 2f)

            val half = w * 0.16f
            fun yFor(acc: Float) = h * (1f - (acc / 100f).coerceIn(0f, 1f))
            fun drawGroup(pts: List<ConversionPoint>, cx: Float, color: Color) {
                if (pts.isEmpty()) return
                drawRect(color.copy(alpha = 0.06f), topLeft = Offset(cx - half - 8f, 0f), size = Size((half + 8f) * 2f, h))
                pts.forEachIndexed { i, p ->
                    // Deterministic golden-ratio jitter keeps dots stable across recompositions.
                    val frac = ((i * 0.6180339887).rem(1.0)).toFloat()
                    val x = cx + (frac - 0.5f) * 2f * half
                    val center = Offset(x, yFor(p.accuracy))
                    drawCircle(color.copy(alpha = 0.55f), radius = 7f, center = center)
                    drawCircle(color, radius = 7f, center = center, style = Stroke(width = 1.5f))
                }
                val median = pts.map { it.accuracy }.sorted().let { it[it.size / 2] }
                val my = yFor(median)
                drawLine(color, Offset(cx - half - 8f, my), Offset(cx + half + 8f, my), strokeWidth = 4f)
            }
            drawGroup(good, w * 0.30f, WonColor)
            drawGroup(bad, w * 0.70f, LostColor)
        }
    }
}

@Composable
private fun ColumnLabel(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
