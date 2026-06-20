package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import kotlin.math.abs

/**
 * How often the user's move matches the engine in **sharp** (tactical / volatile) positions
 * versus **quiet** ones. A move is sharp when a tactical motif is on the board OR the eval
 * swung sharply, otherwise quiet.
 */
object EngineCorrelation {

    /** Absolute eval swing (cp) at/above which a move is considered sharp even without a motif. */
    private const val SHARP_SWING_CP = 100

    data class Result(
        val sharpMoves: Int,
        val sharpBest: Int,
        val quietMoves: Int,
        val quietBest: Int,
    )

    fun compute(userEvals: List<GameEvaluation>, isWhite: Boolean): Result {
        var sharpMoves = 0; var sharpBest = 0
        var quietMoves = 0; var quietBest = 0
        userEvals.forEach { ev ->
            val best = MoveMetrics.isBestMove(ev, isWhite)
            if (isSharp(ev)) {
                sharpMoves++; if (best) sharpBest++
            } else {
                quietMoves++; if (best) quietBest++
            }
        }
        return Result(sharpMoves, sharpBest, quietMoves, quietBest)
    }

    private fun isSharp(ev: GameEvaluation): Boolean =
        ev.motif != "mixed" || abs(ev.evalDelta) >= SHARP_SWING_CP
}
