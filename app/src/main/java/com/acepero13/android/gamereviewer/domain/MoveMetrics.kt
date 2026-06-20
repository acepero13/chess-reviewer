package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.GameEvaluation

/**
 * Shared per-move centipawn math, so [GameStatsCalculator] and the Insights metric helpers
 * ([MoveTimeDistribution], [EngineCorrelation], [RecoveryRate]) all derive eval / CPL the same way.
 */
object MoveMetrics {

    /** White-perspective eval before the move was played. */
    fun evalBefore(ev: GameEvaluation): Int = ev.evalCp - ev.evalDelta

    /** Eval before the move from the mover's own perspective. */
    fun moverEvalBefore(ev: GameEvaluation, isWhite: Boolean): Int =
        if (isWhite) evalBefore(ev) else -evalBefore(ev)

    /** Centipawn loss for the mover (>= 0). */
    fun cpl(ev: GameEvaluation, isWhite: Boolean): Int =
        maxOf(0, if (isWhite) -ev.evalDelta else ev.evalDelta)

    /** A move that closely matches the engine's best line (no meaningful loss). */
    fun isBestMove(ev: GameEvaluation, isWhite: Boolean): Boolean =
        when (MoveClassifier.classify(cpl(ev, isWhite))) {
            MoveClassifier.Quality.BEST, MoveClassifier.Quality.EXCELLENT -> true
            else -> false
        }
}
