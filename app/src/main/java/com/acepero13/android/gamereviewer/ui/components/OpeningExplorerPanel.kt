package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.acepero13.chess.core.opening.MoveFrequency
import com.acepero13.chess.core.ui.theme.ChessGold
import java.text.NumberFormat

@Composable
fun OpeningExplorerPanel(
    state: OpeningExplorerUiState,
    onMoveSelected: (uci: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ExplorerHeader(state)
        HorizontalDivider()
        when {
            state.loading -> {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ChessGold, modifier = Modifier.size(28.dp))
                }
            }
            state.result == null || state.result.moves.isEmpty() -> {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text  = "No master games for this position",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                MoveTableHeader()
                HorizontalDivider()
                val total = state.result.total.coerceAtLeast(1)
                LazyColumn {
                    items(state.result.moves) { move ->
                        MoveRow(move = move, totalGames = total, onClick = { onMoveSelected(move.uci) })
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplorerHeader(state: OpeningExplorerUiState) {
    val result = state.result
    val title = when {
        result?.openingEco != null && result.openingName != null -> "${result.openingEco} ${result.openingName}"
        result?.openingName != null -> result.openingName!!
        !state.loading && result != null -> "Opening Explorer"
        else -> "Opening Explorer"
    }
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector        = Icons.AutoMirrored.Outlined.MenuBook,
            contentDescription = null,
            tint               = ChessGold,
            modifier           = Modifier.size(18.dp),
        )
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
            modifier   = Modifier.weight(1f),
        )
        if (result != null && result.total > 0) {
            Text(
                text  = formatGames(result.total) + " games",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MoveTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text("Move",  style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(52.dp))
        Text("Games", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(110.dp))
        Text("White / Draw / Black", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MoveRow(move: MoveFrequency, totalGames: Int, onClick: () -> Unit) {
    val pct = if (totalGames > 0) (move.total * 100 / totalGames) else 0
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text       = move.san,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.width(52.dp),
        )
        Text(
            text     = "${formatGames(move.total)} ($pct%)",
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp),
        )
        WinRateBar(
            white  = move.white,
            draws  = move.draws,
            black  = move.black,
            total  = move.total.coerceAtLeast(1),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WinRateBar(white: Int, draws: Int, black: Int, total: Int, modifier: Modifier = Modifier) {
    val whiteFraction = white.toFloat() / total
    val drawFraction  = draws.toFloat() / total
    val blackFraction = black.toFloat() / total
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        ) {
            if (whiteFraction > 0f) Box(Modifier.weight(whiteFraction).height(8.dp).background(Color(0xFFE8E8E8)))
            if (drawFraction  > 0f) Box(Modifier.weight(drawFraction).height(8.dp).background(Color(0xFF9E9E9E)))
            if (blackFraction > 0f) Box(Modifier.weight(blackFraction).height(8.dp).background(Color(0xFF424242)))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${(whiteFraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${(drawFraction  * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${(blackFraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatGames(count: Int): String = NumberFormat.getNumberInstance().format(count)
