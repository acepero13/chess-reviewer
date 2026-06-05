package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiObjects
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.InsightReconciler
import com.acepero13.android.gamereviewer.ui.components.AnalysePanel
import com.acepero13.android.gamereviewer.ui.components.CriticalMomentSheet
import com.acepero13.android.gamereviewer.ui.components.MentorPanel
import com.acepero13.android.gamereviewer.ui.components.MentorPivotalMomentsPanel
import com.acepero13.android.gamereviewer.ui.components.NavigateModeContent
import com.acepero13.android.gamereviewer.ui.components.OpeningDeviationPanel
import com.acepero13.android.gamereviewer.ui.components.PositionCoachCard
import com.acepero13.android.gamereviewer.domain.CoachingTrigger
import com.acepero13.android.gamereviewer.ui.components.CalibrationPanel
import com.acepero13.android.gamereviewer.ui.components.ProactiveCoachingPanel
import com.acepero13.android.gamereviewer.ui.components.GameStoryCard
import com.acepero13.android.gamereviewer.ui.components.PostGameDebrief
import com.acepero13.android.gamereviewer.ui.components.PredictionGate
import com.acepero13.android.gamereviewer.ui.components.DevCoachPromptSheet
import com.acepero13.android.gamereviewer.ui.components.StatsSheet
import com.acepero13.chess.core.ui.board.ChessBoard
import com.acepero13.chess.core.ui.components.EvalBar
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

// ── Design tokens ────────────────────────────────────────────────────────────
private val BtnActive        = ChessGold
private val BtnSelectedBg    = Color(0x1FC9A84C)   // ChessGold @ 12% alpha
private val BannerBorder     = Color(0xFFC9A84C)   // ChessGold solid

