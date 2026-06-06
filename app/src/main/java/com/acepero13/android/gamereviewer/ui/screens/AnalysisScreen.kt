package com.acepero13.android.gamereviewer.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.components.AnalysePanel
import com.acepero13.android.gamereviewer.ui.components.CriticalMomentSheet
import com.acepero13.android.gamereviewer.ui.components.DevCoachPromptSheet
import com.acepero13.android.gamereviewer.ui.components.PredictionGate
import com.acepero13.android.gamereviewer.ui.components.StatsSheet
import com.acepero13.android.gamereviewer.ui.screens.analysis.AnalysisBottomBar
import com.acepero13.android.gamereviewer.ui.screens.analysis.AnalysisBoardArea
import com.acepero13.android.gamereviewer.ui.screens.analysis.AnalysisTopBar
import com.acepero13.android.gamereviewer.ui.screens.analysis.MentorModePanel
import com.acepero13.android.gamereviewer.ui.screens.analysis.MissedMomentBanner
import com.acepero13.android.gamereviewer.ui.screens.analysis.NavigateModePanel
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    gameId:                Long,
    onBack:                () -> Unit,
    onViewReport:          (Long) -> Unit = {},
    initialMoveIndex:      Int? = null,
    onInitialMoveConsumed: () -> Unit = {},
    vm:                    AnalysisViewModel = koinViewModel(parameters = { parametersOf(gameId) }),
) {
    val state by vm.uiState.collectAsState()

    BackHandler(enabled = state.reviewMode != ReviewMode.NAVIGATE) {
        when (state.reviewMode) {
            ReviewMode.ANALYSE -> vm.setReviewMode(ReviewMode.NAVIGATE)
            ReviewMode.MENTOR  -> vm.exitMentorMode()
            else               -> {}
        }
    }

    LaunchedEffect(initialMoveIndex) {
        if (initialMoveIndex != null) { vm.goToMove(initialMoveIndex); onInitialMoveConsumed() }
    }

    val sheetState    = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val devSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope    = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context  = LocalContext.current

    var showDevSheet  by remember { mutableStateOf(false) }
    var devPromptText by remember { mutableStateOf("") }

    if (showDevSheet && devPromptText.isNotBlank()) {
        DevCoachPromptSheet(
            dataSection = devPromptText,
            sheetState  = devSheetState,
            onCopy = { text ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Coach LLM Prompt", text))
                scope.launch { snackbar.showSnackbar("Prompt copied to clipboard") }
            },
            onDismiss = { showDevSheet = false; scope.launch { devSheetState.hide() } },
        )
    }

    if (state.showStatsSheet) {
        StatsSheet(
            game        = state.game,
            highlights  = state.gameHighlights,
            playerStats = state.playerStats,
            onDismiss   = vm::dismissStatsSheet,
            onNavigate  = { idx -> vm.dismissStatsSheet(); vm.onMoveNodeClick(idx.toLong()) },
        )
    }

    if (state.showCriticalSheet) {
        CriticalMomentSheet(
            sheetState = sheetState,
            onSubmit   = { plan, threats, candidates ->
                vm.saveCriticalAnswers(plan, threats, candidates)
                scope.launch { sheetState.hide() }
            },
            onDismiss  = { vm.dismissCriticalSheet(); scope.launch { sheetState.hide() } },
        )
    }

    val hasCoachContent = state.showProactiveCoaching || state.showCalibrationPanel ||
        state.guidedDiscoveryMode || state.reviewMode == ReviewMode.MENTOR ||
        state.showOpeningDeviationPanel || state.showEndgameRecognitionPanel || state.showMiddlegamePlanPanel
    val boardFraction by animateFloatAsState(if (hasCoachContent) 0.65f else 1f, tween(300), label = "boardFraction")

    val devPanelActive = state.developerModeEnabled &&
        (state.showProactiveCoaching || state.guidedDiscoveryMode || state.showMiddlegamePlanPanel || state.showEndgameRecognitionPanel)

    val appColors = LocalAppColors.current

    Scaffold(
        containerColor       = appColors.background,
        snackbarHost         = { SnackbarHost(snackbar) },
        topBar               = { AnalysisTopBar(state, vm, gameId, onBack, onViewReport) },
        bottomBar            = { AnalysisBottomBar(state, vm, snackbar, scope) },
        floatingActionButton = {
            if (devPanelActive) {
                SmallFloatingActionButton(
                    onClick        = { vm.buildCoachEvalPrompt()?.let { devPromptText = it; showDevSheet = true } },
                    containerColor = Color(0xFF1F3A2A),
                    contentColor   = Color(0xFF4ADE80),
                ) { Icon(Icons.Outlined.BugReport, "Copy LLM coach accuracy prompt", modifier = Modifier.size(18.dp)) }
            }
        },
    ) { padding ->
        if (state.game == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ChessGold)
                    Spacer(Modifier.height(12.dp))
                    Text("Loading game…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier            = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.reviewMode != ReviewMode.MENTOR) {
                    MissedMomentBanner(
                        visible   = state.showMissedMomentBanner,
                        onReview  = vm::reviewMissedMoment,
                        onDismiss = vm::dismissMissedMomentBanner,
                    )
                }
                AnalysisBoardArea(state, vm, boardFraction, snackbar, scope)
                AnimatedContent(
                    targetState    = state.reviewMode,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label          = "modePanel",
                    modifier       = Modifier.fillMaxWidth().weight(1f),
                ) { mode ->
                    when (mode) {
                        ReviewMode.NAVIGATE -> NavigateModePanel(state, vm, onViewReport = { onViewReport(gameId) }, modifier = Modifier.fillMaxWidth())
                        ReviewMode.ANALYSE  -> AnalysePanel(state, vm, modifier = Modifier.fillMaxWidth())
                        ReviewMode.MENTOR   -> MentorModePanel(state, vm, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            PredictionGate(
                visible     = state.showPredictionGate,
                whitePlayer = state.game?.whitePlayer ?: "",
                blackPlayer = state.game?.blackPlayer ?: "",
                result      = state.game?.result ?: "",
                onSubmit    = vm::submitPrediction,
                onSkip      = vm::skipPrediction,
                modifier    = Modifier.fillMaxSize(),
            )
        }
    }
}
