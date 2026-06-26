package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.data.model.MotifTacticStat

/**
 * Turns a game's cached [GameEvaluation] rows into per-motif tactic find-rate
 * ([MotifTacticStat]) from the **user's** perspective — backs the Tactics tab radar.
 *
 * An *opportunity* is a position the user faced where the engine's best move was a tactic of a
 * given motif; it counts as *found* when the user actually played the best move. Motif labels come
 * from [com.acepero13.chess.core.engine.MotifClassifier] (stored on each [GameEvaluation.motif]).
 */
object MotifTacticAggregator {

    /** Engine motif labels that represent a real tactical chance (everything but "mixed"). */
    private val TACTIC_MOTIFS = setOf("fork", "hanging", "pin", "skewer", "discovered", "checkmate")

    /** Display motif key — collapses "checkmate" to "mate"; passes the rest through. */
    private fun normalize(motif: String): String = if (motif == "checkmate") "mate" else motif

    fun aggregate(
        gameId: Long,
        evaluations: List<GameEvaluation>,
        playerIsWhite: Boolean,
    ): List<MotifTacticStat> {
        val parity = if (playerIsWhite) 1 else 0
        return evaluations
            .filter { it.moveIndex % 2 == parity && it.motif in TACTIC_MOTIFS }
            .groupBy { normalize(it.motif) }
            .map { (motif, evs) ->
                MotifTacticStat(
                    gameId        = gameId,
                    motif         = motif,
                    opportunities = evs.size,
                    found         = evs.count { MoveMetrics.isBestMove(it, playerIsWhite) },
                )
            }
    }
}
