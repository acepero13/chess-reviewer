package com.acepero13.android.gamereviewer.domain

import android.util.Log
import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.github.bhlangonijr.chesslib.Board

private const val TAG = "TriggerSuppressor"

internal object TriggerSuppressor {

    fun apply(
        triggers: MutableList<CoachingTrigger>, board: Board?,
        eval: GameEvaluation, isWhite: Boolean, playerIsWhite: Boolean,
        moverLoss: Int, pfx: String,
    ) {
        applySafetyOverride(triggers, pfx)
        applyCheckOverride(triggers, board, pfx)
        applyEvalSwingOverride(triggers, eval, pfx)
        applyConversion(triggers, eval, isWhite, playerIsWhite, pfx)
        applyCctOpponentMutex(triggers, pfx)
        applyForcingPunishMutex(triggers, pfx)
        applyPunishPmcMutex(triggers, pfx)
    }

    private fun applySafetyOverride(triggers: MutableList<CoachingTrigger>, pfx: String) {
        if (triggers.none { it is CoachingTrigger.Safety }) return
        val suppressed = triggers.filter { it.tier() >= 3 }
        if (suppressed.isNotEmpty()) {
            Log.d(TAG, "$pfx KingSafetyOverride: suppressing ${suppressed.map { it.typeName() }}")
            triggers.removeAll { it.tier() >= 3 }
        }
    }

    private fun applyCheckOverride(triggers: MutableList<CoachingTrigger>, board: Board?, pfx: String) {
        if (board == null || !board.isKingAttacked()) return
        val suppressed = triggers.filter { it.tier() >= 3 }
        if (suppressed.isNotEmpty()) {
            Log.d(TAG, "$pfx InCheckOverride: suppressing ${suppressed.map { it.typeName() }}")
            triggers.removeAll { it in suppressed }
        }
    }

    private fun applyEvalSwingOverride(triggers: MutableList<CoachingTrigger>, eval: GameEvaluation, pfx: String) {
        if (kotlin.math.abs(eval.evalDelta) < CoachingThresholds.COORDINATION_BLUNDER_SUPPRESS_CP) return
        val suppressed = triggers.filter {
            it is CoachingTrigger.PieceHarmony       || it is CoachingTrigger.CoordinatedAttack ||
            it is CoachingTrigger.WorstPiece         || it is CoachingTrigger.CandidateMoves    ||
            it is CoachingTrigger.CandidateSearch    || it is CoachingTrigger.RookActivation    ||
            it is CoachingTrigger.OpponentPlan
        }
        if (suppressed.isNotEmpty()) {
            Log.d(TAG, "$pfx EvalSwingSuppression: removing ${suppressed.map { it.typeName() }}")
            triggers.removeAll { it in suppressed }
        }
    }

    private fun applyConversion(
        triggers: MutableList<CoachingTrigger>, eval: GameEvaluation,
        isWhite: Boolean, playerIsWhite: Boolean, pfx: String,
    ) {
        if (kotlin.math.abs(eval.evalCp) > CoachingThresholds.CONVERSION_ADVANTAGE_THRESHOLD_CP) {
            val suppressed = triggers.filter {
                it is CoachingTrigger.RookActivation    || it is CoachingTrigger.CandidateMoves ||
                it is CoachingTrigger.WorstPiece        || it is CoachingTrigger.CandidateSearch ||
                it is CoachingTrigger.OpponentPlan      || it is CoachingTrigger.CoordinatedAttack ||
                it is CoachingTrigger.PieceHarmony
            }
            if (suppressed.isNotEmpty()) {
                Log.d(TAG, "$pfx ConversionSuppression: removing ${suppressed.map { it.typeName() }}")
                triggers.removeAll { it in suppressed }
            }
        }
        val evalFromPlayer = if (playerIsWhite) eval.evalCp else -eval.evalCp
        if (evalFromPlayer > CoachingThresholds.CONVERSION_ADVANTAGE_THRESHOLD_CP && isWhite == playerIsWhite) {
            triggers.add(CoachingTrigger.ConversionStrategy(eval.moveIndex, eval.evalCp))
            Log.d(TAG, "$pfx ConversionStrategy: FIRE evalFromPlayer=$evalFromPlayer")
        }
    }

    private fun applyCctOpponentMutex(triggers: MutableList<CoachingTrigger>, pfx: String) {
        if (triggers.none { it is CoachingTrigger.CctCheck }) return
        Log.d(TAG, "$pfx CctOpponentMutex: removing OpponentPlan")
        triggers.removeAll { it is CoachingTrigger.OpponentPlan }
    }

    private fun applyForcingPunishMutex(triggers: MutableList<CoachingTrigger>, pfx: String) {
        if (triggers.none { it is CoachingTrigger.ForcingMove }) return
        Log.d(TAG, "$pfx ForcingPunishMutex: removing PunishBlunder")
        triggers.removeAll { it is CoachingTrigger.PunishBlunder }
    }

    private fun applyPunishPmcMutex(triggers: MutableList<CoachingTrigger>, pfx: String) {
        if (triggers.none { it is CoachingTrigger.PunishBlunder }) return
        Log.d(TAG, "$pfx PunishPreMoveMutex: removing PreMoveChecklist")
        triggers.removeAll { it is CoachingTrigger.PreMoveChecklist }
    }
}
