package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.components.GuidedDiscoveryPanel
import com.acepero13.chess.core.ui.board.ChessBoard
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private val BannerBorder = Color(0xFFC9A84C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeaknessDrillScreen(
    categoryNames: List<String>,
    drillTitle:    String,
    onBack:        () -> Unit,
    vm: WeaknessDrillViewModel = koinViewModel(parameters = { parametersOf(categoryNames) }),
) {
    val state by vm.uiState.collectAsState()
    val appColors = LocalAppColors.current

    Scaffold(
        containerColor = appColors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            drillTitle,
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = ChessGold,
                        )
                        if (state.queue.isNotEmpty()) {
                            Text(
                                "${state.currentIdx + 1} / ${state.queue.size} positions",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Outlined.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = appColors.background),
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier          = Modifier.fillMaxSize().padding(padding),
                    contentAlignment  = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = ChessGold)
                }
            }

            state.queue.isEmpty() -> {
                Box(
                    modifier         = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No positions found for this weakness pattern.\nAnalyse more games first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                Column(
                    modifier            = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val currentItem = state.queue.getOrNull(state.currentIdx)

                    // Game context banner
                    if (currentItem != null) {
                        DrillContextBanner(
                            gameLabel = currentItem.gameLabel,
                            moveIndex = currentItem.moveIndex,
                            modifier  = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                        )
                    }

                    // Board
                    ChessBoard(
                        boardState     = state.boardState,
                        modifier       = Modifier.fillMaxWidth(),
                        onSquareTap    = vm::onMentorSquareTap,
                        onArrowDrawn   = { _, _ -> },
                        onSquareMarked = { _ -> },
                    )

                    // Progress bar
                    if (state.queue.isNotEmpty()) {
                        LinearProgressIndicator(
                            progress = { state.currentIdx.toFloat() / state.queue.size },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            color    = ChessGold,
                        )
                    }

                    // Drill complete overlay
                    AnimatedVisibility(
                        visible = state.isComplete,
                        enter   = fadeIn() + expandVertically(),
                        exit    = fadeOut() + shrinkVertically(),
                    ) {
                        DrillCompleteCard(
                            total    = state.queue.size,
                            title    = drillTitle,
                            onBack   = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        )
                    }

                    // Guided discovery panel (hidden when complete)
                    if (!state.isComplete) {
                        state.insight?.let { insight ->
                            GuidedDiscoveryPanel(
                                insight                     = insight,
                                insightRevealed             = state.guidedDiscoveryInsightRevealed,
                                thoughts                    = state.guidedDiscoveryThoughts,
                                hintVisible                 = state.guidedDiscoveryHintVisible,
                                answerRevealed              = state.guidedDiscoveryAnswerRevealed,
                                engineThinking              = state.guidedDiscoveryEngineThinking,
                                revealedEvalCp              = state.guidedDiscoveryRevealedEvalCp,
                                mentorMoveInputActive       = state.mentorMoveInputActive,
                                mentorMoveChecking          = state.mentorMoveChecking,
                                mentorMoveResult            = state.mentorMoveResult,
                                mentorMoveFeedback          = state.mentorMoveFeedback,
                                showClassificationQuiz      = state.showClassificationQuiz,
                                classificationOptions       = state.classificationOptions,
                                classificationCorrectIndex  = state.classificationCorrectIndex,
                                classificationSelectedIndex = state.classificationSelectedIndex,
                                onSelectClassification      = vm::selectClassificationOption,
                                onThoughtsChange            = vm::updateGuidedThoughts,
                                onRevealHint                = vm::revealGuidedHint,
                                onRevealAnswer              = vm::revealGuidedAnswer,
                                onSubmit                    = vm::submitGuidedThoughts,
                                onExit                      = onBack,
                                onToggleMoveInput           = vm::toggleMentorMoveInput,
                                onRetryMove                 = vm::retryMentorMove,
                                modifier                    = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrillContextBanner(
    gameLabel: String,
    moveIndex: Int,
    modifier:  Modifier = Modifier,
) {
    val appColors     = LocalAppColors.current
    val fullMoveNum   = (moveIndex + 1) / 2
    val side          = if (moveIndex % 2 == 1) "White" else "Black"
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(8.dp),
        colors   = CardDefaults.cardColors(containerColor = ChessGold.copy(alpha = 0.10f)),
        border   = androidx.compose.foundation.BorderStroke(1.dp, BannerBorder),
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(
                gameLabel,
                style  = MaterialTheme.typography.labelMedium,
                color  = appColors.textPrimary,
            )
            Text(
                "Move $fullMoveNum — Find the best for $side",
                style  = MaterialTheme.typography.labelSmall,
                color  = appColors.textSecondary,
            )
        }
    }
}

@Composable
private fun DrillCompleteCard(
    total:    Int,
    title:    String,
    onBack:   () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier              = Modifier.padding(20.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(8.dp),
        ) {
            Text("🏆", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Drill Complete!",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = ChessGold,
            )
            Text(
                "You've reviewed all $total $title positions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onBack,
                colors  = ButtonDefaults.buttonColors(containerColor = ChessGold),
            ) {
                Text("Back to Dashboard", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
