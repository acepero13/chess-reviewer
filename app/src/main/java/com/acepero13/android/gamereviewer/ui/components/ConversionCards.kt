package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.ConversionPoint
import com.acepero13.android.gamereviewer.domain.SideStats
import com.acepero13.chess.core.ui.theme.LocalAppColors
import kotlin.math.abs

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
 * Scatter of one dot per game: x = magnitude of the peak eval reached (cp), y = accuracy in that
 * phase. Dots are coloured by whether the game was ultimately won or lost.
 */
@Composable
fun ConversionScatterChart(
    title: String,
    points: List<ConversionPoint>,
    axisMaxCp: Int,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    InsightCard(title = title, modifier = modifier) {
        if (points.isEmpty()) {
            Text("No games in this category yet.", style = MaterialTheme.typography.bodySmall, color = appColors.textSecondary)
            return@InsightCard
        }
        val grid = appColors.textTertiary.copy(alpha = 0.25f)
        Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val padL = 8f; val padR = 8f; val padT = 8f; val padB = 8f
            val w = size.width - padL - padR
            val h = size.height - padT - padB
            // Grid lines (accuracy bands at 25/50/75%).
            listOf(0.25f, 0.5f, 0.75f).forEach { f ->
                val y = padT + h * (1f - f)
                drawLine(grid, Offset(padL, y), Offset(padL + w, y), strokeWidth = 1f)
            }
            val maxCp = axisMaxCp.coerceAtLeast(1)
            points.forEach { p ->
                val x = padL + w * (abs(p.peakCp).toFloat() / maxCp).coerceIn(0f, 1f)
                val y = padT + h * (1f - (p.accuracy / 100f).coerceIn(0f, 1f))
                drawCircle(
                    color = if (p.won) WonColor else LostColor,
                    radius = 6f,
                    center = Offset(x, y),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot("Won", WonColor)
            LegendDot("Lost", LostColor)
            Text("x: peak eval · y: accuracy", style = MaterialTheme.typography.labelSmall, color = appColors.textSecondary)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Text("● $label", style = MaterialTheme.typography.labelSmall, color = color)
}