/**
 * Main game review screen — three-mode design.
 *
 * ## Navigate mode (default)
 * Move-by-move navigation with persistent bottom bar buttons:
 * [🚩 Critical] [🔍 Analyse] [📊 Stats]
 *
 * ## Analyse mode
 * Engine toggles (Eval Bar / Best Move), three sub-modes:
 * - VIEW   → MoveTree with annotations visible
 * - EDIT   → Color pickers + comment field + MoveTree
 * - EXPLORE→ Sandbox play with Blunder Guard
 *
 * ## Mentor mode
 * Guided question-and-answer panel. Navigation frozen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    gameId:               Long,
    onBack:               () -> Unit,
    onViewReport:         (Long) -> Unit = {},
    initialMoveIndex:     Int? = null,
    onInitialMoveConsumed: () -> Unit = {},
    vm:                   AnalysisViewModel = koinViewModel(parameters = { parametersOf(gameId) }),
) {
    val state    by vm.uiState.collectAsState()

    // Intercept back button in sub-modes so it returns to Navigate instead of exiting the screen.
    BackHandler(enabled = state.reviewMode != ReviewMode.NAVIGATE) {
        when (state.reviewMode) {
            ReviewMode.ANALYSE -> vm.setReviewMode(ReviewMode.NAVIGATE)
            ReviewMode.MENTOR  -> vm.exitMentorMode()
            else               -> {}
        }
    }

    LaunchedEffect(initialMoveIndex) {
        if (initialMoveIndex != null) {
            vm.goToMove(initialMoveIndex)
            onInitialMoveConsumed()
        }
    }
    val sheetState    = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val devSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope    = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context  = LocalContext.current

    var showDevSheet   by remember { mutableStateOf(false) }
    var devPromptText  by remember { mutableStateOf("") }

    if (showDevSheet && devPromptText.isNotBlank()) {
        DevCoachPromptSheet(
            dataSection = devPromptText,
            sheetState  = devSheetState,
            onCopy = { text ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Coach LLM Prompt", text))
                scope.launch { snackbar.showSnackbar("Prompt copied to clipboard") }
            },
            onDismiss = {
                showDevSheet = false
                scope.launch { devSheetState.hide() }
            },
        )
    }

    // ── Stats bottom-sheet ─────────────────────────────────────────────────────
    if (state.showStatsSheet) {
        StatsSheet(
            game        = state.game,
            highlights  = state.gameHighlights,
            playerStats = state.playerStats,
            onDismiss   = vm::dismissStatsSheet,
            onNavigate  = { idx -> vm.dismissStatsSheet(); vm.onMoveNodeClick(idx.toLong()) },
        )
    }

    // ── Critical Moment questionnaire bottom-sheet ─────────────────────────────
    if (state.showCriticalSheet) {
        CriticalMomentSheet(
            sheetState = sheetState,
            onSubmit   = { plan, threats, candidates ->
                vm.saveCriticalAnswers(plan, threats, candidates)
                scope.launch { sheetState.hide() }
            },
            onDismiss  = {
                vm.dismissCriticalSheet()
                scope.launch { sheetState.hide() }
            },
        )
    }

    val devPanelActive = state.developerModeEnabled &&
        (state.showProactiveCoaching || state.guidedDiscoveryMode ||
         state.showMiddlegamePlanPanel || state.showEndgameRecognitionPanel)

    val hasCoachContent = state.showProactiveCoaching ||
                          state.showCalibrationPanel  ||
                          state.guidedDiscoveryMode   ||
                          state.reviewMode == ReviewMode.MENTOR ||
                          state.showOpeningDeviationPanel ||
                          state.showEndgameRecognitionPanel ||
                          state.showMiddlegamePlanPanel

    // Smoothly shrink the board to 65 % width when coach / mentor content is active so the
    // position stays visible while the question panel below has room to scroll.
    val boardFraction by animateFloatAsState(
        targetValue   = if (hasCoachContent) 0.65f else 1f,
        animationSpec = tween(300),
        label         = "boardFraction",
    )

    val appColors = LocalAppColors.current

    Scaffold(
        containerColor = appColors.background,
        snackbarHost   = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (devPanelActive) {
                SmallFloatingActionButton(
                    onClick = {
                        val prompt = vm.buildCoachEvalPrompt()
                        if (prompt != null) {
                            devPromptText = prompt
                            showDevSheet  = true
                        }
                    },
                    containerColor = Color(0xFF1F3A2A),
                    contentColor   = Color(0xFF4ADE80),
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.BugReport,
                        contentDescription = "Copy LLM coach accuracy prompt",
                        modifier           = Modifier.size(18.dp),
                    )
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        state.game?.let { g ->
                            Text(
                                "${g.whitePlayer} vs ${g.blackPlayer}",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = ChessGold,
                            )
                        }
                        if (state.openingSummary.isNotEmpty()) {
                            val deviation = state.openingDeviation
                            val summaryText = if (deviation != null)
                                "${state.openingSummary}  ·  left book: ${deviation.moveLabel}"
                            else
                                state.openingSummary
                            Text(
                                text      = summaryText,
                                style     = MaterialTheme.typography.bodySmall,
                                color     = if (deviation != null) ChessGold.copy(alpha = 0.8f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier  = if (deviation != null)
                                    Modifier.clickable { vm.goToMove(deviation.moveIndex) }
                                else Modifier,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, "Back", tint = ChessGold)
                    }
                },
                colors  = TopAppBarDefaults.topAppBarColors(containerColor = appColors.background),
                actions = {
                    if (state.reviewMode == ReviewMode.MENTOR) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = "Navigation frozen",
                            tint     = ChessGold,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(20.dp),
                        )
                    }
                    if (state.isBackgroundAnalysisDone) {
                        IconButton(onClick = { onViewReport(gameId) }) {
                            Icon(
                                Icons.Outlined.Assessment,
                                contentDescription = "View full report",
                                tint = ChessGold,
                            )
                        }
                    }
                    if (!state.isBackgroundAnalysisDone && state.backgroundAnalysisProgress > 0f) {
                        CircularProgressIndicator(
                            progress    = { state.backgroundAnalysisProgress },
                            modifier    = Modifier
                                .padding(end = 12.dp)
                                .size(18.dp),
                            strokeWidth = 2.dp,
                            color       = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        bottomBar = { AnalysisBottomBar(state = state, vm = vm, snackbar = snackbar, scope = scope) },
    ) { padding ->
        if (state.game == null) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ChessGold)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Loading game…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            // ── Missed Moment banner (Task 3.2) ────────────────────────────────
            if (state.reviewMode != ReviewMode.MENTOR) {
                MissedMomentBanner(
                    visible   = state.showMissedMomentBanner,
                    onReview  = vm::reviewMissedMoment,
                    onDismiss = vm::dismissMissedMomentBanner,
                )
            }

            // ── Mentor context banner — tells user which decision point they're at ──
            AnimatedVisibility(
                visible = state.reviewMode == ReviewMode.MENTOR && state.mentorContextLabel.isNotBlank(),
                enter   = fadeIn() + slideInVertically { -it },
                exit    = fadeOut() + slideOutVertically { -it },
            ) {
                MentorContextBanner(label = state.mentorContextLabel)
            }

            // ── EvalBar — only in Analyse mode when toggled on ─────────────────
            AnimatedVisibility(
                visible = state.reviewMode == ReviewMode.ANALYSE && state.evalBarVisible,
                enter   = fadeIn(),
                exit    = fadeOut(),
            ) {
                EvalBar(
                    evalCp   = state.currentEvalCp,
                    thinking = state.sandboxEngineThinking || state.guidedDiscoveryEngineThinking,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ── Chess board (with Blunder Guard border flash) ──────────────────
            // Shrinks to 65 % width when coach / mentor content is active so the user
            // can still read the position while the question panel below scrolls.
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BoardWithBlunderFlash(
                    state           = state,
                    onArrow         = { f, t -> vm.onArrowDrawn(f, t) },
                    onMark          = { sq -> vm.onSquareMarked(sq) },
                    onTap           = { sq ->
                        if (state.reviewMode == ReviewMode.ANALYSE &&
                            state.analyseSubMode == AnalyseSubMode.EXPLORE) {
                            vm.onSandboxSquareTap(sq)
                        }
                    },
                    onMentorTap     = { sq -> vm.onMentorSquareTap(sq) },
                    onProactiveTap  = { sq -> vm.answerProactiveQuestion(sq) },
                    modifier        = Modifier.fillMaxWidth(boardFraction),
                )
            }

            // Sandbox engine-thinking stripe
            if (state.sandboxEngineThinking &&
                state.reviewMode == ReviewMode.ANALYSE &&
                state.analyseSubMode == AnalyseSubMode.EXPLORE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .border(0.dp, ChessGold.copy(alpha = 0.6f), RoundedCornerShape(2.dp)),
                )
            }

            // ── Navigation controls (always visible, disabled in MENTOR) ───────
            NavigationControls(state = state, vm = vm, snackbar = snackbar, scope = scope)

            // Proactive coaching panel — outside AnimatedContent so it survives mode transitions
            // (e.g. forcing-sequence animation enters sandbox/ANALYSE mode while panel stays visible)
            if (!state.showOpeningDeviationPanel && !state.showEndgameRecognitionPanel && !state.showMiddlegamePlanPanel) {
                state.activeProactiveTrigger?.let { trigger ->
                    ProactiveCoachingPanel(
                        trigger                   = trigger,
                        visible                   = state.showProactiveCoaching,
                        isWeakArea                = trigger.typeName() in state.weakTriggerTypes,
                        onDismiss                 = vm::dismissProactiveCoaching,
                        onStartInteraction        = vm::startProactiveInteraction,
                        proactiveInteractiveMode  = state.proactiveInteractiveMode,
                        proactiveAnswerFeedback   = state.proactiveAnswerFeedback,
                        proactiveAnswerIsCorrect  = state.proactiveAnswerIsCorrect,
                        proactiveFoundCount       = state.proactiveFoundSquares.size,
                        proactiveTotalCount       = state.proactiveHangingSquares.size,
                        coordinationQuizPhase     = state.coordinationQuizPhase,
                        onCoordinationReveal      = vm::onCoordinationQuizReveal,
                        onTryForcingSequence      = vm::enterForcingSequenceMode,
                        onShowForcingSequence     = vm::showForcingSequence,
                        onReplayForcingSequence   = vm::replayForcingSequence,
                        forcingSequenceMode       = state.forcingSequenceMode,
                        forcingSequenceAnimating  = state.forcingSequenceAnimating,
                        forcingSequenceComplete   = state.forcingSequenceComplete,
                        forcingSequenceCurrentStep = state.forcingSequenceCurrentStep,
                        forcingSequenceTotalSteps  = state.forcingSequencePvMoves.size,
                        modifier                  = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Calibration quiz panel — shown when an EvalCalibration trigger fires
            state.calibrationTrigger?.let { trigger ->
                CalibrationPanel(
                    trigger          = trigger,
                    visible          = state.showCalibrationPanel,
                    selectedValue    = state.calibrationUserValue,
                    locked           = state.calibrationLocked,
                    feedback         = state.calibrationFeedback,
                    feedbackPositive = state.calibrationFeedbackPositive,
                    onValueChange    = vm::onCalibrationValueChange,
                    onLockIn         = vm::lockInCalibration,
                    onDismiss        = vm::dismissCalibration,
                    modifier         = Modifier.fillMaxWidth(),
                )
            }

            // Forcing sequence banner — shown in sandbox explore mode (try / animating / complete)
            if (state.forcingSequenceMode && state.analyseSubMode == AnalyseSubMode.EXPLORE) {
                ForcingSequenceBanner(
                    animating  = state.forcingSequenceAnimating,
                    complete   = state.forcingSequenceComplete,
                    currentStep = state.forcingSequenceCurrentStep,
                    totalSteps  = state.forcingSequencePvMoves.size,
                    onGiveUp    = vm::showForcingSequence,
                    onReplay    = vm::replayForcingSequence,
                    onDone      = vm::exitForcingSequenceMode,
                    modifier    = Modifier.fillMaxWidth(),
                )
            }

            // ── Mode-specific content panel ────────────────────────────────────
            AnimatedContent(
                targetState    = state.reviewMode,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label          = "modePanel",
                modifier       = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { mode ->
                when (mode) {
                    ReviewMode.NAVIGATE -> Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Game story headline — dismissable narrative summary
                        GameStoryCard(
                            story     = state.gameStory,
                            visible   = state.gameStoryUnlocked && !state.gameStoryDismissed,
                            onDismiss = vm::dismissGameStory,
                            modifier  = Modifier.fillMaxWidth(),
                        )

                        // Post-game debrief — prediction vs engine findings (shown at end of game)
                        PostGameDebrief(
                            visible      = state.showPostGameDebrief,
                            prediction   = state.gamePrediction,
                            matchResult  = state.predictionMatchResult,
                            onDismiss    = vm::dismissPostGameDebrief,
                            onViewReport = { onViewReport(gameId) },
                            modifier     = Modifier.fillMaxWidth(),
                        )

                        // Opening Theory Coach panel — shown at the exact deviation move
                        if (state.showOpeningDeviationPanel) {
                            state.openingDeviation?.let { deviation ->
                                OpeningDeviationPanel(
                                    deviation  = deviation,
                                    insight    = InsightReconciler.forReason(
                                        com.acepero13.android.gamereviewer.data.model.CriticalMoment.ReasonCategory.OPENING_DEVIATION
                                    ),
                                    onContinue = vm::dismissOpeningDeviationPanel,
                                    modifier   = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        // Endgame Recognition Coach panel — shown at the first endgame move
                        if (state.showEndgameRecognitionPanel) {
                            state.endgameClassification?.let { classification ->
                                com.acepero13.android.gamereviewer.ui.components.EndgameRecognitionPanel(
                                    classification = classification,
                                    insight        = InsightReconciler.forEndgame(
                                        chapter = classification.entry.chapter,
                                        name    = classification.entry.name,
                                    ),
                                    onContinue     = vm::dismissEndgameRecognitionPanel,
                                    modifier       = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        // Middlegame Plan Coach panel — shown at the first out-of-book move
                        if (state.showMiddlegamePlanPanel) {
                            state.middlegamePlanClassification?.let { classification ->
                                val insights = classification.plans.map { plan ->
                                    InsightReconciler.forMiddlegamePlan(plan)
                                }
                                com.acepero13.android.gamereviewer.ui.components.MiddlegamePlanPanel(
                                    classification = classification,
                                    insights       = insights,
                                    onContinue     = vm::dismissMiddlegamePlanPanel,
                                    modifier       = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 320.dp),
                                )
                            }
                        }

                        // Structured analysis prompt (position coach) — only when:
                        //   • setting is enabled
                        //   • a highlight exists at this move
                        //   • proactive coaching panel is not already open
                        //   • user hasn't dismissed it for this position this session
                        if (state.positionCoachEnabled && !state.showProactiveCoaching &&
                            !state.showOpeningDeviationPanel && !state.showEndgameRecognitionPanel && !state.showMiddlegamePlanPanel) {
                            val highlight = remember(state.moveIndex, state.gameHighlights) {
                                state.gameHighlights.firstOrNull { it.moveIndex == state.moveIndex }
                            }
                            if (highlight != null && state.moveIndex !in state.positionCoachDismissedMoves) {
                                PositionCoachCard(
                                    moveIndex = state.moveIndex,
                                    highlight = highlight,
                                    onDismiss = vm::dismissPositionCoach,
                                    modifier  = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        NavigateModeContent(
                            entries        = state.treeItems,
                            onNodeClick    = vm::onMoveNodeClick,
                            currentComment = state.currentComment,
                            isOverthougt   = state.overthougtMoveIndices.contains(state.moveIndex),
                            modifier       = Modifier.fillMaxWidth().weight(1f),
                        )
                    }
                    ReviewMode.ANALYSE  -> AnalysePanel(
                        state    = state,
                        vm       = vm,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ReviewMode.MENTOR   -> Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            when {
                                state.showPivotalMomentsPanel -> {
                                    // ── Big Three overview panel ───────────────────
                                    state.pivotalMoments?.let { moments ->
                                        MentorPivotalMomentsPanel(
                                            moments  = moments,
                                            onReview = vm::reviewPivotalMoment,
                                            onSkip   = vm::dismissPivotalMomentsPanel,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                                state.showReflectionMode -> {
                                    // ── Board Scan Reflection Mode ─────────────────────
                                    BoardScanReflectionPanel(
                                        items     = state.reflectionItems,
                                        onAnswer  = vm::answerReflection,
                                        onFinish  = vm::exitReflectionMode,
                                        modifier  = Modifier.fillMaxWidth(),
                                    )
                                }
                                else -> {
                                    // ── Normal Mentor panel ────────────────────────────
                                    // Coach's Briefing — cross-game weakness context shown at session start
                                    AnimatedVisibility(
                                        visible = state.showCoachsBriefing && state.weaknessContext != null,
                                        enter   = fadeIn(tween(200)) + expandVertically(),
                                        exit    = fadeOut(tween(150)) + shrinkVertically(),
                                    ) {
                                        state.weaknessContext?.let { ctx ->
                                            CoachsBriefingCard(
                                                ctx       = ctx,
                                                onDismiss = vm::dismissCoachsBriefing,
                                                modifier  = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp),
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = appColors.border, thickness = 1.dp)
                                    if (state.mentorSessionQueue.isNotEmpty()) {
                                        val weaknessDotPositions: Set<Int> = run {
                                            val matchingIndices = state.weaknessContext
                                                ?.matchingMoveIndices?.toSet() ?: emptySet()
                                            state.mentorSessionQueue
                                                .mapIndexedNotNull { pos, moveIdx ->
                                                    if (moveIdx in matchingIndices) pos + 1 else null
                                                }
                                                .toSet()
                                        }
                                        MentorSessionProgressHeader(
                                            current              = state.mentorSessionIdx + 1,
                                            total                = state.mentorSessionQueue.size,
                                            modifier             = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 6.dp),
                                            weaknessDotPositions = weaknessDotPositions,
                                        )
                                    }
                                    val isRecurring = state.weaknessContext
                                        ?.matchingMoveIndices
                                        ?.contains(state.guidedDiscoveryCriticalMoment?.moveIndex) == true
                                    MentorPanel(
                                        state              = state,
                                        vm                 = vm,
                                        isRecurringPattern = isRecurring,
                                        modifier           = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        // ── Pre-game prediction gate — overlays everything until dismissed ────────
        PredictionGate(
            visible     = state.showPredictionGate,
            whitePlayer = state.game?.whitePlayer ?: "",
            blackPlayer = state.game?.blackPlayer ?: "",
            result      = state.game?.result ?: "*",
            onSubmit    = vm::submitPrediction,
            onSkip      = vm::skipPrediction,
            modifier    = Modifier.fillMaxSize(),
        )
        }
    }   // closes content Box
}       // closes Scaffold content lambda
}       // closes AnalysisScreen

// ═══════════════════════════════════════════════════════════════════════════════
// Board Scan Reflection Mode panel
// ═══════════════════════════════════════════════════════════════════════════════

private val ReflectionBorder = Color(0xFF2D6A4F)
private val CorrectGreen     = Color(0xFF4CAF50)
private val WrongAmber       = Color(0xFFF0A500)

// All coaching pattern display labels in canonical order (must match CoachingTrigger.ALL_LABELS)
private val REFLECTION_LABELS = listOf(
    "Safety Issue", "Multiple Plans", "Restricted Piece",
    "Forcing Move", "Opponent's Plan", "Pre-Move Check", "Rook Activation",
    "Conversion Strategy",
)

@Composable
private fun BoardScanReflectionPanel(
    items:    List<ReflectionItem>,
    onAnswer: (moveIndex: Int, answer: String) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val answered = items.count { it.userAnswer != null }
    val correct  = items.count { it.userAnswer == it.correctLabel }

    val appColors = LocalAppColors.current
    Column(
        modifier            = modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text       = "Board Scan Review",
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = appColors.textPrimary,
        )
        Text(
            text  = "You navigated past these patterns. Can you name each one?",
            style = MaterialTheme.typography.bodySmall,
            color = appColors.textSecondary,
        )
        HorizontalDivider(color = ReflectionBorder.copy(alpha = 0.4f))

        Column(
            modifier            = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items.forEach { item ->
                ReflectionItemCard(
                    item     = item,
                    onAnswer = { answer -> onAnswer(item.moveIndex, answer) },
                )
            }
        }

        if (answered == items.size) {
            HorizontalDivider(color = ReflectionBorder.copy(alpha = 0.4f))
            Text(
                text       = "Board Scan score: $correct / ${items.size}",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = if (correct == items.size) CorrectGreen else ChessGold,
            )
        }
        Button(
            onClick  = onFinish,
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(8.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = ReflectionBorder),
        ) {
            Text(
                text       = if (answered < items.size) "Skip reflection" else "Finish session",
                fontWeight = FontWeight.SemiBold,
                color      = Color.White,
            )
        }
    }
}

@Composable
private fun ReflectionItemCard(
    item:     ReflectionItem,
    onAnswer: (String) -> Unit,
) {
    val appColors = LocalAppColors.current
    Card(
        shape  = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.surface),
        border = BorderStroke(1.dp, ReflectionBorder),
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text       = "Move ${(item.moveIndex + 1) / 2} — what coaching pattern was here?",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color      = appColors.textPrimary,
            )
            if (item.userAnswer == null) {
                REFLECTION_LABELS.chunked(2).forEach { row ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        row.forEach { label ->
                            OutlinedButton(
                                onClick  = { onAnswer(label) },
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(6.dp),
                                border   = BorderStroke(1.dp, ReflectionBorder),
                            ) {
                                Text(
                                    text  = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = appColors.textPrimary,
                                )
                            }
                        }
                        // Pad odd-count rows
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                val isCorrect = item.userAnswer == item.correctLabel
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text  = if (isCorrect) "Correct" else "Not quite",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCorrect) CorrectGreen else WrongAmber,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text  = "— you said: ${item.userAnswer}",
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.textSecondary,
                    )
                }
                if (!isCorrect) {
                    Text(
                        text  = "Answer: ${item.correctLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CorrectGreen,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Lichess-style bottom action bar
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Persistent bottom bar inspired by the Lichess analysis toolbar.
 *
 * Each action is rendered as a flat icon-above-label column button.
 * The active mode / sub-mode is highlighted with [ChessGold] tint and a
 * subtle background. A thin separator line divides the bar from the board area.
 *
 * - **NAVIGATE**: [🚩 Critical] [🔍 Analyse] [📊 Stats]
 * - **ANALYSE**:  [✏ Edit] [▶ Explore]  (back via system back / top-bar arrow)
 * - **MENTOR**:   [← Return …] (full-width)
 */
