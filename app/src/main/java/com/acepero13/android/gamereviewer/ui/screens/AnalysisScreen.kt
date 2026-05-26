package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.components.BlunderReflectionPanel
import com.acepero13.android.gamereviewer.ui.components.CriticalMomentSheet
import com.acepero13.android.gamereviewer.ui.components.GuidedDiscoveryPanel
import com.acepero13.chess.core.ui.board.ChessBoard
import com.acepero13.chess.core.ui.components.MoveTree
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.WCDark
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Main game review screen.
 *
 * ## Milestone 1 — Human-First principles enforced
 * - No EvalBar shown.
 * - No engine lines shown.
 * - Non-judgmental opening/phase header.
 * - Background analysis progress visible only as a tiny spinner (no content).
 *
 * ## Milestone 2 — Editor mode
 * - Board in editor mode: drag-to-arrow, tap-to-mark.
 * - Move comment field persisted per position.
 * - "Mark Critical" → questionnaire BottomSheet.
 * - "Explore" → Sandbox mode with engine auto-reply.
 *
 * ## Milestone 3 — Insight Reconciliation
 * - Task 3.1 — Blunder Guard: red border flash + reflection panel blocks further play.
 * - Task 3.2 — Missed Moment banner triggers Task 3.3 on tap.
 * - Task 3.3 — Guided Discovery: navigation frozen, targeted questions, hint → answer reveal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    gameId: Long,
    onBack: () -> Unit,
    onViewReport: (Long) -> Unit = {},
    vm: AnalysisViewModel = koinViewModel(parameters = { parametersOf(gameId) }),
) {
    val state      by vm.uiState.collectAsState()
    val sheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope       = rememberCoroutineScope()
    val snackbar    = remember { SnackbarHostState() }

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

    Scaffold(
        containerColor = WCDark,
        snackbarHost   = { SnackbarHost(snackbar) },
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
                            Text(
                                state.openingSummary,
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
                colors  = TopAppBarDefaults.topAppBarColors(containerColor = WCDark),
                actions = {
                    // Guided discovery lock icon
                    if (state.guidedDiscoveryMode) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = "Navigation frozen",
                            tint     = ChessGold,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(20.dp),
                        )
                    }
                    // "View Report" button — only after background analysis is complete
                    if (state.isBackgroundAnalysisDone) {
                        IconButton(onClick = { onViewReport(gameId) }) {
                            Icon(
                                Icons.Outlined.Assessment,
                                contentDescription = "View full report",
                                tint = ChessGold,
                            )
                        }
                    }
                    // Silent background analysis progress dot
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            // ── Missed Moment banner (Task 3.2) ────────────────────────────────
            // Hidden while guided discovery is already active
            if (!state.guidedDiscoveryMode) {
                MissedMomentBanner(
                    visible   = state.showMissedMomentBanner,
                    onReview  = vm::reviewMissedMoment,
                    onDismiss = vm::dismissMissedMomentBanner,
                )
            }

            // ── Chess board (with Blunder Guard border flash) ──────────────────
            BoardWithBlunderFlash(
                state    = state,
                onArrow  = { f, t -> vm.onArrowDrawn(f, t) },
                onMark   = { sq -> vm.onSquareMarked(sq) },
                onTap    = { sq ->
                    if (state.sandboxMode) vm.onSandboxSquareTap(sq)
                },
            )

            // Sandbox engine-thinking stripe
            if (state.sandboxEngineThinking) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .border(0.dp, ChessGold.copy(alpha = 0.6f), RoundedCornerShape(2.dp)),
                )
            }

            // ═══════════════════════════════════════════════════════════════════
            // BODY — switches between three modes
            // ═══════════════════════════════════════════════════════════════════

            when {
                // ── Task 3.3: Guided Discovery (navigation frozen) ─────────────
                state.guidedDiscoveryMode -> {
                    val insight = state.guidedDiscoveryInsight
                    if (insight != null) {
                        GuidedDiscoveryPanel(
                            insight          = insight,
                            thoughts         = state.guidedDiscoveryThoughts,
                            hintVisible      = state.guidedDiscoveryHintVisible,
                            answerRevealed   = state.guidedDiscoveryAnswerRevealed,
                            engineThinking   = state.guidedDiscoveryEngineThinking,
                            revealedEvalCp   = state.guidedDiscoveryRevealedEvalCp,
                            onThoughtsChange = vm::updateGuidedThoughts,
                            onRevealHint     = vm::revealGuidedHint,
                            onRevealAnswer   = vm::revealGuidedAnswer,
                            onSubmit         = vm::submitGuidedThoughts,
                            onExit           = vm::exitGuidedDiscovery,
                            modifier         = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        )
                    }
                }

                // ── Sandbox mode (Milestone 2 + Task 3.1 Blunder Guard) ────────
                state.sandboxMode -> {
                    // Blunder Guard reflection panel (Task 3.1)
                    if (state.blunderReflectionMode) {
                        val insight = state.blunderReflectionInsight
                        if (insight != null) {
                            BlunderReflectionPanel(
                                insight    = insight,
                                cpLoss     = state.blunderCpLoss,
                                onRetry    = vm::retryAfterBlunder,
                                onContinue = vm::continueAfterBlunder,
                                modifier   = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    // Exit sandbox button
                    FilledTonalButton(
                        onClick  = vm::exitSandboxMode,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Exit Sandbox") }
                }

                // ── Normal review mode ─────────────────────────────────────────
                else -> {
                    // Navigation row
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = vm::goToStart) {
                            Icon(Icons.Outlined.FastRewind,   "Start",    tint = ChessGold)
                        }
                        IconButton(onClick = vm::stepBackward) {
                            Icon(Icons.Outlined.SkipPrevious, "Previous", tint = ChessGold)
                        }
                        Text(
                            "${state.moveIndex} / ${state.totalMoves}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        IconButton(onClick = vm::stepForward) {
                            Icon(Icons.Outlined.SkipNext,     "Next",     tint = ChessGold)
                        }
                        IconButton(onClick = vm::goToEnd) {
                            Icon(Icons.Outlined.FastForward,  "End",      tint = ChessGold)
                        }
                    }

                    // Action bar (Mark Critical / Explore Sandbox)
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilledTonalButton(
                            onClick  = vm::markCurrentAsCritical,
                            modifier = Modifier.weight(1f),
                            enabled  = state.moveIndex > 0,
                        ) {
                            Icon(Icons.Outlined.Flag, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Mark Critical")
                        }
                        FilledTonalButton(
                            onClick  = vm::enterSandboxMode,
                            modifier = Modifier.weight(1f),
                            enabled  = state.moveIndex > 0,
                        ) {
                            Icon(Icons.Outlined.Lightbulb, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Explore")
                        }
                    }

                    // Move comment field
                    OutlinedTextField(
                        value         = state.currentComment,
                        onValueChange = { vm.updateMoveComment(it) },
                        label         = { Text("Move comment") },
                        placeholder   = {
                            Text(
                                "What were you thinking here?",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                    )

                    // MoveTree
                    MoveTree(
                        entries     = state.treeItems,
                        onNodeClick = vm::onMoveNodeClick,
                        modifier    = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Sub-composables
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Wraps [ChessBoard] with a pulsing red border when the Blunder Guard is active (Task 3.1).
 */
@Composable
private fun BoardWithBlunderFlash(
    state:   AnalysisUiState,
    onArrow: (com.github.bhlangonijr.chesslib.Square, com.github.bhlangonijr.chesslib.Square) -> Unit,
    onMark:  (com.github.bhlangonijr.chesslib.Square) -> Unit,
    onTap:   (com.github.bhlangonijr.chesslib.Square) -> Unit,
) {
    // Blunder border pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "blunderFlash")
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue   = if (state.blunderGuardActive) 0.2f else 0f,
        targetValue    = if (state.blunderGuardActive) 0.9f else 0f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blunderAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .border(
                width  = if (state.blunderGuardActive) 4.dp else 0.dp,
                color  = MaterialTheme.colorScheme.error.copy(alpha = flashAlpha),
                shape  = RoundedCornerShape(4.dp),
            ),
    ) {
        ChessBoard(
            boardState     = state.boardState,
            onSquareTap    = onTap,
            onArrowDrawn   = if (!state.sandboxMode && !state.guidedDiscoveryMode) onArrow else null,
            onSquareMarked = if (!state.sandboxMode && !state.guidedDiscoveryMode) onMark  else null,
            modifier       = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Non-intrusive "Review suggestion available" banner (Task 3.2).
 * Tapping "Review" activates the Guided Discovery panel (Task 3.3).
 */
@Composable
private fun MissedMomentBanner(
    visible:   Boolean,
    onReview:  () -> Unit,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn() + slideInVertically { -it },
        exit    = fadeOut() + slideOutVertically { -it },
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
            shape = RoundedCornerShape(8.dp),
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier           = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text     = "Review suggestion available",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onReview) {
                    Text(
                        "Review",
                        color      = MaterialTheme.colorScheme.onTertiaryContainer,
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        "×",
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}
