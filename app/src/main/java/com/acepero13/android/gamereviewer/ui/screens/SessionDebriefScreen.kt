package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.SessionDebrief
import com.acepero13.android.gamereviewer.domain.TimeAnalyzer
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.WCDark
import org.koin.androidx.compose.koinViewModel

private val SurfaceCard = Color(0xFF1A1A1A)
private val GreenAccent = Color(0xFF2D6A4F)
private val TextOnCard  = Color(0xFFCCE8D9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDebriefScreen(
    onBack:       () -> Unit,
    onStartDrill: (categoryNames: String, drillTitle: String) -> Unit,
    vm: SessionDebriefViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsState()

    Scaffold(
        containerColor = WCDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Post-Session Debrief",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = ChessGold,
                        )
                        Text(
                            "Today's coaching summary",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = ChessGold) }
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

        val summary = state.summary
        if (summary == null || !summary.hasData) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(32.dp),
                ) {
                    Text("📭", style = MaterialTheme.typography.displaySmall)
                    Text(
                        "No session data yet",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = ChessGold,
                    )
                    Text(
                        "Import and review at least one game today to receive your coaching summary.",
                        style     = MaterialTheme.typography.bodySmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

            // ── Session header ─────────────────────────────────────────────────
            SessionHeaderCard(summary)

            // ── Time pattern stats ─────────────────────────────────────────────
            if (summary.hasTimeData) {
                TimePatternCard(summary)
            }

            // ── Top weakness ───────────────────────────────────────────────────
            summary.topTrend?.let { trend ->
                WeaknessCard(trend.emoji, trend.title, trend.totalCount)
            }

            // ── Coach's message ────────────────────────────────────────────────
            CoachMessageCard(summary.coachMessage)

            // ── Drill CTA ──────────────────────────────────────────────────────
            if (summary.drillCategoryNames.isNotEmpty()) {
                DrillCtaCard(
                    drillTitle = summary.drillTitle,
                    onStart    = {
                        onStartDrill(
                            summary.drillCategoryNames.joinToString(","),
                            summary.drillTitle,
                        )
                    },
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SessionHeaderCard(summary: SessionDebrief.Summary) {
    val gamesLabel = if (summary.gameCount == 1) "1 game" else "${summary.gameCount} games"
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape  = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ChessGold.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    "Today's Session",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = ChessGold,
                )
                Text(
                    "$gamesLabel reviewed",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextOnCard.copy(alpha = 0.7f),
                )
            }
            Text("📋", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun TimePatternCard(summary: SessionDebrief.Summary) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape    = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "⏱ Time Patterns",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = ChessGold,
            )

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                DebriefStat(
                    value = "${summary.totalFastMoves}",
                    label = "Fast moves\n(< ${TimeAnalyzer.FAST_MOVE_SECONDS}s)",
                )
                DebriefStat(
                    value = "${summary.rushedBlunders}",
                    label = "Rushed\nblunders",
                    highlight = summary.rushedBlunders >= 2,
                )
                DebriefStat(
                    value = "${summary.carefulBlunders}",
                    label = "Careful\nblunders",
                    highlight = summary.carefulBlunders >= 2,
                )
            }

            if (summary.totalBlunders > 0) {
                HorizontalDivider(color = GreenAccent.copy(alpha = 0.3f))
                val impulseRate = if (summary.totalBlunders > 0)
                    (summary.rushedBlunders * 100) / summary.totalBlunders else 0
                Text(
                    text  = "$impulseRate% of blunders were impulsive (played under ${TimeAnalyzer.FAST_MOVE_SECONDS}s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextOnCard.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun WeaknessCard(emoji: String, title: String, count: Int) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape    = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Column {
                Text(
                    "Top Session Weakness",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextOnCard.copy(alpha = 0.55f),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    title,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = ChessGold,
                )
                Text(
                    "Occurred $count time${if (count == 1) "" else "s"} in this session",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextOnCard.copy(alpha = 0.65f),
                )
            }
        }
    }
}

@Composable
private fun CoachMessageCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF163B2A),
        ),
        shape  = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GreenAccent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "🎓 Coach's Observation",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = ChessGold,
            )
            Text(
                text      = message,
                style     = MaterialTheme.typography.bodySmall,
                color     = TextOnCard,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

@Composable
private fun DrillCtaCard(drillTitle: String, onStart: () -> Unit) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape    = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Tomorrow's Focus",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = ChessGold,
            )
            Text(
                "Recommended drill based on today's patterns:",
                style = MaterialTheme.typography.bodySmall,
                color = TextOnCard.copy(alpha = 0.65f),
            )
            Button(
                onClick  = onStart,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = GreenAccent),
            ) {
                Icon(
                    Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = Color.White,
                )
                Text(
                    "  Start: $drillTitle",
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White,
                )
            }
        }
    }
}

@Composable
private fun DebriefStat(value: String, label: String, highlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = if (highlight) Color(0xFFEF4444) else ChessGold,
        )
        Text(
            text      = label,
            style     = MaterialTheme.typography.labelSmall,
            color     = TextOnCard.copy(alpha = 0.65f),
        )
    }
}
