package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.domain.CoachingTrigger
import com.acepero13.android.gamereviewer.engine.highlights.BoardAttackHelper
import com.acepero13.android.gamereviewer.ui.screens.CoordinationQuizPhase
import com.acepero13.chess.core.ui.board.Arrow
import com.acepero13.chess.core.ui.theme.AnalyzeBlue
import com.github.bhlangonijr.chesslib.Square
import kotlinx.coroutines.flow.update

internal class ProactiveCoachingController(private val session: GameSession) {

    private val detector = BoardScanDetector(session)
    private val multiSelect = MultiSelectAnswerHandler(session)

    fun enterProactiveCoaching() {
        val idx = session.uiState.value.moveIndex
        val trigger = session.uiState.value.triggersByMove[idx]?.firstOrNull() ?: return
        if (trigger is CoachingTrigger.EvalCalibration) {
            session.uiState.update {
                it.copy(showCalibrationPanel = true, calibrationTrigger = trigger,
                    calibrationUserValue = 0, calibrationLocked = false, calibrationFeedback = "",
                    calibrationFeedbackPositive = false, triggersEngaged = it.triggersEngaged + idx)
            }
        } else {
            session.uiState.update {
                it.copy(showProactiveCoaching = true, activeProactiveTrigger = trigger,
                    triggersEngaged = it.triggersEngaged + idx)
            }
        }
    }

    fun dismissProactiveCoaching() = session.uiState.update {
        it.copy(showProactiveCoaching = false, proactiveInteractiveMode = false,
            proactiveAnswerFeedback = null, proactiveAnswerIsCorrect = null,
            coachHighlightSquares = emptyList(), proactiveHangingSquares = emptyList(),
            proactiveHangingOwnSquares = emptySet(), proactiveFoundSquares = emptySet(),
            coordinationQuizPhase = CoordinationQuizPhase.ASKING,
            boardState = it.boardState.copy(arrows = emptyList()))
    }

    fun onCoordinationQuizReveal() {
        val trigger = session.uiState.value.activeProactiveTrigger ?: return
        session.uiState.update {
            it.copy(coordinationQuizPhase = CoordinationQuizPhase.REVEALING,
                boardState = it.boardState.copy(arrows = buildCoordinationArrows(trigger)))
        }
    }

    fun dismissPositionCoach() {
        val idx = session.uiState.value.moveIndex
        session.uiState.update { it.copy(positionCoachDismissedMoves = it.positionCoachDismissedMoves + idx) }
    }

    fun startProactiveInteraction() {
        val trigger = session.uiState.value.activeProactiveTrigger
        val (targetSquares, ownSquares) = when (trigger) {
            is CoachingTrigger.PreMoveChecklist -> detector.detectAllHangingSquares()
            is CoachingTrigger.CctCheck -> Pair(detector.detectOpponentCctSquares(), emptySet())
            else -> Pair(emptyList(), emptySet())
        }
        session.uiState.update {
            it.copy(proactiveInteractiveMode = true, proactiveAnswerFeedback = null,
                proactiveAnswerIsCorrect = null, coachHighlightSquares = emptyList(),
                proactiveHangingSquares = targetSquares, proactiveHangingOwnSquares = ownSquares,
                proactiveFoundSquares = emptySet())
        }
    }

    fun answerProactiveQuestion(square: Square) {
        val trigger = session.uiState.value.activeProactiveTrigger ?: return
        val state = session.uiState.value
        val isMultiSelect = (trigger is CoachingTrigger.PreMoveChecklist || trigger is CoachingTrigger.CctCheck)
            && state.proactiveHangingSquares.isNotEmpty()
        if (isMultiSelect) multiSelect.handle(trigger, state, square)
        else {
            val (feedback, correct, marks) = ProactiveAnswerEvaluator.evaluate(trigger, square)
            session.uiState.update {
                it.copy(proactiveInteractiveMode = false, proactiveAnswerFeedback = feedback,
                    proactiveAnswerIsCorrect = correct, coachHighlightSquares = marks)
            }
        }
    }

    private fun buildCoordinationArrows(trigger: CoachingTrigger): List<Arrow> = when (trigger) {
        is CoachingTrigger.CoordinatedAttack -> {
            val target = trigger.targetSquare ?: return emptyList()
            trigger.attackerSquares.map { Arrow(it, target, AnalyzeBlue) }
        }
        is CoachingTrigger.PieceHarmony -> {
            if (trigger.targetSquares.isEmpty() || trigger.attackerSquares.isEmpty()) return emptyList()
            trigger.attackerSquares.mapNotNull { attacker ->
                val target = trigger.targetSquares.minByOrNull { t ->
                    val df = (BoardAttackHelper.fileOf(attacker) - BoardAttackHelper.fileOf(t)).toLong()
                    val dr = (BoardAttackHelper.rankOf(attacker) - BoardAttackHelper.rankOf(t)).toLong()
                    df * df + dr * dr
                } ?: return@mapNotNull null
                Arrow(attacker, target, AnalyzeBlue)
            }
        }
        else -> emptyList()
    }
}
