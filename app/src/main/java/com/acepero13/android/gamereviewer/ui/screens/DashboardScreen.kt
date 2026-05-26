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
import androidx.compose.material.icons.outlined.Refresh
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
import com.acepero13.android.gamereviewer.ui.components.BehavioralProfileCard
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.WCDark
import org.koin.androidx.compose.koinViewModel

/**
 * Cross-game Cognitive Diagnostic Dashboard (Task 4.3).
 *
 * Displays:
 * - Global stats: games imported, games analysed, total critical moments.
 * - Top 3 failure trends as [BehavioralProfileCard] rows.
 * - "Wishful Thinking" warning card if detected.
 * - Prompt to analyse more games if < 3 have been processed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onBack: () -> Unit,
    vm: DashboardViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsState()

    Scaffold(
        containerColor = WCDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Cognitive Dashboard",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = ChessGold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, "Back", tint = ChessGold)
                    }
                },
                actions = {
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Outlined.Refresh, "Refresh", tint = ChessGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WCDark),
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = ChessGold)
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

            // ── Global stats ──────────────────────────────────────────────────
            GlobalStatsCard(state = state)

            // ── Wishful thinking warning ──────────────────────────────────────
            if (state.hasWishfulThinking) {
                WishfulThinkingCard()
            }

            // ── Not enough data nudge ─────────────────────────────────────────
            if (state.gamesAnalyzed < 3) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text     = "🔍 Open more games for review to unlock your full behavioural profile. " +
                            "${state.gamesAnalyzed} of ${state.totalGamesImported} games analysed so far.",
                        style    = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                        color    = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }

            // ── Failure trends ────────────────────────────────────────────────
            if (state.trends.isNotEmpty()) {
                Text(
                    "Your Top Failure Patterns",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = ChessGold,
                )
                state.trends.forEach { trend ->
                    BehavioralProfileCard(
                        trend    = trend,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else if (state.gamesAnalyzed > 0) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text     = "✅ Great news — no recurring failure patterns detected yet. " +
                            "Keep analysing games to track emerging trends.",
                        style    = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text     = "📊 No analysis data yet.\n\nImport a game and open it for review. " +
                            "Background analysis runs automatically — then return here for your profile.",
                        style    = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GlobalStatsCard(state: DashboardUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape  = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Overview",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = ChessGold,
            )
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatColumn(label = "Games\nImported", value = "${state.totalGamesImported}")
                StatColumn(label = "Games\nAnalysed", value = "${state.gamesAnalyzed}")
                StatColumn(label = "Critical\nMoments",  value = "${state.totalCriticalMoments}")
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            style      = MaterialTheme.typography.headlineSmall,
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
private fun WishfulThinkingCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text("🌈", style = MaterialTheme.typography.titleMedium)
            Text(
                text     = " Wishful Thinking detected — you frequently flag positions as " +
                    "critical where the engine sees little danger. This may indicate " +
                    "over-estimating threats. Try to verify candidate moves with concrete variations.",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
