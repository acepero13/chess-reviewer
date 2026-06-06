package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.data.db.CriticalMomentDao
import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.domain.BehavioralDiagnostic
import com.acepero13.android.gamereviewer.domain.PivotalMomentsSelector
import com.acepero13.android.gamereviewer.ui.screens.WeaknessContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class MentorSessionController(
    private val session: GameSession,
    private val criticalMomentDao: CriticalMomentDao,
    private val mentorMode: MentorModeController,
    private val reflection: ReflectionController,
) {

    fun enterMentorSession() {
        session.scope.launch {
            val currentMoments = session.uiState.value.criticalMoments
                .filter { it.type == CriticalMoment.Type.ENGINE_MARKED.name && isUserMove(it.moveIndex) }
            if (currentMoments.isEmpty()) return@launch

            val allMoments = withContext(Dispatchers.IO) { criticalMomentDao.getAll() }
            val totalGamesAnalyzed = withContext(Dispatchers.IO) { criticalMomentDao.countGamesAnalyzed() }
            val topTrend: BehavioralDiagnostic.FailureTrend? = withContext(Dispatchers.Default) {
                BehavioralDiagnostic.diagnose(allMoments, topN = 1).firstOrNull()
            }
            val topCategories = topTrend?.triggerCategories ?: emptySet()
            val pivotalMoments = withContext(Dispatchers.Default) {
                PivotalMomentsSelector.select(
                    truthMap = session.truthMap, criticalMoments = currentMoments, isUserMove = ::isUserMove,
                )
            }
            val queue = pivotalMoments.moveIndices.ifEmpty {
                buildWeaknessPrioritizedQueue(currentMoments, topCategories)
            }
            if (queue.isEmpty()) return@launch

            val weaknessCtx = buildWeaknessContext(topTrend, topCategories, currentMoments, totalGamesAnalyzed)
            session.uiState.update {
                it.copy(
                    mentorSessionQueue = queue, mentorSessionIdx = 0,
                    weaknessContext = weaknessCtx, showCoachsBriefing = false,
                    pivotalMoments = pivotalMoments, showPivotalMomentsPanel = true,
                    reviewMode = com.acepero13.android.gamereviewer.ui.screens.ReviewMode.MENTOR,
                    previousReviewMode = it.reviewMode,
                )
            }
        }
    }

    fun dismissPivotalMomentsPanel() {
        session.uiState.update { it.copy(showPivotalMomentsPanel = false) }
        val queue = session.uiState.value.mentorSessionQueue
        if (queue.isNotEmpty()) mentorMode.enterMentorMode(queue[0])
        else mentorMode.exitMentorMode()
    }

    fun reviewPivotalMoment(moveIndex: Int) {
        session.uiState.update { it.copy(showPivotalMomentsPanel = false) }
        val queue = session.uiState.value.mentorSessionQueue
        val idx = queue.indexOf(moveIndex)
        if (idx >= 0) session.uiState.update { it.copy(mentorSessionIdx = idx) }
        mentorMode.enterMentorMode(moveIndex)
    }

    fun dismissCoachsBriefing() = session.uiState.update { it.copy(showCoachsBriefing = false) }

    fun advanceMentorSession() {
        val nextIdx = session.uiState.value.mentorSessionIdx + 1
        val queue = session.uiState.value.mentorSessionQueue
        if (nextIdx < queue.size) {
            session.uiState.update { it.copy(mentorSessionIdx = nextIdx) }
            mentorMode.enterMentorMode(queue[nextIdx])
        } else {
            session.uiState.update { it.copy(mentorSessionQueue = emptyList(), mentorSessionIdx = 0) }
            val hasReflectionItems = session.uiState.value.triggersByMove
                .keys.any { it !in session.uiState.value.triggersEngaged }
            if (hasReflectionItems) reflection.enterReflectionMode(mentorMode::exitMentorMode)
            else mentorMode.exitMentorMode()
        }
    }

    private fun isUserMove(moveIndex: Int): Boolean {
        if (!session.playerSideKnown) return true
        val isBlackMove = moveIndex % 2 == 0
        return session.boardFlippedForBlack == isBlackMove
    }

    private fun buildWeaknessContext(
        topTrend: BehavioralDiagnostic.FailureTrend?,
        topCategories: Set<CriticalMoment.ReasonCategory>,
        currentMoments: List<CriticalMoment>,
        totalGamesAnalyzed: Int,
    ): WeaknessContext? {
        topTrend ?: return null
        val matchingIndices = currentMoments.filter { it.toReason() in topCategories }.map { it.moveIndex }
        return WeaknessContext(
            trendTitle = topTrend.title, trendEmoji = topTrend.emoji,
            trendDescription = topTrend.description, gamesAffected = topTrend.frequency,
            totalGamesAnalyzed = totalGamesAnalyzed, matchingMoveIndices = matchingIndices,
        )
    }

    private fun buildWeaknessPrioritizedQueue(
        moments: List<CriticalMoment>,
        topCategories: Set<CriticalMoment.ReasonCategory>,
    ): List<Int> {
        val weaknessMoments = moments.filter { it.toReason() in topCategories }.sortedByDescending { it.severity }
        val otherMoments = moments.filter { it.toReason() !in topCategories }.sortedByDescending { it.severity }
        return (weaknessMoments + otherMoments).take(3).map { it.moveIndex }
    }
}