@Composable
private fun AnalysisBottomBar(
    state:    AnalysisUiState,
    vm:       AnalysisViewModel,
    snackbar: SnackbarHostState,
    scope:    kotlinx.coroutines.CoroutineScope,
) {
    val appColors = LocalAppColors.current
    Surface(color = appColors.surface, tonalElevation = 0.dp) {
        Column(modifier = Modifier.animateContentSize(tween(200))) {
            // Top separator — Lichess signature thin rule
            HorizontalDivider(color = appColors.border, thickness = 0.5.dp)

            AnimatedContent(
                targetState    = state.reviewMode,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label          = "bottomBar",
            ) { mode -> when (mode) {

                // ── Navigate ───────────────────────────────────────────────────
                ReviewMode.NAVIGATE -> {
                    val analysisReady = state.isBackgroundAnalysisDone
                    val onNotReady: () -> Unit = {
                        scope.launch {
                            snackbar.showSnackbar("Analysis is still running — results will be ready shortly")
                        }
                    }
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        BottomBarButton(
                            icon    = Icons.Outlined.Flag,
                            label   = "Critical",
                            onClick = vm::markCurrentAsCritical,
                            enabled = state.moveIndex > 0,
                        )
                        BottomBarButton(
                            icon    = Icons.Outlined.Search,
                            label   = "Analyse",
                            onClick = vm::enterAnalyseMode,
                        )
                        BottomBarButton(
                            icon            = Icons.Outlined.BarChart,
                            label           = "Stats",
                            onClick         = vm::toggleStatsSheet,
                            enabled         = analysisReady,
                            onDisabledClick = if (!analysisReady) onNotReady else null,
                        )
                        // ── Coach Lamp ────────────────────────────────────────────
                        // Tier-1 triggers (Safety, ForcingMove, PreMoveChecklist,
                        // ImpulseControl) are backed by a concrete eval loss and always
                        // light the lamp — no recency suppression.
                        // Lower-tier positional/structural habits (WorstPiece, Rook,
                        // CandidateMoves …) use a same-side recency window (step by 2
                        // to skip opponent moves) to avoid repetitive alerts.
                        val currentTrigger     = state.triggersByMove[state.moveIndex]?.firstOrNull()
                        val currentTriggerType = currentTrigger?.typeName()
                        val recentTriggerTypes = (1..3).mapNotNull { step ->
                            state.triggersByMove[state.moveIndex - step * 2]?.firstOrNull()?.typeName()
                        }.toSet()
                        // EvalCalibration is always interactive (never repetitive), so exempt it
                        // from the recency suppression window the same way tier-1 triggers are.
                        val hasActiveTrigger   = currentTrigger != null &&
                            (currentTrigger.tier() == 1 ||
                             currentTrigger is CoachingTrigger.EvalCalibration ||
                             currentTriggerType !in recentTriggerTypes)
                        BottomBarButton(
                            icon            = Icons.Outlined.Lightbulb,
                            label           = "Coach",
                            onClick         = vm::enterProactiveCoaching,
                            enabled         = hasActiveTrigger && analysisReady,
                            selected        = hasActiveTrigger && analysisReady,
                            onDisabledClick = if (!analysisReady) onNotReady else null,
                        )
                        // ── Mentor session entry point ─────────────────────────
                        BottomBarButton(
                            icon            = Icons.Outlined.EmojiObjects,
                            label           = "Mentor",
                            onClick         = vm::enterMentorSession,
                            enabled         = analysisReady &&
                                              state.criticalMoments.any { it.type == "ENGINE_MARKED" },
                            onDisabledClick = if (!analysisReady) onNotReady else null,
                        )
                    }
                }

                // ── Analyse ────────────────────────────────────────────────────
                ReviewMode.ANALYSE -> {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        BottomBarButton(
                            icon    = Icons.Outlined.ArrowBackIosNew,
                            label   = "Navigate",
                            onClick = { vm.setReviewMode(ReviewMode.NAVIGATE) },
                        )
                        BottomBarButton(
                            icon     = Icons.Outlined.Edit,
                            label    = "Edit",
                            selected = state.analyseSubMode == AnalyseSubMode.EDIT,
                            onClick  = {
                                if (state.analyseSubMode == AnalyseSubMode.EDIT)
                                    vm.setAnalyseSubMode(AnalyseSubMode.VIEW)
                                else
                                    vm.setAnalyseSubMode(AnalyseSubMode.EDIT)
                            },
                        )
                        BottomBarButton(
                            icon     = Icons.Outlined.PlayArrow,
                            label    = "Explore",
                            selected = state.analyseSubMode == AnalyseSubMode.EXPLORE,
                            onClick  = {
                                if (state.analyseSubMode == AnalyseSubMode.EXPLORE)
                                    vm.exitSandboxMode()
                                else
                                    vm.enterSandboxMode()
                            },
                        )
                    }
                }

                // ── Mentor — exit + optional "next mistake" buttons ────────────
                ReviewMode.MENTOR -> {
                    val sessionActive = state.mentorSessionQueue.isNotEmpty()
                    val canAdvance    = sessionActive &&
                        state.mentorSessionIdx + 1 < state.mentorSessionQueue.size &&
                        (state.guidedDiscoveryAnswerRevealed ||
                         state.mentorMoveResult == MentorMoveResult.CORRECT ||
                         state.mentorMoveResult == MentorMoveResult.CLOSE)

                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick  = vm::exitMentorMode,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(8.dp),
                        ) {
                            Icon(
                                Icons.Outlined.ArrowBackIosNew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint     = ChessGold,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text  = if (sessionActive) "Exit review" else "Return",
                                color = ChessGold,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (canAdvance) {
                            Button(
                                onClick  = vm::advanceMentorSession,
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(8.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = ChessGold),
                            ) {
                                Text(
                                    "Next mistake",
                                    color      = Color.Black,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint     = Color.Black,
                                )
                            }
                        }
                    }
                }
            } }  // closes when(mode) and AnimatedContent lambda
        }
    }
}

