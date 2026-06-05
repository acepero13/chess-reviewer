package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.acepero13.android.gamereviewer.domain.PhaseFailureHeatmap
import com.acepero13.android.gamereviewer.domain.PhaseFailureRow
import com.acepero13.chess.core.ui.theme.ChessGold

/**
 * Heatmap grid showing mistake distribution across Opening / Middlegame / Endgame
 * versus all failure categories. Layout: categories as rows, phases as columns so
 * every label fits without clipping. The darker the cell, the more frequent the
 * failure in that phase.
 */
@Composable
fun PhaseFailureHeatmapCard(
    rows:     List<PhaseFailureRow>,
    modifier: Modifier = Modifier,
) {
    if (rows.all { it.total == 0 }) return

    // Collect all categories that appear in at least one phase
    val allCategories = rows
        .flatMap { it.cells.keys }
        .distinct()
        .sortedByDescending { cat -> rows.sumOf { row -> row.cells[cat] ?: 0 } }

    val phases   = rows.map { it.phase }
    val maxCount = rows.flatMap { it.cells.values }.maxOrNull() ?: 1

    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape    = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text  = "Phase × Failure Heatmap",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text  = "Where in the game each mistake type occurs most often",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(12.dp))

            // Column headers: phase names
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // spacer for row-label column
                Spacer(Modifier.width(100.dp))
                phases.forEach { phase ->
                    Text(
                        text      = phase,
                        style     = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color     = ChessGold,
                        modifier  = Modifier.weight(1f),
                        maxLines  = 1,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // One row per failure category
            allCategories.forEach { cat ->
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text     = PhaseFailureHeatmap.categoryLabel(cat),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(100.dp),
                        maxLines = 2,
                    )
                    rows.forEach { row ->
                        val count = row.cells[cat] ?: 0
                        HeatCell(
                            count    = count,
                            maxCount = maxCount,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // Phase totals footer
            Spacer(Modifier.height(4.dp))
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(100.dp))
                rows.forEach { row ->
                    Text(
                        text     = if (row.total > 0) "${row.total} err" else "—",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatCell(
    count:    Int,
    maxCount: Int,
    modifier: Modifier = Modifier,
) {
    val intensity = if (maxCount == 0) 0f else count.toFloat() / maxCount
    val cellColor = Color(0xFFF0A500).copy(alpha = 0.15f + intensity * 0.75f)

    Box(
        modifier         = modifier.padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier         = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (count > 0) cellColor else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (count > 0) {
                Text(
                    text       = "$count",
                    style      = MaterialTheme.typography.labelSmall,
                    color      = if (intensity > 0.5f) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
