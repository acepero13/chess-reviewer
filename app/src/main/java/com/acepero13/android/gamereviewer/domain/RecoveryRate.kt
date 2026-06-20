package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.GameEvaluation

/**
 * Mental-recovery metric: after the user plays an inaccuracy (or worse), how often is their
 * **next** move a best move? Measures the ability to stop a blunder "streak" after the first slip.
 */
object RecoveryRate {

    data class Result(val oversights: Int, val recovered: Int)

    /** @param userEvals the user's own half-moves, in play order. */
    fun compute(userEvals: List<GameEvaluation>, isWhite: Boolean): Result {
        val ordered = userEvals.sortedBy { it.moveIndex }
        var oversights = 0; var recovered = 0
        for (i in 0 until ordered.size - 1) {
            if (!isOversight(ordered[i], isWhite)) continue
            oversights++
            if (MoveMetrics.isBestMove(ordered[i + 1], isWhite)) recovered++
        }
        return Result(oversights, recovered)
    }

    private fun isOversight(ev: GameEvaluation, isWhite: Boolean): Boolean =
        when (MoveClassifier.classify(MoveMetrics.cpl(ev, isWhite))) {
            MoveClassifier.Quality.INACCURACY,
            MoveClassifier.Quality.MISTAKE,
            MoveClassifier.Quality.BLUNDER -> true
            else -> false
        }
}
