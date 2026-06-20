package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.components.AccuracyStatsCard
import com.acepero13.android.gamereviewer.ui.components.AnalysisProgressCard
import com.acepero13.android.gamereviewer.ui.components.EngineCorrelationCard
import com.acepero13.android.gamereviewer.ui.components.MotifWeaknessCard
import com.acepero13.android.gamereviewer.ui.components.MoveTimeDistributionCard
import com.acepero13.android.gamereviewer.ui.components.PawnStructureCard
import com.acepero13.android.gamereviewer.ui.components.PhaseAccuracyCard
import com.acepero13.android.gamereviewer.ui.components.PhaseBreakdownCard
import com.acepero13.android.gamereviewer.ui.components.PlayerStyleCard
import com.acepero13.android.gamereviewer.ui.components.RadarChartCard
import com.acepero13.android.gamereviewer.ui.components.RecoveryRateCard
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors
import org.koin.androidx.compose.koinViewModel

/**
 * Cross-game analytics screen with two tabs:
 *  - **Dashboard** — the existing Cognitive Diagnostic ([DashboardContent]).
 *  - **Insights**  — Chess.com-style accuracy, phase breakdown and player-style profile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onBack:       () -> Unit,
    onStartDrill: ((categoryNames: String, drillTitle: String) -> Unit)? = null,
    dashboardVm:  DashboardViewModel = koinViewModel(),
    insightsVm:   InsightsViewModel  = koinViewModel(),
) {
    val appColors = LocalAppColors.current
    var tab by remember { mutableIntStateOf(0) }

    val dashState     by dashboardVm.uiState.collectAsState()
    val insightsState by insightsVm.uiState.collectAsState()

    Scaffold(
        containerColor = appColors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Analytics",
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
                    IconButton(onClick = { if (tab == 0) dashboardVm.refresh() else insightsVm.load() }) {
                        Icon(Icons.Outlined.Refresh, "Refresh", tint = ChessGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appColors.background),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = tab,
                containerColor   = appColors.background,
                contentColor     = ChessGold,
            ) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Dashboard") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Insights") })
            }

            when (tab) {
                0 -> DashboardContent(state = dashState, onStartDrill = onStartDrill)
                else -> InsightsContent(
                    state     = insightsState,
                    onAnalyze = insightsVm::analyzePendingGames,
                )
            }
        }
    }
}

@Composable
private fun InsightsContent(
    state:     InsightsUiState,
    onAnalyze: () -> Unit,
    modifier:  Modifier = Modifier,
) {
    val appColors = LocalAppColors.current

    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ChessGold)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AnalysisProgressCard(
            gamesAnalyzed = state.gamesAnalyzed,
            gamesPending  = state.gamesPending,
            inProgress    = state.analysisInProgress,
            progressDone  = state.progressDone,
            progressTotal = state.progressTotal,
            onAnalyze     = onAnalyze,
            modifier      = Modifier.fillMaxWidth(),
        )

        if (state.gamesAnalyzed == 0) {
            Text(
                text  = "No analyzed games yet. Run the analysis above to unlock your accuracy stats and style profile.",
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textSecondary,
            )
            return@Column
        }

        AccuracyStatsCard(
            avgAccuracy  = state.avgAccuracy,
            avgAcpl      = state.avgAcpl,
            blunders     = state.totalBlunders,
            mistakes     = state.totalMistakes,
            inaccuracies = state.totalInaccuracies,
            modifier     = Modifier.fillMaxWidth(),
        )

        PhaseAccuracyCard(
            openingAccuracy    = state.openingAccuracy,
            middlegameAccuracy = state.middlegameAccuracy,
            endgameAccuracy    = state.endgameAccuracy,
            attackAccuracy     = state.attackAccuracy,
            defenseAccuracy    = state.defenseAccuracy,
            modifier           = Modifier.fillMaxWidth(),
        )

        if (state.avgTimeWinningSec > 0f || state.avgTimeLosingSec > 0f) {
            MoveTimeDistributionCard(
                winningSec = state.avgTimeWinningSec,
                losingSec  = state.avgTimeLosingSec,
                modifier   = Modifier.fillMaxWidth(),
            )
        }

        if (state.sharpCorrelation > 0f || state.quietCorrelation > 0f) {
            EngineCorrelationCard(
                sharpCorrelation = state.sharpCorrelation,
                quietCorrelation = state.quietCorrelation,
                modifier         = Modifier.fillMaxWidth(),
            )
        }

        if (state.oversightCount > 0) {
            RecoveryRateCard(
                recoveryRate   = state.recoveryRate,
                oversightCount = state.oversightCount,
                modifier       = Modifier.fillMaxWidth(),
            )
        }

        MotifWeaknessCard(items = state.motifWeaknesses, modifier = Modifier.fillMaxWidth())

        PawnStructureCard(items = state.pawnStructures, modifier = Modifier.fillMaxWidth())

        state.phaseBreakdown?.let { breakdown ->
            if (breakdown.total > 0) {
                PhaseBreakdownCard(
                    breakdown = breakdown,
                    modifier  = Modifier.fillMaxWidth(),
                )
            }
        }

        state.profile?.let { profile ->
            RadarChartCard(profile = profile, modifier = Modifier.fillMaxWidth())
            PlayerStyleCard(profile = profile, modifier = Modifier.fillMaxWidth())
        }
    }
}
