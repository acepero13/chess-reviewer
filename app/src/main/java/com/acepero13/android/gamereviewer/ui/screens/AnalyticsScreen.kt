package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.AnalyticsFilter
import com.acepero13.android.gamereviewer.domain.AnalyticsFilterStore
import com.acepero13.android.gamereviewer.domain.DAY_OPTIONS
import com.acepero13.android.gamereviewer.domain.GAME_OPTIONS
import org.koin.compose.koinInject
import com.acepero13.android.gamereviewer.ui.components.AccuracyStatsCard
import com.acepero13.android.gamereviewer.ui.components.AnalysisProgressCard
import com.acepero13.android.gamereviewer.ui.components.BlunderCausesCard
import com.acepero13.android.gamereviewer.ui.components.BlunderDirectionCard
import com.acepero13.android.gamereviewer.ui.components.CleanRateDonutCard
import com.acepero13.android.gamereviewer.ui.components.ComposureTimelineChart
import com.acepero13.android.gamereviewer.ui.components.ConversionScatterChart
import com.acepero13.android.gamereviewer.ui.components.EngineCorrelationCard
import com.acepero13.android.gamereviewer.ui.components.HangingPieceDetectionCard
import com.acepero13.android.gamereviewer.ui.components.MotifRadarCard
import com.acepero13.android.gamereviewer.ui.components.MotifWeaknessCard
import com.acepero13.android.gamereviewer.ui.components.MoveTimeDistributionCard
import com.acepero13.android.gamereviewer.ui.components.NotablePositionCarousel
import com.acepero13.android.gamereviewer.ui.components.OpeningTable
import com.acepero13.android.gamereviewer.ui.components.PawnStructureCard
import com.acepero13.android.gamereviewer.ui.components.PhaseAccuracyCard
import com.acepero13.android.gamereviewer.ui.components.PhaseBreakdownCard
import com.acepero13.android.gamereviewer.ui.components.PlayerStyleCard
import com.acepero13.android.gamereviewer.ui.components.RadarChartCard
import com.acepero13.android.gamereviewer.ui.components.RatingLeakCard
import com.acepero13.android.gamereviewer.ui.components.RecoveryRateCard
import com.acepero13.android.gamereviewer.ui.components.RepertoireConcentrationDonut
import com.acepero13.android.gamereviewer.ui.components.StatTile
import com.acepero13.android.gamereviewer.ui.components.StatTileGrid
import com.acepero13.android.gamereviewer.ui.components.StrongestWeakestCard
import com.acepero13.android.gamereviewer.ui.components.TimeBudgetByPhaseCard
import com.acepero13.android.gamereviewer.ui.components.TopOpeningCard
import com.acepero13.android.gamereviewer.ui.components.WhenAheadBehindCard
import androidx.compose.ui.graphics.Color
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
    blunderVm:    BlunderInsightsViewModel = koinViewModel(),
    conversionVm: ConversionViewModel  = koinViewModel(),
    disciplineVm: DisciplineViewModel  = koinViewModel(),
    preparationVm: PreparationViewModel = koinViewModel(),
    tacticsVm:    TacticsViewModel     = koinViewModel(),
) {
    val appColors = LocalAppColors.current
    var tab by remember { mutableIntStateOf(0) }

    val dashState     by dashboardVm.uiState.collectAsState()
    val insightsState by insightsVm.uiState.collectAsState()
    val blunderState  by blunderVm.uiState.collectAsState()
    val conversionState  by conversionVm.uiState.collectAsState()
    val disciplineState  by disciplineVm.uiState.collectAsState()
    val preparationState by preparationVm.uiState.collectAsState()
    val tacticsState     by tacticsVm.uiState.collectAsState()

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
                    IconButton(onClick = {
                        when (tab) {
                            0 -> dashboardVm.refresh()
                            1 -> insightsVm.load()
                            2 -> blunderVm.load()
                            3 -> conversionVm.load()
                            4 -> disciplineVm.load()
                            5 -> preparationVm.load()
                            else -> tacticsVm.load()
                        }
                    }) {
                        Icon(Icons.Outlined.Refresh, "Refresh", tint = ChessGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appColors.background),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = tab,
                containerColor   = appColors.background,
                contentColor     = ChessGold,
                edgePadding      = 0.dp,
            ) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Dashboard") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Insights") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Blunders") })
                Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("Conversion") })
                Tab(selected = tab == 4, onClick = { tab = 4 }, text = { Text("Discipline") })
                Tab(selected = tab == 5, onClick = { tab = 5 }, text = { Text("Preparation") })
                Tab(selected = tab == 6, onClick = { tab = 6 }, text = { Text("Tactics") })
            }

            AnalyticsFilterBar()

            when (tab) {
                0 -> DashboardContent(state = dashState, onStartDrill = onStartDrill)
                1 -> InsightsContent(
                    state     = insightsState,
                    onAnalyze = insightsVm::analyzePendingGames,
                )
                2 -> BlunderInsightsContent(
                    state     = blunderState,
                    onAnalyze = blunderVm::analyzePendingGames,
                )
                3 -> ConversionContent(
                    state     = conversionState,
                    onAnalyze = conversionVm::analyzePendingGames,
                )
                4 -> DisciplineContent(
                    state     = disciplineState,
                    onAnalyze = disciplineVm::analyzePendingGames,
                )
                5 -> PreparationContent(
                    state     = preparationState,
                    onAnalyze = preparationVm::analyzePendingGames,
                )
                else -> TacticsContent(
                    state     = tacticsState,
                    onAnalyze = tacticsVm::analyzePendingGames,
                )
            }
        }
    }
}

