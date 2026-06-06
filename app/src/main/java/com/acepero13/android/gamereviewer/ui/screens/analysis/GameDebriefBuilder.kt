package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.ui.screens.GamePrediction
import com.acepero13.android.gamereviewer.ui.screens.PredictionMatchResult

internal class GameDebriefBuilder(private val navigation: NavigationController) {

    fun buildGameStory(moments: List<CriticalMoment>, totalHalfMoves: Int): String {
        val user = moments.filter {
            it.type == CriticalMoment.Type.ENGINE_MARKED.name && navigation.isUserMove(it.moveIndex)
        }
        if (user.isEmpty()) return "Solid game — the engine found no major errors."
        val worst     = user.maxByOrNull { it.severity }!!
        val moveNum   = worst.moveIndex / 2 + 1
        val phase     = phaseLabel(worst.moveIndex, totalHalfMoves)
        val catName   = CategoryLabels.dominant(user)?.let { CategoryLabels.label(it) }
        return when (user.size) {
            1    -> "The game hinged on one moment — around move $moveNum in $phase."
            2    -> "Two errors shaped this game. Biggest shift: move $moveNum in $phase."
            else -> "${user.size} errors detected. Biggest: move $moveNum.${catName?.let { " Pattern: $it." } ?: ""}"
        }
    }

    fun buildDebrief(prediction: GamePrediction?, moments: List<CriticalMoment>): PredictionMatchResult {
        val user = moments.filter {
            it.type == CriticalMoment.Type.ENGINE_MARKED.name && navigation.isUserMove(it.moveIndex)
        }
        if (prediction == null || prediction == GamePrediction.NOT_SURE)
            return buildNoPredictionResult(user)
        return when (prediction) {
            GamePrediction.SPECIFIC_BLUNDER        -> evalBlunderPrediction(user)
            GamePrediction.TIME_PRESSURE           -> evalTimePrediction(user)
            GamePrediction.OUTPLAYED_POSITIONALLY  -> evalPositionalPrediction(user)
            GamePrediction.NOT_SURE                -> error("handled above")
        }
    }

    private fun buildNoPredictionResult(user: List<CriticalMoment>): PredictionMatchResult {
        if (user.isEmpty()) return PredictionMatchResult(true, "Solid game — no major errors detected.")
        val worst = user.maxByOrNull { it.severity }!!
        return PredictionMatchResult(true,
            headline = CategoryLabels.dominant(user)?.let { "Main pattern: ${CategoryLabels.label(it)}." }
                ?: "${user.size} error(s) detected.",
            detail = "Biggest moment: move ${worst.moveIndex / 2 + 1}.")
    }

    private fun evalBlunderPrediction(user: List<CriticalMoment>): PredictionMatchResult {
        val blunders = user.filter { it.severity >= 150 }
        if (blunders.isNotEmpty()) {
            val worst = blunders.maxByOrNull { it.severity }!!
            return PredictionMatchResult(true,
                headline = "Your read was right — blunder on move ${worst.moveIndex / 2 + 1}.",
                detail   = if (blunders.size > 1) "${blunders.size} blunders total (${worst.severity}cp worst)." else "${worst.severity}cp loss.")
        }
        return PredictionMatchResult(false, "No clean blunder — errors were more subtle.",
            detail = CategoryLabels.dominant(user)?.let { "Main pattern: ${CategoryLabels.label(it)}." })
    }

    private fun evalTimePrediction(user: List<CriticalMoment>): PredictionMatchResult {
        val timeMoments = user.filter { it.reasonCategory == CriticalMoment.ReasonCategory.TIME_PRESSURE.name }
        return if (timeMoments.isNotEmpty())
            PredictionMatchResult(true, "Confirmed — ${timeMoments.size} time-pressure mistake(s).")
        else
            PredictionMatchResult(false, "Time wasn't the main issue here.",
                CategoryLabels.dominant(user)?.let { "Main pattern: ${CategoryLabels.label(it)}." })
    }

    private fun evalPositionalPrediction(user: List<CriticalMoment>): PredictionMatchResult {
        val positional = user.filter {
            it.reasonCategory in listOf(CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE.name,
                CriticalMoment.ReasonCategory.OPENING_DEVIATION.name)
        }
        return if (positional.isNotEmpty())
            PredictionMatchResult(true, "Correct — ${positional.size} strategic error(s) detected.")
        else
            PredictionMatchResult(false, "The problem was more tactical than positional.",
                CategoryLabels.dominant(user)?.let { "Main pattern: ${CategoryLabels.label(it)}." })
    }

    private fun phaseLabel(moveIndex: Int, totalHalfMoves: Int) = when {
        moveIndex < 20                      -> "the opening"
        moveIndex < totalHalfMoves * 2 / 3  -> "the middlegame"
        else                                -> "the endgame"
    }
}
