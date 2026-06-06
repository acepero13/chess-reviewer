package com.acepero13.android.gamereviewer.ui.screens.analysis

import android.util.Log
import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.domain.InsightReconciler
import com.acepero13.android.gamereviewer.ui.screens.ReviewMode
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "MentorMode"

internal class MentorModeController(
    private val session: GameSession,
    private val navigation: NavigationController,
) {

    fun enterMentorMode(targetMoveIndex: Int) {
        val from    = session.uiState.value.reviewMode
        val moment  = session.uiState.value.criticalMoments.firstOrNull {
            it.moveIndex == targetMoveIndex && it.type == CriticalMoment.Type.ENGINE_MARKED.name
        }
        val reason  = moment?.toReason() ?: CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE
        val insight = InsightReconciler.forReason(reason)
        val clamped = targetMoveIndex.coerceIn(0, session.uciMoves.size)
        val displayIndex = (clamped - 1).coerceAtLeast(0)
        val contextLabel = buildContextLabel(displayIndex, clamped)
        session.scope.launch(Dispatchers.Default) {
            Log.d(TAG, "enterMentorMode: navigating to $displayIndex")
            navigation.applyMoveIndex(displayIndex)
            applyMentorState(insight, moment, contextLabel, from)
            Log.d(TAG, "enterMentorMode complete — reviewMode=${session.uiState.value.reviewMode}")
        }
    }

    fun exitMentorMode() {
        val returnTo = session.uiState.value.previousReviewMode
        val idx      = session.uiState.value.moveIndex
        session.uiState.update { resetMentorState(it, returnTo) }
        session.scope.launch(Dispatchers.Default) { navigation.applyMoveIndex(idx) }
    }

    private fun buildContextLabel(displayIndex: Int, clamped: Int): String {
        val displayFen = session.fenSequence.getOrElse(displayIndex) { session.startFen }
        val sideToMove = runCatching { Board().apply { loadFromFen(displayFen) }.sideToMove }.getOrNull()
        val who = when (sideToMove) { Side.WHITE -> "White"; Side.BLACK -> "Black"; else -> "you" }
        return "Move ${clamped / 2 + 1}. — Find the best move for $who"
    }

    private fun applyMentorState(
        insight: InsightReconciler.Insight,
        moment: CriticalMoment?,
        contextLabel: String,
        from: ReviewMode,
    ) {
        session.uiState.update {
            it.copy(
                reviewMode = ReviewMode.MENTOR,
                previousReviewMode = if (from == ReviewMode.MENTOR) it.previousReviewMode else from,
                guidedDiscoveryMode = true, guidedDiscoveryInsight = insight,
                guidedDiscoveryCriticalMoment = moment, guidedDiscoveryThoughts = "",
                guidedDiscoveryHintVisible = false, guidedDiscoveryAnswerRevealed = false,
                guidedDiscoveryRevealedEvalCp = null, guidedDiscoveryEngineThinking = false,
                boardState = it.boardState.copy(isEditorMode = false, arrows = emptyList()),
                showMissedMomentBanner = false, mentorMoveInputActive = false,
                mentorMoveChecking = false, mentorMoveResult = null, mentorMoveFeedback = "",
                mentorContextLabel = contextLabel, showClassificationQuiz = false,
                classificationOptions = emptyList(), classificationCorrectIndex = -1,
                classificationSelectedIndex = -1, guidedDiscoveryInsightRevealed = false,
            )
        }
    }

    private fun resetMentorState(it: com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState, returnTo: ReviewMode) =
        it.copy(
            reviewMode = returnTo, guidedDiscoveryMode = false, guidedDiscoveryInsight = null,
            guidedDiscoveryCriticalMoment = null, guidedDiscoveryAnswerRevealed = false,
            guidedDiscoveryHintVisible = false, guidedDiscoveryRevealedEvalCp = null,
            mentorMoveInputActive = false, mentorMoveChecking = false,
            mentorMoveResult = null, mentorMoveFeedback = "", mentorContextLabel = "",
            mentorSessionQueue = emptyList(), mentorSessionIdx = 0,
            showClassificationQuiz = false, classificationOptions = emptyList(),
            classificationCorrectIndex = -1, classificationSelectedIndex = -1,
            guidedDiscoveryInsightRevealed = false, weaknessContext = null,
            showCoachsBriefing = false, pivotalMoments = null, showPivotalMomentsPanel = false,
            showProactiveCoaching = false, activeProactiveTrigger = null,
            triggersEngaged = emptySet(), showReflectionMode = false, reflectionItems = emptyList(),
            showCalibrationPanel = false, calibrationTrigger = null, calibrationUserValue = 0,
            calibrationLocked = false, calibrationFeedback = "", calibrationFeedbackPositive = false,
        )
}