/**
 * Shared scope selector applied to every tab. Date and game-count windows are mutually
 * exclusive: choosing one resets the other (both drive the single [AnalyticsFilter] value held
 * by the [AnalyticsFilterStore] singleton, which every tab's ViewModel observes).
 */
@Composable
private fun AnalyticsFilterBar(modifier: Modifier = Modifier) {
    val filterStore: AnalyticsFilterStore = koinInject()
    val filter by filterStore.filter.collectAsState()

    val selectedDays  = (filter as? AnalyticsFilter.ByDays)?.days
    val selectedGames = (filter as? AnalyticsFilter.ByGames)?.count

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterDropdown(
            label    = selectedDays?.let { "Last $it days" } ?: "All dates",
            options  = DAY_OPTIONS.map { it to "Last $it days" },
            allLabel = "All dates",
            onAll    = { filterStore.set(AnalyticsFilter.All) },
            onPick   = { filterStore.set(AnalyticsFilter.ByDays(it)) },
            modifier = Modifier.weight(1f),
        )
        FilterDropdown(
            label    = selectedGames?.let { "Last $it games" } ?: "All games",
            options  = GAME_OPTIONS.map { it to "Last $it games" },
            allLabel = "All games",
            onAll    = { filterStore.set(AnalyticsFilter.All) },
            onPick   = { filterStore.set(AnalyticsFilter.ByGames(it)) },
            modifier = Modifier.weight(1f),
        )
    }
}

/** A compact chip that opens a [DropdownMenu] of an "All" entry plus the supplied [options]. */
@Composable
private fun FilterDropdown(
    label: String,
    options: List<Pair<Int, String>>,
    allLabel: String,
    onAll: () -> Unit,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        Surface(
            color  = appColors.background,
            shape  = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, ChessGold),
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = ChessGold)
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = ChessGold)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text    = { Text(allLabel) },
                onClick = { onAll(); expanded = false },
            )
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text    = { Text(text) },
                    onClick = { onPick(value); expanded = false },
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

/**
 * The **Blunders** tab — a Chess.com-style "Catastrophic Blunders" view reframing the cached
 * stats around the single story of blunders: estimated rating leak, clean rate, what causes
 * them, when they happen and which direction they take.
 */
@Composable
private fun BlunderInsightsContent(
    state:     BlunderUiState,
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

        val report = state.report
        if (report == null || !report.hasData) {
            Text(
                text  = "No analyzed games yet. Run the analysis above to see what your blunders " +
                    "are costing you.",
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textSecondary,
            )
            return@Column
        }

        RatingLeakCard(leak = report.ratingLeak, modifier = Modifier.fillMaxWidth())

        CleanRateDonutCard(
            counts       = report.classification,
            cleanRatePct = report.cleanRatePct,
            modifier     = Modifier.fillMaxWidth(),
        )

        BlunderCausesCard(causes = report.causes, modifier = Modifier.fillMaxWidth())

        val phaseBreakdown = PhaseBreakdown(
            opening    = report.phaseOpening,
            middlegame = report.phaseMiddlegame,
            endgame    = report.phaseEndgame,
        )
        if (phaseBreakdown.total > 0) {
            PhaseBreakdownCard(breakdown = phaseBreakdown, modifier = Modifier.fillMaxWidth())
        }

        BlunderDirectionCard(direction = report.direction, modifier = Modifier.fillMaxWidth())
    }
}

/** Shared scaffold for the new tabs: loading spinner, progress card, then [body] once data exists. */
@Composable
private fun TabScaffold(
    isLoading: Boolean,
    gamesAnalyzed: Int,
    gamesPending: Int,
    analysisInProgress: Boolean,
    progressDone: Int,
    progressTotal: Int,
    hasData: Boolean,
    onAnalyze: () -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier,
    body: @Composable () -> Unit,
) {
    val appColors = LocalAppColors.current
    if (isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ChessGold)
        }
        return
    }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AnalysisProgressCard(
            gamesAnalyzed = gamesAnalyzed,
            gamesPending  = gamesPending,
            inProgress    = analysisInProgress,
            progressDone  = progressDone,
            progressTotal = progressTotal,
            onAnalyze     = onAnalyze,
            modifier      = Modifier.fillMaxWidth(),
        )
        if (!hasData) {
            Text(emptyText, style = MaterialTheme.typography.bodySmall, color = appColors.textSecondary)
        } else {
            body()
        }
    }
}

