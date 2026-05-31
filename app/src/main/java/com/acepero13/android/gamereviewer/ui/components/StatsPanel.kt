package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.data.model.PlayerStats
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import com.acepero13.android.gamereviewer.engine.highlights.GameHighlight
import com.acepero13.chess.core.ui.theme.ChessGold

private val SurfaceDim = Color(0xFF1A1A1A)
private val ColDivider = Color(0xFF2D2D2D)

/**
 * Bottom sheet showing game stats and collapsible highlights list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsSheet(
    game:        ReviewGame?,
    highlights:  List<GameHighlight>,
    playerStats: Pair<PlayerStats, PlayerStats>?,
    onDismiss:   () -> Unit,
    onNavigate:  (moveIndex: Int) -> Unit,
    modifier:    Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        sheetState        = sheetState,
        containerColor    = SurfaceDim,
        modifier          = modifier,
    ) {
        LazyColumn(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── Game info header ─────────────────────────────────────────────
            item {
                Text(
                    "Game Info",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = ChessGold,
                )
                Spacer(Modifier.height(8.dp))
            }

            if (game != null) {
                // Basic metadata (result, date, opening)
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Column(
                            modifier            = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            StatRow("Result", game.result)
                            if (game.date.isNotEmpty())        StatRow("Date",    game.date)
                            if (game.event.isNotEmpty())       StatRow("Event",   game.event)
                            if (game.openingName.isNotEmpty()) StatRow("Opening", "${game.openingEco} · ${game.openingName}")
                        }
                    }
                }

                // ── Two-column player stats ──────────────────────────────────
                item {
                    Spacer(Modifier.height(4.dp))
                    when {
                        playerStats == null -> {
                            // Still loading
                            Row(
                                modifier            = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment   = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color    = ChessGold,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Computing stats…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        playerStats.first.totalMoves == 0 && playerStats.second.totalMoves == 0 -> {
                            Text(
                                "No engine evaluation yet — run background analysis first.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        else -> {
                            PlayerStatsColumns(
                                white = playerStats.first,
                                black = playerStats.second,
                            )
                        }
                    }
                }
            }

            // ── Highlights (collapsible) ─────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(Modifier.height(4.dp))
                HighlightsSection(
                    highlights = highlights,
                    onNavigate = { idx ->
                        onNavigate(idx)
                        onDismiss()
                    },
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ── Two-column player stats ───────────────────────────────────────────────────

@Composable
private fun PlayerStatsColumns(white: PlayerStats, black: PlayerStats) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(12.dp),
        ) {
            PlayerColumn(stats = white, modifier = Modifier.weight(1f))

            VerticalDivider(
                modifier  = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                color     = ColDivider,
                thickness = 1.dp,
            )

            PlayerColumn(stats = black, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun PlayerColumn(stats: PlayerStats, modifier: Modifier = Modifier) {
    val sideLabel = if (stats.isWhite) "White" else "Black"
    val sideColor = if (stats.isWhite) Color.White else Color(0xFFBBBBBB)

    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Header: piece icon + player name + rating
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text      = sideLabel,
                style     = MaterialTheme.typography.labelSmall,
                color     = sideColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
            Text(
                text       = stats.name,
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color      = sideColor,
                textAlign  = TextAlign.Center,
                maxLines   = 1,
            )
            if (stats.rating != "?") {
                Text(
                    text      = stats.rating,
                    style     = MaterialTheme.typography.labelSmall,
                    color     = ChessGold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        HorizontalDivider(color = ColDivider)

        // Stats grid
        StatItem(label = "Accuracy",        value = "${"%.1f".format(stats.accuracy)}%")
        StatItem(label = "Total moves",     value = "${stats.totalMoves}")
        StatItem(
            label = "Avg clock",
            value = stats.avgClockSeconds?.let { "${"%.1f".format(it)}s" } ?: "—",
        )
        StatItem(label = "Excellent %",   value = "${"%.0f".format(stats.excellentMovePercent)}%")
        StatItem(label = "Good moves %",  value = "${"%.0f".format(stats.goodMovePercent)}%")
        StatItem(label = "Blunder rate",    value = "${"%.0f".format(stats.blunderRate)}%")
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color      = ChessGold,
            textAlign  = TextAlign.Center,
        )
        Text(
            text      = label,
            style     = MaterialTheme.typography.labelSmall,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Highlights collapsible ────────────────────────────────────────────────────

@Composable
private fun HighlightsSection(
    highlights: List<GameHighlight>,
    onNavigate: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }

    Column {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text       = "Highlights (${highlights.size})",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
                modifier   = Modifier.weight(1f),
            )
            Icon(
                imageVector        = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(20.dp),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically(),
        ) {
            if (highlights.isEmpty()) {
                Text(
                    text     = "No highlights yet — analysis is still running.",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    highlights.forEach { h ->
                        HighlightChip(
                            highlight = h,
                            onClick   = { onNavigate(h.moveIndex) },
                            modifier  = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

// ── Stat row (basic key/value used for metadata) ──────────────────────────────

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text       = value,
            style      = MaterialTheme.typography.bodySmall,
            color      = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}
