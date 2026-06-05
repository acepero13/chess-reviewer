package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.acepero13.chess.core.ui.theme.ChessGold

/**
 * A single ECO entry: how many moves into the game the player typically stays in book.
 *
 * @param eco            ECO classification code (e.g. "B20").
 * @param openingName    Human-readable opening name.
 * @param avgDeviationMove Average move number at which this player leaves theory.
 * @param gameCount      Number of analyzed games in this opening.
 * @param maxDeviationMove Theoretical maximum used to normalise the progress bar.
 */
data class EcoDeviationRow(
    val eco:              String,
    val openingName:      String,
    val avgDeviationMove: Float,
    val gameCount:        Int,
    val maxDeviationMove: Int = 20,
)

/**
 * Lists the player's most-played ECO openings alongside the average move
 * at which they leave opening theory.
 *
 * A low deviation move (e.g. move 5) for a complex opening signals a
 * concrete preparation gap more precisely than "you had an opening deviation."
 */
@Composable
fun OpeningDeviationConvergenceCard(
    rows:     List<EcoDeviationRow>,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) return

    Card(
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape    = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text  = "Opening Preparation Depth",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text  = "Average move you leave opening theory, by ECO",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(12.dp))

            rows.forEachIndexed { i, row ->
                if (i > 0) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color     = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                DeviationRow(row = row)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DeviationRow(row: EcoDeviationRow) {
    val fraction = (row.avgDeviationMove / row.maxDeviationMove).coerceIn(0f, 1f)
    val depthLabel = when {
        row.avgDeviationMove < 6f  -> "Shallow"
        row.avgDeviationMove < 12f -> "Moderate"
        else                        -> "Deep"
    }
    val depthColor = when {
        row.avgDeviationMove < 6f  -> androidx.compose.ui.graphics.Color(0xFFE53935)
        row.avgDeviationMove < 12f -> androidx.compose.ui.graphics.Color(0xFFFF9800)
        else                        -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
    }

    Column {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text       = row.eco,
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color      = ChessGold,
                modifier   = Modifier.width(36.dp),
            )
            Text(
                text     = row.openingName,
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text  = "${row.gameCount}g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LinearProgressIndicator(
                progress   = { fraction },
                modifier   = Modifier.weight(1f).height(6.dp),
                color      = depthColor,
                trackColor = depthColor.copy(alpha = 0.2f),
                strokeCap  = StrokeCap.Round,
            )
            Text(
                text  = "move ${row.avgDeviationMove.toInt()} · $depthLabel",
                style = MaterialTheme.typography.labelSmall,
                color = depthColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
