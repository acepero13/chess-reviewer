package com.acepero13.android.gamereviewer.domain

import android.util.Log
import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.engine.highlights.BoardAttackHelper
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.PieceType

private const val TAG = "TacticalMotifDetector"

internal object TacticalMotifDetector {

    fun detectForcingMove(
        eval: GameEvaluation, isWhite: Boolean, playerIsWhite: Boolean,
        moverLoss: Int, pfx: String = "",
    ): CoachingTrigger.ForcingMove? {
        if (eval.motif !in listOf("fork", "hanging", "checkmate", "mixed")) {
            Log.d(TAG, "$pfx ForcingMove: SUPPRESS motif=${eval.motif}"); return null
        }
        if (isWhite == playerIsWhite) {
            Log.d(TAG, "$pfx ForcingMove: SUPPRESS player's own move"); return null
        }
        if (moverLoss < CoachingThresholds.FORCING_MOVE_MIN_CP_LOSS) {
            Log.d(TAG, "$pfx ForcingMove: SUPPRESS moverLoss=$moverLoss < threshold"); return null
        }
        val playerAdvantage = if (playerIsWhite) eval.evalCp else -eval.evalCp
        if (playerAdvantage < CoachingThresholds.FORCING_MOVE_MIN_PLAYER_ADVANTAGE_CP) {
            Log.d(TAG, "$pfx ForcingMove: SUPPRESS playerAdvantage=$playerAdvantage"); return null
        }
        return CoachingTrigger.ForcingMove(eval.moveIndex, eval.motif)
    }

    fun detectPreMoveChecklist(
        board: Board, eval: GameEvaluation, moverLoss: Int, pfx: String = "",
    ): CoachingTrigger.PreMoveChecklist? {
        if (moverLoss < CoachingThresholds.PRE_MOVE_CHECKLIST_MIN_CP_LOSS) {
            Log.d(TAG, "$pfx PreMoveChecklist: SUPPRESS moverLoss=$moverLoss < threshold"); return null
        }
        if (moverLoss >= CoachingThresholds.PRE_MOVE_CHECKLIST_MAX_CP_LOSS) {
            Log.d(TAG, "$pfx PreMoveChecklist: SUPPRESS terminal blunder moverLoss=$moverLoss"); return null
        }
        val hangingSquare = BoardAttackHelper.allPieces(board)
            .filter { (_, piece) -> piece.pieceType != PieceType.KING }
            .firstOrNull { (sq, piece) -> BoardAttackHelper.isGenuinelyHanging(board, sq, piece) }
            ?.first
        return if (hangingSquare != null) CoachingTrigger.PreMoveChecklist(eval.moveIndex, hangingSquare.name)
               else { Log.d(TAG, "$pfx PreMoveChecklist: SUPPRESS no hanging piece"); null }
    }

    fun detectOpponentPlan(
        eval: GameEvaluation, isWhite: Boolean, pfx: String = "",
    ): CoachingTrigger.OpponentPlan? {
        val moverGain = if (isWhite) eval.evalDelta else -eval.evalDelta
        return if (moverGain in CoachingThresholds.OPPONENT_PLAN_MIN_CP..CoachingThresholds.OPPONENT_PLAN_MAX_CP)
            CoachingTrigger.OpponentPlan(eval.moveIndex, moverGain)
        else { Log.d(TAG, "$pfx OpponentPlan: SUPPRESS moverGain=$moverGain"); null }
    }

    fun detectPunishBlunder(
        eval: GameEvaluation, moverLoss: Int, pfx: String = "",
    ): CoachingTrigger.PunishBlunder? =
        if (moverLoss >= CoachingThresholds.PUNISH_BLUNDER_MIN_CP_LOSS)
            CoachingTrigger.PunishBlunder(eval.moveIndex, moverLoss)
        else { Log.d(TAG, "$pfx PunishBlunder: SUPPRESS moverLoss=$moverLoss"); null }
}
