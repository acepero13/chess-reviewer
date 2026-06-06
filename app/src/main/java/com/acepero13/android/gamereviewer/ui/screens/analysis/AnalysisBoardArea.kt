package com.acepero13.android.gamereviewer.ui.screens.analysis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.components.CalibrationPanel
import com.acepero13.android.gamereviewer.ui.components.ProactiveCoachingPanel
import com.acepero13.android.gamereviewer.ui.screens.AnalyseSubMode
import com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState
import com.acepero13.android.gamereviewer.ui.screens.AnalysisViewModel
import com.acepero13.android.gamereviewer.ui.screens.ReviewMode
import com.acepero13.chess.core.ui.components.EvalBar
import com.acepero13.chess.core.ui.theme.ChessGold
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun AnalysisBoardArea(
    state:         AnalysisUiState,
    vm:            AnalysisViewModel,
    boardFraction: Float,
    snackbar:      SnackbarHostState,
    scope:         CoroutineScope,
) {
    AnimatedVisibility(
        visible = state.reviewMode == ReviewMode.ANALYSE && state.evalBarVisible,
        enter   = fadeIn(),
        exit    = fadeOut(),
    ) { EvalBar(evalCp = state.currentEvalCp, thinking = state.sandboxEngineThinking || state.guidedDiscoveryEngineThinking, modifier = Modifier.fillMaxWidth()) }

    AnimatedVisibility(
        visible = state.reviewMode == ReviewMode.MENTOR && state.mentorContextLabel.isNotBlank(),
        enter   = fadeIn() + slideInVertically { -it },
        exit    = fadeOut() + slideOutVertically { -it },
    ) { MentorContextBanner(label = state.mentorContextLabel) }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BoardWithBlunderFlash(
            state           = state,
            onArrow         = { f, t -> vm.onArrowDrawn(f, t) },
            onMark          = { sq -> vm.onSquareMarked(sq) },
            onTap           = { sq -> if (state.reviewMode == ReviewMode.ANALYSE && state.analyseSubMode == AnalyseSubMode.EXPLORE) vm.onSandboxSquareTap(sq) },
            onMentorTap     = { sq -> vm.onMentorSquareTap(sq) },
            onProactiveTap  = { sq -> vm.answerProactiveQuestion(sq) },
            modifier        = Modifier.fillMaxWidth(boardFraction),
        )
    }

    if (state.sandboxEngineThinking && state.reviewMode == ReviewMode.ANALYSE && state.analyseSubMode == AnalyseSubMode.EXPLORE) {
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).border(0.dp, ChessGold.copy(alpha = 0.6f), RoundedCornerShape(2.dp)))
    }

    NavigationControls(state = state, vm = vm, snackbar = snackbar, scope = scope)

    if (!state.showOpeningDeviationPanel && !state.showEndgameRecognitionPanel && !state.showMiddlegamePlanPanel) {
        state.activeProactiveTrigger?.let { trigger ->
            ProactiveCoachingPanel(
                trigger                    = trigger,
                visible                    = state.showProactiveCoaching,
                isWeakArea                 = trigger.typeName() in state.weakTriggerTypes,
                onDismiss                  = vm::dismissProactiveCoaching,
                onStartInteraction         = vm::startProactiveInteraction,
                proactiveInteractiveMode   = state.proactiveInteractiveMode,
                proactiveAnswerFeedback    = state.proactiveAnswerFeedback,
                proactiveAnswerIsCorrect   = state.proactiveAnswerIsCorrect,
                proactiveFoundCount        = state.proactiveFoundSquares.size,
                proactiveTotalCount        = state.proactiveHangingSquares.size,
                coordinationQuizPhase      = state.coordinationQuizPhase,
                onCoordinationReveal       = vm::onCoordinationQuizReveal,
                onTryForcingSequence       = vm::enterForcingSequenceMode,
                onShowForcingSequence      = vm::showForcingSequence,
                onReplayForcingSequence    = vm::replayForcingSequence,
                forcingSequenceMode        = state.forcingSequenceMode,
                forcingSequenceAnimating   = state.forcingSequenceAnimating,
                forcingSequenceComplete    = state.forcingSequenceComplete,
                forcingSequenceCurrentStep = state.forcingSequenceCurrentStep,
                forcingSequenceTotalSteps  = state.forcingSequencePvMoves.size,
                modifier                   = Modifier.fillMaxWidth(),
            )
        }
    }

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

    if (state.forcingSequenceMode && state.analyseSubMode == AnalyseSubMode.EXPLORE) {
        ForcingSequenceBanner(
            animating   = state.forcingSequenceAnimating,
            complete    = state.forcingSequenceComplete,
            currentStep = state.forcingSequenceCurrentStep,
            totalSteps  = state.forcingSequencePvMoves.size,
            onGiveUp    = vm::showForcingSequence,
            onReplay    = vm::replayForcingSequence,
            onDone      = vm::exitForcingSequenceMode,
            modifier    = Modifier.fillMaxWidth(),
        )
    }
}
