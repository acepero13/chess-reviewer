package com.acepero13.android.gamereviewer.domain

import android.util.Log
import com.acepero13.android.gamereviewer.data.model.GameEvaluation

private const val TAG = "TimePatternDetector"

internal object TimePatternDetector {

    fun detectAll(
        eval: GameEvaluation, isWhite: Boolean,
        timeByMoveIndex: (Int) -> Int?, pfx: String = "",
    ): List<CoachingTrigger> = listOfNotNull(
        detectImpulseControl(eval, isWhite, timeByMoveIndex, pfx),
        detectCalculationBlunder(eval, isWhite, timeByMoveIndex, pfx),
        detectTacticalOversight(eval, isWhite, timeByMoveIndex, pfx),
    )

    private fun detectImpulseControl(
        eval: GameEvaluation, isWhite: Boolean,
        timeByMoveIndex: (Int) -> Int?, pfx: String,
    ): CoachingTrigger.ImpulseControl? {
        val timeSpent = timeByMoveIndex(eval.moveIndex) ?: run {
            Log.d(TAG, "$pfx ImpulseControl: SUPPRESS no clock data"); return null
        }
        if (timeSpent >= CoachingThresholds.IMPULSE_TIME_THRESHOLD_SECONDS) {
            Log.d(TAG, "$pfx ImpulseControl: SUPPRESS timeSpent=${timeSpent}s"); return null
        }
        val moverDelta = if (isWhite) eval.evalDelta else -eval.evalDelta
        return if (moverDelta <= -CoachingThresholds.IMPULSE_CP_LOSS_THRESHOLD)
            CoachingTrigger.ImpulseControl(eval.moveIndex, timeSpent, kotlin.math.abs(moverDelta))
        else { Log.d(TAG, "$pfx ImpulseControl: SUPPRESS fast but small loss"); null }
    }

    private fun detectCalculationBlunder(
        eval: GameEvaluation, isWhite: Boolean,
        timeByMoveIndex: (Int) -> Int?, pfx: String,
    ): CoachingTrigger.CalculationBlunder? {
        val timeSpent = timeByMoveIndex(eval.moveIndex) ?: run {
            Log.d(TAG, "$pfx CalculationBlunder: SUPPRESS no clock data"); return null
        }
        if (timeSpent < CoachingThresholds.CALCULATION_BLUNDER_TIME_THRESHOLD_SECONDS) {
            Log.d(TAG, "$pfx CalculationBlunder: SUPPRESS timeSpent=${timeSpent}s"); return null
        }
        val moverDelta = if (isWhite) eval.evalDelta else -eval.evalDelta
        return if (moverDelta <= -CoachingThresholds.IMPULSE_CP_LOSS_THRESHOLD)
            CoachingTrigger.CalculationBlunder(eval.moveIndex, timeSpent, kotlin.math.abs(moverDelta))
        else { Log.d(TAG, "$pfx CalculationBlunder: SUPPRESS slow but small loss"); null }
    }

    private fun detectTacticalOversight(
        eval: GameEvaluation, isWhite: Boolean,
        timeByMoveIndex: (Int) -> Int?, pfx: String,
    ): CoachingTrigger.TacticalOversight? {
        val timeSpent = timeByMoveIndex(eval.moveIndex) ?: run {
            Log.d(TAG, "$pfx TacticalOversight: SUPPRESS no clock data"); return null
        }
        val min = CoachingThresholds.TACTICAL_OVERSIGHT_MIN_SECONDS
        val max = CoachingThresholds.TACTICAL_OVERSIGHT_MAX_SECONDS
        if (timeSpent < min || timeSpent >= max) {
            Log.d(TAG, "$pfx TacticalOversight: SUPPRESS timeSpent=${timeSpent}s not in [$min..$max)"); return null
        }
        val moverDelta = if (isWhite) eval.evalDelta else -eval.evalDelta
        return if (moverDelta <= -CoachingThresholds.IMPULSE_CP_LOSS_THRESHOLD)
            CoachingTrigger.TacticalOversight(eval.moveIndex, timeSpent, kotlin.math.abs(moverDelta))
        else { Log.d(TAG, "$pfx TacticalOversight: SUPPRESS not enough loss"); null }
    }
}
