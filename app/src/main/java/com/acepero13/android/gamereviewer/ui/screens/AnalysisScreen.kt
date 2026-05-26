package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Lightbulb
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
import com.acepero13.android.gamereviewer.ui.components.CriticalMomentSheet
import com.acepero13.chess.core.ui.board.ChessBoard
import com.acepero13.chess.core.ui.components.MoveTree
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.WCDark
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Main game review screen — implements the "Human-First, Engine-Second" flow.
 *
 * **What is deliberately absent:**
 * - No EvalBar (hidden until progressive reveal is unlocked in Milestone 4)
 * - No raw engine lines / best move arrows visible during self-analysis
 *
 * **What is present:**
 * - Non-judgmental opening/phase summary
 * - MoveTree for navigation
 * - Editor mode board (arrows, square marks)
 * - Move comment field (loaded from DB annotation)
 * - "Mark as Critical" button → BottomSheet questionnaire
 * - "Review suggestion available" banner for missed critical moments
 * - Sandbox mode with engine response (Milestone 2)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    gameId: Long,
    onBack: () -> Unit,
    vm: AnalysisViewModel = koinViewModel(parameters = { parametersOf(gameId) }),
) {
    val state        by vm.uiState.collectAsState()
    val sheetState   = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope        = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    // Show critical moment bottom-sheet
    if (state.showCriticalSheet) {
        CriticalMomentSheet(
            sheetState = sheetState,
            onSubmit   = { plan, threats, candidates ->
                vm.saveCriticalAnswers(plan, threats, candidates)
                scope.launch { sheetState.hide() }
            },
            onDismiss = {
                vm.dismissCriticalSheet()
                scope.launch { sheetState.hide() }
            },
        )
    }

    Scaffold(
        containerColor = WCDark,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val game = state.game
                        if (game != null) {
                            Text(
                                text       = "${game.whitePlayer} vs ${game.blackPlayer}",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color      = ChessGold,
                            )
                        }
                        if (state.openingSummary.isNotEmpty()) {
                            Text(
                                text  = state.openingSummary,
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
                actions = {
                    // Silent background analysis indicator (only shows when running)
                    if (!state.isBackgroundAnalysisDone && state.backgroundAnalysisProgress > 0f) {
                        CircularProgressIndicator(
                            progress      = { state.backgroundAnalysisProgress },
                            modifier      = Modifier.size(20.dp).padding(end = 4.dp),
                            strokeWidth   = 2.dp,
                            color         = MaterialTheme.colorScheme.onSurfaceVariant,
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

            // ── Missed Moment banner ────────────────────────────────────────
            MissedMomentBanner(
                visible   = state.showMissedMomentBanner,
                onReview  = vm::reviewMissedMoment,
                onDismiss = vm::dismissMissedMomentBanner,
            )

            // ── Blunder Guard banner (sandbox only) ─────────────────────────
            BlunderGuardBanner(
                visible  = state.blunderGuardActive,
                message  = state.blunderGuardMessage,
                onDismiss = vm::dismissBlunderGuard,
            )

            // ── Chess board ─────────────────────────────────────────────────
            if (state.sandboxMode) {
                // Sandbox: user taps squares to move
                Box {
                    ChessBoard(
                        boardState     = state.boardState,
                        onSquareTap    = { sq -> vm.onSandboxSquareTap(sq) },
                        onArrowDrawn   = { from, to -> vm.onArrowDrawn(from, to) },
                        onSquareMarked = { sq -> vm.onSquareMarked(sq) },
                        modifier       = Modifier.fillMaxWidth(),
                    )
                    if (state.sandboxEngineThinking) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .align(Alignment.BottomCenter)
                                .background(ChessGold.copy(alpha = 0.6f)),
                        )
                    }
                }
            } else {
                // Review: editor mode — arrows and square marks only; no piece movement
                ChessBoard(
                    boardState     = state.boardState,
                    onSquareTap    = { /* no selection in review mode */ },
                    onArrowDrawn   = { from, to -> vm.onArrowDrawn(from, to) },
                    onSquareMarked = { sq -> vm.onSquareMarked(sq) },
                    modifier       = Modifier.fillMaxWidth(),
                )
            }

            // ── Navigation controls ─────────────────────────────────────────
            Row(
                modifier                = Modifier.fillMaxWidth(),
                horizontalArrangement   = Arrangement.SpaceEvenly,
                verticalAlignment       = Alignment.CenterVertically,
            ) {
                if (state.sandboxMode) {
                    FilledTonalButton(onClick = vm::exitSandboxMode) {
                        Text("Exit Sandbox")
                    }
                } else {
                    IconButton(onClick = vm::goToStart) {
                        Icon(Icons.Outlined.FastRewind, "Start", tint = ChessGold)
                    }
                    IconButton(onClick = vm::stepBackward) {
                        Icon(Icons.Outlined.SkipPrevious, "Previous", tint = ChessGold)
                    }
                    Text(
                        text  = "${state.moveIndex} / ${state.totalMoves}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(onClick = vm::stepForward) {
                        Icon(Icons.Outlined.SkipNext, "Next", tint = ChessGold)
                    }
                    IconButton(onClick = vm::goToEnd) {
                        Icon(Icons.Outlined.FastForward, "End", tint = ChessGold)
                    }
                }
            }

            // ── Action bar (Mark critical / Enter sandbox) ──────────────────
            if (!state.sandboxMode) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Mark as Critical → BottomSheet questionnaire
                    FilledTonalButton(
                        onClick  = vm::markCurrentAsCritical,
                        modifier = Modifier.weight(1f),
                        enabled  = state.moveIndex > 0,
                    ) {
                        Icon(Icons.Outlined.Flag, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mark Critical")
                    }

                    // Enter sandbox to explore alternatives
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
            }

            // ── Move comment field ─────────────────────────────────────────
            OutlinedTextField(
                value         = state.currentComment,
                onValueChange = { vm.updateMoveComment(it) },
                label         = { Text("Move comment") },
                placeholder   = { Text("What were you thinking here?", style = MaterialTheme.typography.bodySmall) },
                modifier      = Modifier.fillMaxWidth(),
                minLines      = 2,
                maxLines      = 4,
                enabled       = !state.sandboxMode,
            )

            // ── Move tree ───────────────────────────────────────────────────
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

// ── Sub-composables ────────────────────────────────────────────────────────────

@Composable
private fun MissedMomentBanner(
    visible: Boolean,
    onReview: () -> Unit,
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
                modifier          = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier           = Modifier.size(20.dp),
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
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.labelMedium,
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

@Composable
private fun BlunderGuardBanner(
    visible: Boolean,
    message: String,
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
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
            shape = RoundedCornerShape(8.dp),
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Flag,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onErrorContainer,
                    modifier           = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text     = message,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss) {
                    Text(
                        "OK",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
