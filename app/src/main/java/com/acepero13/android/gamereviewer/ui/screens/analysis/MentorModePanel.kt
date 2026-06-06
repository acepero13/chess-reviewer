package com.acepero13.android.gamereviewer.ui.screens.analysis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.components.MentorPanel
import com.acepero13.android.gamereviewer.ui.components.MentorPivotalMomentsPanel
import com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState
import com.acepero13.android.gamereviewer.ui.screens.AnalysisViewModel
import com.acepero13.chess.core.ui.theme.LocalAppColors

@Composable
internal fun MentorModePanel(
    state:    AnalysisUiState,
    vm:       AnalysisViewModel,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier            = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                state.showPivotalMomentsPanel -> {
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
                    BoardScanReflectionPanel(
                        items    = state.reflectionItems,
                        onAnswer = vm::answerReflection,
                        onFinish = vm::exitReflectionMode,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> {
                    AnimatedVisibility(
                        visible = state.showCoachsBriefing && state.weaknessContext != null,
                        enter   = fadeIn(tween(200)) + expandVertically(),
                        exit    = fadeOut(tween(150)) + shrinkVertically(),
                    ) {
                        state.weaknessContext?.let { ctx ->
                            CoachsBriefingCard(ctx = ctx, onDismiss = vm::dismissCoachsBriefing, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
                        }
                    }
                    HorizontalDivider(color = appColors.border, thickness = 1.dp)
                    if (state.mentorSessionQueue.isNotEmpty()) {
                        val matchingIndices = state.weaknessContext?.matchingMoveIndices?.toSet() ?: emptySet()
                        val weaknessDotPositions = state.mentorSessionQueue
                            .mapIndexedNotNull { pos, moveIdx -> if (moveIdx in matchingIndices) pos + 1 else null }
                            .toSet()
                        MentorSessionProgressHeader(
                            current              = state.mentorSessionIdx + 1,
                            total                = state.mentorSessionQueue.size,
                            modifier             = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            weaknessDotPositions = weaknessDotPositions,
                        )
                    }
                    val isRecurring = state.weaknessContext?.matchingMoveIndices
                        ?.contains(state.guidedDiscoveryCriticalMoment?.moveIndex) == true
                    MentorPanel(state = state, vm = vm, isRecurringPattern = isRecurring, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
