package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.MotifFindRate
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors

private val StrongColor = Color(0xFF81C784)
private val WeakColor = Color(0xFFE57373)

/** Find-rate radar across motifs + an overall headline find rate. */
@Composable
fun MotifRadarCard(
    motifs: List<MotifFindRate>,
    overallFindRatePct: Int,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    InsightCard(title = "Tactical Find Rate", modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("$overallFindRatePct%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = ChessGold)
            Text("overall find rate", style = MaterialTheme.typography.labelMedium, color = appColors.textSecondary)
        }
        RadarChart(
            axes = motifs.map { RadarAxis(it.motif.replaceFirstChar { c -> c.uppercase() }, it.ratePct / 100f) },
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            motifs.filter { it.opportunities > 0 }.forEach { m ->
                LabeledBar(
                    label = m.motif.replaceFirstChar { it.uppercase() },
                    valueText = "${m.found}/${m.opportunities}",
                    fraction = m.ratePct / 100f,
                    color = ChessGold,
                    labelWidth = 96,
                )
            }
        }
    }
}

/** Two panels: the user's strongest and weakest motif, each with a find-rate ring. */
@Composable
fun StrongestWeakestCard(
    strongest: MotifFindRate?,
    weakest: MotifFindRate?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MotifPanel("Strongest", strongest, StrongColor, Modifier.weight(1f))
        MotifPanel("Weakest", weakest, WeakColor, Modifier.weight(1f))
    }
}

@Composable
private fun MotifPanel(title: String, motif: MotifFindRate?, accent: Color, modifier: Modifier = Modifier) {
    val appColors = LocalAppColors.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.surface),
        border = BorderStroke(1.dp, appColors.border),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
                Text(
                    motif?.motif?.replaceFirstChar { it.uppercase() } ?: "—",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary,
                )
                if (motif != null) {
                    Text("${motif.found}/${motif.opportunities} found", style = MaterialTheme.typography.labelSmall, color = appColors.textSecondary)
                }
            }
            RingGauge(pct = motif?.ratePct ?: 0, color = accent, size = 64.dp)
        }
    }
}

/** Board-awareness card focused on spotting hanging pieces (a single motif find rate). */
@Composable
fun HangingPieceDetectionCard(
    hanging: MotifFindRate?,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    InsightCard(title = "Hanging Piece Detection", modifier = modifier) {
        if (hanging == null || hanging.opportunities == 0) {
            Text("No hanging-piece chances detected yet.", style = MaterialTheme.typography.bodySmall, color = appColors.textSecondary)
            return@InsightCard
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RingGauge(pct = hanging.ratePct, color = StrongColor, size = 72.dp)
            Text(
                "You spotted ${hanging.found} of ${hanging.opportunities} hanging-piece opportunities. " +
                    "Can you see what's hanging before you move?",
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textSecondary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
