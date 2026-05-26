package com.acepero13.android.gamereviewer.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    vm: GameReportViewModel = koinViewModel(parameters = { parametersOf(gameId) }),
) {
    val state by vm.uiState.collectAsState()

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
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Decision Velocity chart ────────────────────────────────────────
            if (state.hasTimeData) {
                DecisionVelocityChart(
                    decisions = state.decisions,
                    modifier  = Modifier.fillMaxWidth(),
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

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatCell(label = "Total moves",     value = "${state.totalMoves}")
                StatCell(label = "Blunders",        value = "${state.blunderCount}")
                StatCell(label = "Rushed blunders", value = "${state.rushedBlunders}")
            }

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatCell(
                    label = "Avg time on blunders",
                    value = "${"%.1f".format(state.avgTimeOnBlunders)}s",
                )
                StatCell(
                    label = "Avg time on good moves",
                    value = "${"%.1f".format(state.avgTimeOnGoodMoves)}s",
                )
                StatCell(label = "Careful blunders", value = "${state.carefulBlunders}")
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
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = ChessGold,
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