@Composable
private fun ConversionContent(state: ConversionUiState, onAnalyze: () -> Unit) {
    val report = state.report
    TabScaffold(
        isLoading = state.isLoading, gamesAnalyzed = state.gamesAnalyzed, gamesPending = state.gamesPending,
        analysisInProgress = state.analysisInProgress, progressDone = state.progressDone, progressTotal = state.progressTotal,
        hasData = report != null && report.hasData, onAnalyze = onAnalyze,
        emptyText = "No analyzed games yet. Run the analysis above to see how well you convert winning " +
            "positions and save losing ones.",
    ) {
        report!!
        WhenAheadBehindCard(ahead = report.whenAhead, behind = report.whenBehind, modifier = Modifier.fillMaxWidth())
        ConversionScatterChart(
            title = "When Ahead",
            points = report.winningScatter,
            goodLabel = "Converted",
            badLabel = "Threw",
            readingHint = "A low median in the Threw column means wins slip away through inaccurate " +
                "play, not bad luck.",
            modifier = Modifier.fillMaxWidth(),
        )
        ConversionScatterChart(
            title = "When Behind",
            points = report.losingScatter,
            goodLabel = "Saved",
            badLabel = "Lost",
            readingHint = "A high median in the Saved column shows the accuracy lift that rescues " +
                "losing positions.",
            modifier = Modifier.fillMaxWidth(),
        )
        NotablePositionCarousel(
            title = "Missed Simplifications",
            positions = report.missedSimplifications,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DisciplineContent(state: DisciplineUiState, onAnalyze: () -> Unit) {
    val report = state.report
    TabScaffold(
        isLoading = state.isLoading, gamesAnalyzed = state.gamesAnalyzed, gamesPending = state.gamesPending,
        analysisInProgress = state.analysisInProgress, progressDone = state.progressDone, progressTotal = state.progressTotal,
        hasData = report != null && report.hasData, onAnalyze = onAnalyze,
        emptyText = "No clock data yet. Import games with move times and run the analysis to see your " +
            "composure timeline.",
    ) {
        report!!
        StatTileGrid(
            tiles = listOf(
                StatTile("⏱", report.gamesInTimePressure.toString(), "In time pressure", Color(0xFF64B5F6)),
                StatTile("🚩", report.flaggedOnTime.toString(), "Flagged on time", Color(0xFFE57373)),
                StatTile("⚡", report.blundersUnderPressure.toString(), "Blunders under pressure", Color(0xFFFFB74D)),
                StatTile("⚠", report.decisiveBlunders.toString(), "Decisive blunders", Color(0xFFE57373)),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        ComposureTimelineChart(points = report.composure, modifier = Modifier.fillMaxWidth())
        TimeBudgetByPhaseCard(you = report.youBudget, opponent = report.opponentBudget, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PreparationContent(state: PreparationUiState, onAnalyze: () -> Unit) {
    val report = state.report
    TabScaffold(
        isLoading = state.isLoading, gamesAnalyzed = state.gamesAnalyzed, gamesPending = state.gamesPending,
        analysisInProgress = state.analysisInProgress, progressDone = state.progressDone, progressTotal = state.progressTotal,
        hasData = report != null && report.hasData, onAnalyze = onAnalyze,
        emptyText = "No analyzed games yet. Run the analysis above to map out your opening repertoire.",
    ) {
        report!!
        report.topOpening?.let {
            TopOpeningCard(opening = it, overallBookDepthPly = report.overallBookDepthPly, modifier = Modifier.fillMaxWidth())
        }
        OpeningTable(openings = report.openings, modifier = Modifier.fillMaxWidth())
        RepertoireConcentrationDonut(
            concentrationPct = report.concentrationPct,
            distinctOpenings = report.distinctOpenings,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TacticsContent(state: TacticsUiState, onAnalyze: () -> Unit) {
    val report = state.report
    TabScaffold(
        isLoading = state.isLoading, gamesAnalyzed = state.gamesAnalyzed, gamesPending = state.gamesPending,
        analysisInProgress = state.analysisInProgress, progressDone = state.progressDone, progressTotal = state.progressTotal,
        hasData = report != null && report.hasData, onAnalyze = onAnalyze,
        emptyText = "No tactical chances detected yet. Run the analysis above to see your motif find rate.",
    ) {
        report!!
        MotifRadarCard(motifs = report.motifs, overallFindRatePct = report.overallFindRatePct, modifier = Modifier.fillMaxWidth())
        StrongestWeakestCard(strongest = report.strongest, weakest = report.weakest, modifier = Modifier.fillMaxWidth())
        HangingPieceDetectionCard(
            hanging = report.motifs.firstOrNull { it.motif == "hanging" },
            modifier = Modifier.fillMaxWidth(),
        )
        NotablePositionCarousel(
            title = "Tactics You Missed",
            positions = report.missedPositions,
            modifier = Modifier.fillMaxWidth(),
        )
        NotablePositionCarousel(
            title = "Tactics You Found",
            positions = report.foundPositions,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
