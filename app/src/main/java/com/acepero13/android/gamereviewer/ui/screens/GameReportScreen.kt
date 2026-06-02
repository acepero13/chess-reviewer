package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.GameNarrativeSummary
import com.acepero13.android.gamereviewer.ui.components.DecisionVelocityChart
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.WCDark
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Per-game analysis report screen (Task 4.2).
 *
 * Shows:
 * - Decision Velocity chart (eval delta area + time-coloured dots).
 * - Aggregate time analysis stats (rushed blunders, avg time on blunders vs good moves).
 * - A hint toward the player's session patterns.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameReportScreen(
    gameId: Long,
    onBack: () -> Unit,
    onNavigateToMove: (moveIndex: Int) -> Unit = {},
    vm: GameReportViewModel = koinViewModel(parameters = { parametersOf(gameId) }),
) {
    val state by vm.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = WCDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Game Report",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = ChessGold,
                        )
                        if (state.gameTitle.isNotEmpty()) {
                            Text(
                                state.gameTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, "Back", tint = ChessGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WCDark),
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier          = Modifier.fillMaxSize().padding(padding),
                contentAlignment  = Alignment.Center,
            ) {
                CircularProgressIndicator(color = ChessGold)
            }
            return@Scaffold
        }

        if (state.error != null) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = WCDark,
                contentColor     = ChessGold,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    text     = { Text("Overview", style = MaterialTheme.typography.labelMedium) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    text     = { Text("Moves List", style = MaterialTheme.typography.labelMedium) },
                )
            }

            AnimatedContent(
                targetState = selectedTab,
                label       = "report_tab",
            ) { tab ->
                when (tab) {
                    0 -> OverviewTabContent(state, onNavigateToMove)
                    1 -> MoveListTab(entries = state.moveListEntries)
                }
            }
        }
    }
}

@Composable
private fun OverviewTabContent(
    state: GameReportUiState,
    onNavigateToMove: (moveIndex: Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Narrative summary ─────────────────────────────────────────────
        state.narrative?.let { NarrativeSummaryCard(it) }

        // ── Decision Velocity chart ────────────────────────────────────────
        if (state.hasTimeData) {
            DecisionVelocityChart(
                decisions    = state.decisions,
                modifier     = Modifier.fillMaxWidth(),
                onMoveClick  = onNavigateToMove,
            )
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text     = "⏱ No clock data in PGN — time analysis unavailable.\n" +
                        "Import games from Chess.com or Lichess for time tracking.",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        // ── Summary stats ─────────────────────────────────────────────────
        if (state.decisions.isNotEmpty()) {
            StatsSectionCard(state = state)
        }

        // ── Eval-only summary when no time data ───────────────────────────
        if (!state.hasTimeData && state.evaluations.isNotEmpty()) {
            EvalOnlySummaryCard(evaluations = state.evaluations)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatsSectionCard(state: GameReportUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape  = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Time Analysis",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = ChessGold,
            )

            // 2-column grid — each cell gets equal width so labels never overlap
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCell(
                    modifier = Modifier.weight(1f),
                    label    = "Total moves",
                    value    = "${state.totalMoves}",
                )
                StatCell(
                    modifier = Modifier.weight(1f),
                    label    = "Blunders",
                    value    = "${state.blunderCount}",
                )
            }

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCell(
                    modifier = Modifier.weight(1f),
                    label    = "Rushed blunders",
                    value    = "${state.rushedBlunders}",
                )
                StatCell(
                    modifier = Modifier.weight(1f),
                    label    = "Careful blunders",
                    value    = "${state.carefulBlunders}",
                )
            }

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCell(
                    modifier = Modifier.weight(1f),
                    label    = "Avg time on blunders",
                    value    = "${"%.1f".format(state.avgTimeOnBlunders)}s",
                )
                StatCell(
                    modifier = Modifier.weight(1f),
                    label    = "Avg time on good moves",
                    value    = "${"%.1f".format(state.avgTimeOnGoodMoves)}s",
                )
            }

            // Coaching insight
            if (state.rushedBlunders > state.carefulBlunders && state.rushedBlunders > 0) {
                InsightChip(
                    emoji   = "⚡",
                    message = "Most of your blunders were played quickly — consider slowing " +
                        "down in sharp positions.",
                )
            } else if (state.carefulBlunders > 0) {
                InsightChip(
                    emoji   = "🔍",
                    message = "You spent significant time on moves that still turned into blunders. " +
                        "This suggests calculation errors — review candidate selection.",
                )
            }
        }
    }
}

@Composable
private fun EvalOnlySummaryCard(evaluations: List<com.acepero13.android.gamereviewer.data.model.GameEvaluation>) {
    val bigSwings = evaluations.count { kotlin.math.abs(it.evalDelta) >= 150 }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape  = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Evaluation Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = ChessGold)
            Text("Positions analysed: ${evaluations.size}", style = MaterialTheme.typography.bodySmall)
            Text("Significant swings (≥150 cp): $bigSwings", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier              = modifier,
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text       = value,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = ChessGold,
            textAlign  = TextAlign.Center,
        )
        Text(
            text      = label,
            style     = MaterialTheme.typography.labelSmall,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines  = 2,
            overflow  = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NarrativeSummaryCard(summary: GameNarrativeSummary.Summary) {
    val accentColor = when (summary.outcome) {
        GameNarrativeSummary.Outcome.WIN     -> Color(0xFF4CAF50)
        GameNarrativeSummary.Outcome.LOSS    -> Color(0xFFEF5350)
        GameNarrativeSummary.Outcome.DRAW    -> ChessGold
        GameNarrativeSummary.Outcome.UNKNOWN -> ChessGold
    }
    val outcomeEmoji = when (summary.outcome) {
        GameNarrativeSummary.Outcome.WIN     -> "🏆"
        GameNarrativeSummary.Outcome.LOSS    -> "📉"
        GameNarrativeSummary.Outcome.DRAW    -> "🤝"
        GameNarrativeSummary.Outcome.UNKNOWN -> "🔍"
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "$outcomeEmoji Game Summary",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = accentColor,
            )
            Text(
                summary.headline,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            if (summary.details.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    summary.details.forEach { detail ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment     = Alignment.Top,
                        ) {
                            Text("•", style = MaterialTheme.typography.bodySmall, color = accentColor)
                            Text(
                                detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightChip(emoji: String, message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(emoji, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.padding(start = 8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}
