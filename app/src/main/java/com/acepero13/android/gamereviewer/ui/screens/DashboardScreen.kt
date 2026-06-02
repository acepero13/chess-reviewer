package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.components.BehavioralProfileCard
import com.acepero13.android.gamereviewer.ui.components.HabitProgressCard
import com.acepero13.android.gamereviewer.ui.components.PhaseBreakdownCard
import com.acepero13.android.gamereviewer.ui.components.TopCoachTriggerCard
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.WCDark
import org.koin.androidx.compose.koinViewModel

/**
 * Cross-game Cognitive Diagnostic Dashboard (Task 4.3).
 *
 * Displays:
 * - Global stats: games imported, games analysed, total critical moments, habit mastery.
 * - Top 3 failure trends as [BehavioralProfileCard] rows.
 * - "Wishful Thinking" warning card if detected.
 * - Board Scan habit mastery progress.
 * - Endgame weaknesses.
 * - Prompt to analyse more games if < 3 have been processed.
 *
 * All major sections are collapsible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onBack:       () -> Unit,
    onStartDrill: ((categoryNames: String, drillTitle: String) -> Unit)? = null,
    vm:           DashboardViewModel = koinViewModel(),
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
            CollapsibleSection(title = "Overview") {
                GlobalStatsCard(state = state)
            }

            // ── Phase breakdown ───────────────────────────────────────────────
            state.phaseBreakdown?.let { breakdown ->
                if (breakdown.total > 0) {
                    CollapsibleSection(title = "Phase Breakdown") {
                        PhaseBreakdownCard(
                            breakdown = breakdown,
                            modifier  = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

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

            // ── Top coach trigger (focus habit) ──────────────────────────────
            state.topCoachTrigger?.let { trigger ->
                CollapsibleSection(title = "Focus Habit") {
                    TopCoachTriggerCard(
                        trigger  = trigger,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ── Board Scan habit progress ─────────────────────────────────────
            if (state.habitRows.isNotEmpty()) {
                CollapsibleSection(title = "Board Scan Habits") {
                    HabitProgressCard(
                        rows      = state.habitRows,
                        modifier  = Modifier.fillMaxWidth(),
                        showTitle = false,
                    )
                }
            }

            // ── Failure trends ────────────────────────────────────────────────
            if (state.trends.isNotEmpty()) {
                CollapsibleSection(title = "Your Top Failure Patterns") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.trends.forEach { trend ->
                            BehavioralProfileCard(
                                trend        = trend,
                                modifier     = Modifier.fillMaxWidth(),
                                onStartDrill = onStartDrill?.let { callback -> {
                                    val cats = trend.triggerCategories.joinToString(",") { it.name }
                                    callback(cats, "${trend.emoji} ${trend.title}")
                                }},
                            )
                        }
                    }
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

            // ── Endgame weaknesses ─────────────────────────────────────────────
            if (state.endgameWeaknesses.isNotEmpty()) {
                CollapsibleSection(title = "Endgame Weaknesses") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text  = "Endgame types where you made critical mistakes — study the corresponding chapters in 100 Endgames You Should Know.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        state.endgameWeaknesses.forEachIndexed { i, row ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Row(
                                    modifier              = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text(
                                        text       = "${i + 1}",
                                        style      = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color      = ChessGold,
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text       = row.name,
                                            style      = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text  = "Chapter ${row.chapter}  ·  ${row.gamesEncountered} game${if (row.gamesEncountered != 1) "s" else ""}, ${row.gamesWithMistake} with mistakes",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CollapsibleSection(
    title:   String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label       = "chevron",
    )
    Column(modifier = modifier) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color      = ChessGold,
            )
            Icon(
                imageVector        = Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint               = ChessGold,
                modifier           = Modifier.rotate(rotation),
            )
        }
        AnimatedVisibility(visible = expanded) {
            content()
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
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatColumn(label = "Games\nImported", value = "${state.totalGamesImported}")
                StatColumn(label = "Games\nAnalysed", value = "${state.gamesAnalyzed}")
                StatColumn(label = "Critical\nMoments",  value = "${state.totalCriticalMoments}")
            }
            if (state.habitRows.isNotEmpty()) {
                val mastered = state.habitRows.count { it.mastered }
                val total    = state.habitRows.size
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text     = "Habits",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(40.dp),
                    )
                    LinearProgressIndicator(
                        progress   = { mastered.toFloat() / total },
                        modifier   = Modifier.weight(1f).height(6.dp),
                        color      = ChessGold,
                        trackColor = ChessGold.copy(alpha = 0.2f),
                        strokeCap  = StrokeCap.Round,
                    )
                    Text(
                        text  = "$mastered / $total",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChessGold,
                    )
                }
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
