package com.acepero13.android.gamereviewer.domain

import android.util.Log
import com.acepero13.android.gamereviewer.data.model.GameEvaluation

private const val TAG = "CalibrationDetector"

internal class CalibrationDetector {

    private var lastCalibrationAt = -(CoachingThresholds.CALIBRATION_FREQUENCY_CAP * 2)

    fun onCalibrationFired(moveIndex: Int) { lastCalibrationAt = moveIndex }

    fun detect(eval: GameEvaluation, prevEvals: List<GameEvaluation>, pfx: String = ""): CoachingTrigger.EvalCalibration? {
        val moveIndex = eval.moveIndex
        if (moveIndex < CoachingThresholds.TIER4_OPENING_GATE) {
            Log.d(TAG, "$pfx Calibration: SUPPRESS opening (moveIndex=$moveIndex)"); return null
        }
        if (moveIndex - lastCalibrationAt < CoachingThresholds.CALIBRATION_FREQUENCY_CAP) {
            Log.d(TAG, "$pfx Calibration: SUPPRESS frequency cap (lastAt=$lastCalibrationAt)"); return null
        }
        if (kotlin.math.abs(eval.evalCp) > CoachingThresholds.CALIBRATION_MAX_EVAL_CP) {
            Log.d(TAG, "$pfx Calibration: SUPPRESS lopsided evalCp=${eval.evalCp}"); return null
        }
        if (eval.motif in listOf("fork", "hanging", "checkmate")) {
            Log.d(TAG, "$pfx Calibration: SUPPRESS tactical motif=${eval.motif}"); return null
        }
        val avgVolatility = if (prevEvals.isEmpty()) 0
                            else prevEvals.sumOf { kotlin.math.abs(it.evalDelta) } / prevEvals.size
        if (avgVolatility > CoachingThresholds.CALIBRATION_VOLATILITY_MAX_CP) {
            Log.d(TAG, "$pfx Calibration: SUPPRESS high volatility avg=$avgVolatility"); return null
        }
        val isPostOpening  = moveIndex in CoachingThresholds.CALIBRATION_POST_OPENING_MIN..CoachingThresholds.CALIBRATION_POST_OPENING_MAX
        val prevEvalCp     = prevEvals.firstOrNull()?.evalCp ?: 0
        val wasEqual       = kotlin.math.abs(prevEvalCp) < CoachingThresholds.CALIBRATION_EVAL_JUMP_FROM_CP
        val isNowAdvantage = kotlin.math.abs(eval.evalCp) >= CoachingThresholds.CALIBRATION_EVAL_JUMP_TO_CP
        val context = when {
            wasEqual && isNowAdvantage -> CalibrationContext.EVAL_JUMP
            isPostOpening              -> CalibrationContext.POST_OPENING
            else -> {
                Log.d(TAG, "$pfx Calibration: SUPPRESS no context (postOpening=$isPostOpening evalJump=${wasEqual && isNowAdvantage})")
                return null
            }
        }
        return CoachingTrigger.EvalCalibration(moveIndex, eval.evalCp, context)
    }
}
