package com.acepero13.android.gamereviewer.ui.screens.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.domain.InsightReconciler
import com.acepero13.android.gamereviewer.ui.components.EndgameRecognitionPanel
import com.acepero13.android.gamereviewer.ui.components.GameStoryCard
import com.acepero13.android.gamereviewer.ui.components.MiddlegamePlanPanel
import com.acepero13.android.gamereviewer.ui.components.NavigateModeContent
import com.acepero13.android.gamereviewer.ui.components.OpeningDeviationPanel
import com.acepero13.android.gamereviewer.ui.components.PositionCoachCard
import com.acepero13.android.gamereviewer.ui.components.PostGameDebrief
import com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState
import com.acepero13.android.gamereviewer.ui.screens.AnalysisViewModel

@Composable
internal fun NavigateModePanel(
    state:        AnalysisUiState,
    vm:           AnalysisViewModel,
    onViewReport: () -> Unit,
    modifier:     Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GameStoryCard(
            story     = state.gameStory,
            visible   = state.gameStoryUnlocked && !state.gameStoryDismissed,
            onDismiss = vm::dismissGameStory,
            modifier  = Modifier.fillMaxWidth(),
        )
        PostGameDebrief(
            visible      = state.showPostGameDebrief,
            prediction   = state.gamePrediction,
            matchResult  = state.predictionMatchResult,
            onDismiss    = vm::dismissPostGameDebrief,
            onViewReport = { onViewReport() },
            modifier     = Modifier.fillMaxWidth(),
        )
        if (state.showOpeningDeviationPanel) {
            state.openingDeviation?.let { deviation ->
                OpeningDeviationPanel(
                    deviation  = deviation,
                    insight    = InsightReconciler.forReason(CriticalMoment.ReasonCategory.OPENING_DEVIATION),
                    onContinue = vm::dismissOpeningDeviationPanel,
                    modifier   = Modifier.fillMaxWidth(),
                )
            }
        }
        if (state.showEndgameRecognitionPanel) {
            state.endgameClassification?.let { classification ->
                EndgameRecognitionPanel(
                    classification = classification,
                    insight        = InsightReconciler.forEndgame(chapter = classification.entry.chapter, name = classification.entry.name),
                    onContinue     = vm::dismissEndgameRecognitionPanel,
                    modifier       = Modifier.fillMaxWidth(),
                )
            }
        }
        if (state.showMiddlegamePlanPanel) {
            state.middlegamePlanClassification?.let { classification ->
                MiddlegamePlanPanel(
                    classification = classification,
                    insights       = classification.plans.map { InsightReconciler.forMiddlegamePlan(it) },
                    onContinue     = vm::dismissMiddlegamePlanPanel,
                    modifier       = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                )
            }
        }
        if (state.positionCoachEnabled && !state.showProactiveCoaching &&
            !state.showOpeningDeviationPanel && !state.showEndgameRecognitionPanel && !state.showMiddlegamePlanPanel) {
            val highlight = remember(state.moveIndex, state.gameHighlights) {
                state.gameHighlights.firstOrNull { it.moveIndex == state.moveIndex }
            }
            if (highlight != null && state.moveIndex !in state.positionCoachDismissedMoves) {
                PositionCoachCard(moveIndex = state.moveIndex, highlight = highlight, onDismiss = vm::dismissPositionCoach, modifier = Modifier.fillMaxWidth())
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
}
