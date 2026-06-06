package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.domain.CoachingTrigger

internal object CoachTriggerProps {
    fun format(t: CoachingTrigger): String = when (t) {
        is CoachingTrigger.Safety -> "kingSquare=${t.kingSquare}"
        is CoachingTrigger.CandidateMoves -> "evalCp=${t.evalCp}"
        is CoachingTrigger.WorstPiece -> "pieceSquare=${t.pieceSquare}, mobility=${t.mobility}"
        is CoachingTrigger.ForcingMove -> "motif=${t.motif}"
        is CoachingTrigger.OpponentPlan -> "evalGain=${t.evalGain}"
        is CoachingTrigger.PreMoveChecklist -> "hangingSquare=${t.hangingSquare}"
        is CoachingTrigger.RookActivation -> "rookSquare=${t.rookSquare}, openFileIndex=${t.openFileIndex}"
        is CoachingTrigger.ImpulseControl -> "timeSpentSeconds=${t.timeSpentSeconds}, cpLoss=${t.cpLoss}"
        is CoachingTrigger.CalculationBlunder -> "timeSpentSeconds=${t.timeSpentSeconds}, cpLoss=${t.cpLoss}"
        is CoachingTrigger.TacticalOversight -> "timeSpentSeconds=${t.timeSpentSeconds}, cpLoss=${t.cpLoss}"
        is CoachingTrigger.CandidateSearch -> "evalCp=${t.evalCp}"
        is CoachingTrigger.CctCheck -> "evalDelta=${t.evalDelta}"
        is CoachingTrigger.ConversionStrategy -> "evaluationCp=${t.evaluationCp}"
        is CoachingTrigger.CoordinatedAttack -> "isPlayerSide=${t.isPlayerSide}, isLoss=${t.isLoss}, pieceCount=${t.pieceCount}"
        is CoachingTrigger.PieceHarmony -> "isPlayerSide=${t.isPlayerSide}, isLoss=${t.isLoss}, score=${t.score}"
        is CoachingTrigger.PunishBlunder -> "opponentLoss=${t.opponentLoss}"
        is CoachingTrigger.EvalCalibration -> "engineEvalCp=${t.engineEvalCp}, context=${t.context}"
    }
}
