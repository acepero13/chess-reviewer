package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.ComposurePoint
import com.acepero13.android.gamereviewer.domain.PhaseBudget
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors

private val OpeningColor = Color(0xFF4FC3F7)
private val MiddlegameColor = Color(0xFFFFB74D)
private val EndgameColor = Color(0xFFEF9A9A)
private val OpponentColor = Color(0xFF9E9E9E)

/** Average move-time per half-move across all games: "You" (gold) vs opponent (dashed). */
@Composable
fun ComposureTimelineChart(
    points: List<ComposurePoint>,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    InsightCard(title = "Composure Timeline", modifier = modifier) {
        if (points.size < 2) {
            Text("Not enough clock data yet.", style = MaterialTheme.typography.bodySmall, color = appColors.textSecondary)
            return@InsightCard
        }
        val maxSec = (points.flatMap { listOf(it.youAvgSec, it.oppAvgSec) }.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val maxPly = points.maxOf { it.moveIndex }.coerceAtLeast(1)
        val grid = appColors.textTertiary.copy(alpha = 0.25f)
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            val padT = 8f; val padB = 8f; val padL = 8f; val padR = 8f
            val w = size.width - padL - padR
            val h = size.height - padT - padB
            fun px(ply: Int) = padL + w * ((ply - 1f) / (maxPly - 1).coerceAtLeast(1))
            fun py(sec: Float) = padT + h * (1f - (sec / maxSec).coerceIn(0f, 1f))

            // Phase dividers (opening ≤20, middlegame ≤60).
            listOf(20, 60).forEach { divider ->
                if (divider < maxPly) {
                    val x = px(divider)
                    drawLine(grid, Offset(x, padT), Offset(x, padT + h), strokeWidth = 1f)
                }
            }

            fun line(values: (ComposurePoint) -> Float, color: Color, dashed: Boolean) {
                val path = Path()
                points.forEachIndexed { i, p ->
                    val o = Offset(px(p.moveIndex), py(values(p)))
                    if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = 3f,
                        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null,
                    ),
                )
            }
            line({ it.oppAvgSec }, OpponentColor, dashed = true)
            line({ it.youAvgSec }, ChessGold, dashed = false)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("● You", style = MaterialTheme.typography.labelSmall, color = ChessGold)
            Text("- - Opponent", style = MaterialTheme.typography.labelSmall, color = OpponentColor)
            Text("Opening · Middlegame · Endgame", style = MaterialTheme.typography.labelSmall, color = appColors.textSecondary)
        }
    }
}

/** Stacked % bars showing how thinking time is split across the three phases, You vs opponent. */
@Composable
fun TimeBudgetByPhaseCard(
    you: PhaseBudget,
    opponent: PhaseBudget,
    modifier: Modifier = Modifier,
) {
    InsightCard(title = "Time Budget by Phase", modifier = modifier) {
        BudgetRow("You", you)
        BudgetRow("Field", opponent)
    }
}

@Composable
private fun BudgetRow(label: String, budget: PhaseBudget) {
    val appColors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = appColors.textSecondary)
        Row(
            modifier = Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(6.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Segment(budget.openingFraction, OpeningColor, budget.openingAvgSec)
            Segment(budget.middlegameFraction, MiddlegameColor, budget.middlegameAvgSec)
            Segment(budget.endgameFraction, EndgameColor, budget.endgameAvgSec)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Segment(fraction: Float, color: Color, avgSec: Float) {
    if (fraction <= 0f) return
    Box(
        modifier = Modifier.weight(fraction).height(28.dp).background(color),
        contentAlignment = Alignment.Center,
    ) {
        val pct = (fraction * 100).toInt()
        if (fraction > 0.12f) {
            Text(
                "$pct% (${"%.1f".format(avgSec)}s)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A1A),
            )
        }
    }
}
