package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.OpeningRow
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors

private val WinColor = Color(0xFF81C784)
private val DrawColor = Color(0xFF9E9E9E)
private val LossColor = Color(0xFFE57373)

/** "Your Weapon" hero card — the user's best-performing opening with enough games. */
@Composable
fun TopOpeningCard(
    opening: OpeningRow,
    overallBookDepthPly: Float,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.surface),
        border = BorderStroke(1.dp, ChessGold),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("YOUR WEAPON", style = MaterialTheme.typography.labelSmall, color = ChessGold, fontWeight = FontWeight.Bold)
                Text(opening.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                Text(
                    "${opening.games} games · prep depth ${"%.1f".format(opening.bookDepthPly)} ply",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.textSecondary,
                )
                Text(
                    "Repertoire avg ${"%.1f".format(overallBookDepthPly)} ply",
                    style = MaterialTheme.typography.labelSmall,
                    color = appColors.textSecondary,
                )
            }
            RingGauge(pct = opening.winRatePct, color = ChessGold, size = 76.dp)
        }
    }
}

/** Per-opening breakdown table: name, games, W/D/L bar, precision, book depth. */
@Composable
fun OpeningTable(
    openings: List<OpeningRow>,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    InsightCard(title = "Openings", modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            HeaderCell("Opening", Modifier.weight(2.2f))
            HeaderCell("Gms", Modifier.weight(0.7f))
            HeaderCell("W/D/L", Modifier.weight(1.4f))
            HeaderCell("Prec", Modifier.weight(0.9f))
            HeaderCell("Depth", Modifier.weight(0.9f))
        }
        openings.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(row.name, style = MaterialTheme.typography.bodySmall, color = appColors.textPrimary, modifier = Modifier.weight(2.2f), maxLines = 2)
                Text("${row.games}", style = MaterialTheme.typography.bodySmall, color = appColors.textSecondary, modifier = Modifier.weight(0.7f))
                Box(modifier = Modifier.weight(1.4f).padding(end = 6.dp)) { WdlBar(row.wins, row.draws, row.losses) }
                Text("${"%.0f".format(row.precision)}%", style = MaterialTheme.typography.bodySmall, color = ChessGold, modifier = Modifier.weight(0.9f))
                Text("${"%.1f".format(row.bookDepthPly)}", style = MaterialTheme.typography.bodySmall, color = appColors.textSecondary, modifier = Modifier.weight(0.9f))
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    val appColors = LocalAppColors.current
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = appColors.textSecondary, modifier = modifier)
}

@Composable
private fun WdlBar(wins: Int, draws: Int, losses: Int) {
    val total = (wins + draws + losses).coerceAtLeast(1)
    Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
        if (wins > 0) Box(Modifier.weight(wins.toFloat() / total).fillMaxWidth().height(8.dp).background(WinColor))
        if (draws > 0) Box(Modifier.weight(draws.toFloat() / total).fillMaxWidth().height(8.dp).background(DrawColor))
        if (losses > 0) Box(Modifier.weight(losses.toFloat() / total).fillMaxWidth().height(8.dp).background(LossColor))
    }
}

/** Donut showing how concentrated the repertoire is (top opening share vs the rest). */
@Composable
fun RepertoireConcentrationDonut(
    concentrationPct: Int,
    distinctOpenings: Int,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    InsightCard(title = "Repertoire Concentration", modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.size(110.dp)) {
                DonutChart(
                    segments = listOf(
                        DonutSegment(concentrationPct.toFloat(), ChessGold, "Top opening"),
                        DonutSegment((100 - concentrationPct).toFloat(), appColors.surfaceVariant, "Rest"),
                    ),
                    centerLabel = "$concentrationPct%",
                )
            }
            Text(
                "$concentrationPct% of games come from your most-played opening, across $distinctOpenings distinct openings.",
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textSecondary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