/**
 * A single Lichess-style bottom bar button: icon stacked above a text label.
 *
 * - **Inactive**: grey icon + grey label
 * - **Selected**: [ChessGold] icon + label, with a subtle gold background pill
 * - **Disabled**: dim grey, not clickable
 */
@Composable
private fun BottomBarButton(
    icon:            ImageVector,
    label:           String,
    onClick:         () -> Unit,
    enabled:         Boolean = true,
    selected:        Boolean = false,
    onDisabledClick: (() -> Unit)? = null,
) {
    val appColors = LocalAppColors.current
    val color = when {
        !enabled -> appColors.textDisabled
        selected -> BtnActive
        else     -> appColors.iconSubtle
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) BtnSelectedBg else Color.Transparent)
            .then(
                when {
                    enabled                 -> Modifier.clickable(onClick = onClick)
                    onDisabledClick != null -> Modifier.clickable(onClick = onDisabledClick)
                    else                    -> Modifier
                }
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = color,
            modifier           = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Navigation controls
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Five-button navigation row: [⏮ Start] [⏪ Prev] [move counter] [⏩ Next] [⏭ End].
 *
 * In [ReviewMode.MENTOR] the buttons are visually greyed out and tapping them shows a
 * [SnackbarHostState] message explaining that navigation is frozen instead of silently no-oping.
 */
@Composable
private fun NavigationControls(
    state:    AnalysisUiState,
    vm:       AnalysisViewModel,
    snackbar: SnackbarHostState,
    scope:    kotlinx.coroutines.CoroutineScope,
) {
    val enabled = state.reviewMode != ReviewMode.MENTOR
    val frozenMsg = "Navigation is frozen in Mentor mode"

    fun navClick(action: () -> Unit): () -> Unit =
        if (enabled) action
        else ({ scope.launch { snackbar.showSnackbar(frozenMsg) } })

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        IconButton(onClick = navClick(vm::goToStart)) {
            Icon(Icons.Outlined.FastRewind,   "Start",    tint = if (enabled) ChessGold else ChessGold.copy(alpha = 0.38f))
        }
        IconButton(onClick = navClick(vm::stepBackward)) {
            Icon(Icons.Outlined.SkipPrevious, "Previous", tint = if (enabled) ChessGold else ChessGold.copy(alpha = 0.38f))
        }
        Text(
            "${state.moveIndex} / ${state.totalMoves}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
        IconButton(onClick = navClick(vm::stepForward)) {
            Icon(Icons.Outlined.SkipNext,     "Next",     tint = if (enabled) ChessGold else ChessGold.copy(alpha = 0.38f))
        }
        IconButton(onClick = navClick(vm::goToEnd)) {
            Icon(Icons.Outlined.FastForward,  "End",      tint = if (enabled) ChessGold else ChessGold.copy(alpha = 0.38f))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Board with Blunder Guard flash
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Wraps [ChessBoard] with a pulsing red border when the Blunder Guard is active (Task 3.1).
 */
@Composable
private fun BoardWithBlunderFlash(
    state:          AnalysisUiState,
    onArrow:        (com.github.bhlangonijr.chesslib.Square, com.github.bhlangonijr.chesslib.Square) -> Unit,
    onMark:         (com.github.bhlangonijr.chesslib.Square) -> Unit,
    onTap:          (com.github.bhlangonijr.chesslib.Square) -> Unit,
    onMentorTap:    (com.github.bhlangonijr.chesslib.Square) -> Unit,
    onProactiveTap: (com.github.bhlangonijr.chesslib.Square) -> Unit,
    modifier:       Modifier = Modifier.fillMaxWidth(),
) {
    val inEditMode   = state.reviewMode == ReviewMode.ANALYSE && state.analyseSubMode == AnalyseSubMode.EDIT
    val inMentorMode = state.reviewMode == ReviewMode.MENTOR

    LaunchedEffect(state.reviewMode, state.mentorMoveInputActive) {
        Log.d("MentorTap", "BoardWithBlunderFlash recomposed: reviewMode=${state.reviewMode} inMentorMode=$inMentorMode mentorMoveInputActive=${state.mentorMoveInputActive} isEditorMode=${state.boardState.isEditorMode}")
    }

    // ChessBoard caches the tap lambda internally, so we must use rememberUpdatedState to ensure
    // the stable lambda object always reads the latest mode flags rather than stale closure captures.
    val latestState          by rememberUpdatedState(state)
    val latestOnTap          by rememberUpdatedState(onTap)
    val latestOnMentorTap    by rememberUpdatedState(onMentorTap)
    val latestOnProactiveTap by rememberUpdatedState(onProactiveTap)
    val stableSquareTap = remember {
        { sq: com.github.bhlangonijr.chesslib.Square ->
            val s            = latestState
            val isMentor     = s.reviewMode == ReviewMode.MENTOR
            val isExplore    = s.reviewMode == ReviewMode.ANALYSE && s.analyseSubMode == AnalyseSubMode.EXPLORE
            val isEdit       = s.reviewMode == ReviewMode.ANALYSE && s.analyseSubMode == AnalyseSubMode.EDIT
            Log.d("MentorTap", "onSquareTap fired: sq=$sq | inMentor=$isMentor inExplore=$isExplore inEdit=$isEdit | reviewMode=${s.reviewMode} | mentorMoveInputActive=${s.mentorMoveInputActive} | proactiveInteractive=${s.proactiveInteractiveMode} | isEditorMode=${s.boardState.isEditorMode}")
            when {
                s.proactiveInteractiveMode -> latestOnProactiveTap(sq)
                isExplore                  -> latestOnTap(sq)
                isMentor                   -> latestOnMentorTap(sq)
                else                       -> { Log.d("MentorTap", "  → no-op (none of the modes matched)"); Unit }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "blunderFlash")
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue  = if (state.blunderGuardActive) 0.2f else 0f,
        targetValue   = if (state.blunderGuardActive) 0.9f else 0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blunderAlpha",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(
                width = if (state.blunderGuardActive) 4.dp else 0.dp,
                color = MaterialTheme.colorScheme.error.copy(alpha = flashAlpha),
                shape = RoundedCornerShape(4.dp),
            ),
    ) {
        ChessBoard(
            boardState     = state.boardState.copy(
                markedSquares = state.boardState.markedSquares + state.coachHighlightSquares,
            ),
            onSquareTap    = stableSquareTap,
            onArrowDrawn   = if (inEditMode) onArrow else null,
            onSquareMarked = if (inEditMode) onMark  else null,
            modifier       = Modifier.fillMaxWidth(),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Mentor context banner — tells the user which decision point they're reviewing
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Small amber banner shown when [ReviewMode.MENTOR] navigates the board to a decision point.
 * Displays the move notation so the user understands why the position changed.
 * E.g. "Move 14. Nf6 — what would you play?"
 */
@Composable
private fun MentorContextBanner(label: String) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = ChessGold.copy(alpha = 0.10f)),
        shape    = RoundedCornerShape(8.dp),
        border   = BorderStroke(1.dp, BannerBorder.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector        = Icons.Outlined.Lock,
                contentDescription = null,
                tint               = ChessGold,
                modifier           = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text  = label,
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textPrimary,
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Missed Moment banner — Lichess-style amber hint strip
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Non-intrusive "Review suggestion available" banner (Task 3.2).
 *
 * Styled as an amber-tinted card with a gold border — similar to Lichess's
 * computer-analysis hint banners. Slides in from the top.
 * Tapping "Review" enters Mentor mode at the flagged position (Task 3.3).
 */
@Composable
private fun MissedMomentBanner(
    visible:   Boolean,
    onReview:  () -> Unit,
    onDismiss: () -> Unit,
) {
    val appColors = LocalAppColors.current
    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn() + slideInVertically { -it },
        exit    = fadeOut() + slideOutVertically { -it },
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(containerColor = ChessGold.copy(alpha = 0.10f)),
            shape    = RoundedCornerShape(8.dp),
            border   = BorderStroke(1.dp, BannerBorder.copy(alpha = 0.5f)),
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint               = ChessGold,
                    modifier           = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text     = "Review suggestion available",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = appColors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onReview) {
                    Text(
                        "Review",
                        color      = ChessGold,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(
                    onClick  = onDismiss,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.Close,
                        contentDescription = "Dismiss",
                        tint               = appColors.iconSubtle,
                        modifier           = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Mentor session progress header
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Shows progress dots and a text indicator inside the Mentor bottom sheet when
 * the user is in a structured review session (e.g. "Mistake 2 of 3").
 *
 * Filled dots (●) = completed; outline dots (○) = pending.
 */
@Composable
private fun MentorSessionProgressHeader(
    current:              Int,
    total:                Int,
    modifier:             Modifier = Modifier,
    weaknessDotPositions: Set<Int> = emptySet(),
) {
    Row(
        modifier          = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Dot indicators — amber fill for weakness-matching positions
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 1..total) {
                val done = i < current
                val active = i == current
                val isWeakness = i in weaknessDotPositions
                Text(
                    text  = when {
                        done   -> "●"
                        active -> "◉"
                        else   -> "○"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isWeakness && (done || active) -> Color(0xFFFF8F00)   // amber
                        done || active                 -> ChessGold
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text  = "Mistake $current of $total",
            style = MaterialTheme.typography.labelSmall,
            color = ChessGold,
        )
    }
}

@Composable
private fun CoachsBriefingCard(
    ctx:       WeaknessContext,
    onDismiss: () -> Unit,
    modifier:  Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(10.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, ChessGold.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment      = Alignment.CenterVertically,
                horizontalArrangement  = Arrangement.SpaceBetween,
                modifier               = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(ctx.trendEmoji, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Coach's Reading",
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = ChessGold,
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp),
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                ctx.trendTitle,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = ChessGold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Seen across ${ctx.gamesAffected} of ${ctx.totalGamesAnalyzed} analyzed games",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (ctx.matchingMoveIndices.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "${ctx.matchingMoveIndices.size} moment(s) in this game match this pattern — reviewed first",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF8F00),
                )
            }
        }
    }
}

@Composable
private fun ForcingSequenceBanner(
    animating:   Boolean,
    complete:    Boolean,
    currentStep: Int,
    totalSteps:  Int,
    onGiveUp:    () -> Unit,
    onReplay:    () -> Unit,
    onDone:      () -> Unit,
    modifier:    Modifier = Modifier,
) {
    val panelBg     = Color(0xFF1A2E1A)
    val panelBorder = Color(0xFF2D6A4F)
    val panelText   = Color(0xFFCCE8D9)

    Card(
        shape  = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = panelBg),
        border = BorderStroke(1.dp, panelBorder.copy(alpha = 0.7f)),
        modifier = modifier,
    ) {
        when {
            animating -> {
                val progress = if (totalSteps > 0) currentStep.toFloat() / totalSteps else 0f
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text  = "Watching the forcing sequence… ($currentStep/$totalSteps)",
                        style = MaterialTheme.typography.bodySmall,
                        color = panelText.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold,
                    )
                    LinearProgressIndicator(
                        progress  = { progress },
                        modifier  = Modifier.fillMaxWidth(),
                        color     = com.acepero13.chess.core.ui.theme.AnalyzeBlue,
                        trackColor = panelBorder.copy(alpha = 0.3f),
                    )
                }
            }
            complete -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        text       = "Explore variations freely",
                        style      = MaterialTheme.typography.bodySmall,
                        color      = panelText.copy(alpha = 0.8f),
                        modifier   = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        onClick = onReplay,
                        shape   = RoundedCornerShape(6.dp),
                        border  = BorderStroke(1.dp, com.acepero13.chess.core.ui.theme.AnalyzeBlue.copy(alpha = 0.7f)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text("Replay", style = MaterialTheme.typography.labelSmall, color = com.acepero13.chess.core.ui.theme.AnalyzeBlue)
                    }
                    OutlinedButton(
                        onClick = onDone,
                        shape   = RoundedCornerShape(6.dp),
                        border  = BorderStroke(1.dp, panelBorder.copy(alpha = 0.6f)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text("Done", style = MaterialTheme.typography.labelSmall, color = panelText.copy(alpha = 0.7f))
                    }
                }
            }
            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        text       = "Find the forcing sequence!",
                        style      = MaterialTheme.typography.bodySmall,
                        color      = ChessGold.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedButton(
                        onClick = onGiveUp,
                        shape   = RoundedCornerShape(6.dp),
                        border  = BorderStroke(1.dp, panelBorder.copy(alpha = 0.6f)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text  = "Give up",
                            style = MaterialTheme.typography.labelSmall,
                            color = panelText.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}
