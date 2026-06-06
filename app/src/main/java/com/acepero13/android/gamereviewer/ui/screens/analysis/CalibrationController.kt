package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.domain.CoachingTrigger
import kotlinx.coroutines.flow.update

internal class CalibrationController(private val session: GameSession) {

    fun onCalibrationValueChange(value: Int) =
        session.uiState.update { it.copy(calibrationUserValue = value.coerceIn(-2, 2)) }

    fun lockInCalibration() {
        val trigger = session.uiState.value.calibrationTrigger ?: return
        val userValue = session.uiState.value.calibrationUserValue
        val engineValue = evalCpToCalibrationValue(trigger.engineEvalCp)
        val diff = kotlin.math.abs(userValue - engineValue)
        val (feedback, positive) = buildFeedback(diff, userValue, engineValue)
        session.uiState.update {
            it.copy(calibrationLocked = true, calibrationFeedback = feedback, calibrationFeedbackPositive = positive)
        }
    }

    fun dismissCalibration() = session.uiState.update {
        it.copy(
            showCalibrationPanel = false, calibrationTrigger = null,
            calibrationUserValue = 0, calibrationLocked = false,
            calibrationFeedback = "", calibrationFeedbackPositive = false,
        )
    }

    private fun buildFeedback(diff: Int, userValue: Int, engineValue: Int): Pair<String, Boolean> {
        val userLabel = calibrationValueLabel(userValue)
        val engineLabel = calibrationValueLabel(engineValue)
        return when (diff) {
            0 -> Pair(
                "Spot on. You read the board exactly as the engine does — that kind of positional clarity is a real asset.",
                true,
            )
            1 -> Pair(
                "Close. You said $userLabel and the engine agrees it's $engineLabel — you're in the right neighbourhood.",
                true,
            )
            2 -> Pair(
                "You felt it was $userLabel, but the engine evaluates it as $engineLabel. Try to identify what piece or pawn structure difference you might have missed.",
                false,
            )
            else -> Pair(
                "You felt it was $userLabel, but the engine sees $engineLabel here. This is a gap worth studying — look at the pawn structure and piece activity to see what you underestimated.",
                false,
            )
        }
    }

    fun evalCpToCalibrationValue(evalCp: Int): Int = when {
        evalCp <= -150 -> -2
        evalCp <= -50 -> -1
        evalCp < 50 -> 0
        evalCp < 150 -> 1
        else -> 2
    }

    private fun calibrationValueLabel(value: Int) = when (value) {
        -2 -> "a strong Black advantage"
        -1 -> "a slight Black advantage"
        0 -> "equal"
        1 -> "a slight White advantage"
        2 -> "a strong White advantage"
        else -> "unknown"
    }
}

private val CoachingTrigger.engineEvalCp: Int
    get() = if (this is CoachingTrigger.EvalCalibration) engineEvalCp else 0
