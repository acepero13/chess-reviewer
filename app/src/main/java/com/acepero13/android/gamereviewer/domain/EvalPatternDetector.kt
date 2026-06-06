package com.acepero13.android.gamereviewer.domain

import android.util.Log
import com.acepero13.android.gamereviewer.data.model.GameEvaluation

private const val TAG = "EvalPatternDetector"

internal object EvalPatternDetector {

    fun detectCandidate(
        eval: GameEvaluation, isWhite: Boolean, pfx: String = "",
    ): CoachingTrigger.CandidateMoves? {
        val evalFromMover = if (isWhite) eval.evalCp else -eval.evalCp
        val absEval = kotlin.math.abs(evalFromMover)
        return if (eval.motif == "mixed" && absEval <= CoachingThresholds.CANDIDATE_EVAL_THRESHOLD_CP)
            CoachingTrigger.CandidateMoves(eval.moveIndex, eval.evalCp)
        else { Log.d(TAG, "$pfx CandidateMoves: SUPPRESS motif=${eval.motif} absEval=$absEval"); null }
    }

    fun detectCandidateSearch(
        eval: GameEvaluation, isWhite: Boolean, playerIsWhite: Boolean,
        moverLoss: Int, pfx: String = "",
    ): CoachingTrigger.CandidateSearch? {
        val evalFromMover  = if (isWhite) eval.evalCp else -eval.evalCp
        val absMover       = kotlin.math.abs(evalFromMover)
        val evalFromPlayer = if (playerIsWhite) eval.evalCp else -eval.evalCp
        if (moverLoss >= CoachingThresholds.CANDIDATE_SEARCH_CLARITY_CP) {
            Log.d(TAG, "$pfx CandidateSearch: SUPPRESS moverLoss=$moverLoss >= clarity threshold"); return null
        }
        return if (eval.motif == "mixed" && absMover in CoachingThresholds.CANDIDATE_SEARCH_MIN_CP..CoachingThresholds.CANDIDATE_SEARCH_MAX_CP)
            CoachingTrigger.CandidateSearch(eval.moveIndex, evalFromPlayer)
        else { Log.d(TAG, "$pfx CandidateSearch: SUPPRESS motif=${eval.motif} absMoverEval=$absMover"); null }
    }

    fun detectCctCheck(
        eval: GameEvaluation, isWhite: Boolean, playerIsWhite: Boolean,
        moverLoss: Int, pfx: String = "",
    ): CoachingTrigger.CctCheck? {
        if (isWhite == playerIsWhite) {
            Log.d(TAG, "$pfx CctCheck: SUPPRESS player's own move"); return null
        }
        val opponentGain = -moverLoss
        return if (opponentGain > CoachingThresholds.CCT_CHECK_EVAL_SHIFT_CP)
            CoachingTrigger.CctCheck(eval.moveIndex, eval.evalDelta)
        else { Log.d(TAG, "$pfx CctCheck: SUPPRESS opponentGain=$opponentGain"); null }
    }
}
