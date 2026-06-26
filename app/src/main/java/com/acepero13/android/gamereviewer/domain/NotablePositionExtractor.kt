package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.data.model.NotablePosition
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.Move
import kotlin.math.abs

/**
 * Extracts a handful of board-thumbnail-worthy positions from a game by replaying its UCI moves and
 * cross-referencing the cached [GameEvaluation] rows — backs the `NotablePositionCarousel` on the
 * Conversion and Tactics tabs.
 *
 * Flags the user's own decisions only:
 *  - [NotablePosition.Kind.MISSED_SIMPLIFICATION] — winning (≥ +200cp) but played a mistake/blunder.
 *  - [NotablePosition.Kind.TACTIC_FOUND] / [NotablePosition.Kind.TACTIC_MISSED] — a tactical chance
 *    that the user did / didn't convert into the engine's best move.
 */
object NotablePositionExtractor {

    private const val WINNING_CP = 200
    private const val MAX_PER_GAME = 6
    private val TACTIC_MOTIFS = setOf("fork", "hanging", "pin", "skewer", "discovered", "checkmate")

    fun extract(
        gameId: Long,
        uci: List<String>,
        evaluations: List<GameEvaluation>,
        playerIsWhite: Boolean,
    ): List<NotablePosition> {
        val parity = if (playerIsWhite) 1 else 0
        val evalByIndex = evaluations.associateBy { it.moveIndex }
        val board = Board()
        val out = mutableListOf<NotablePosition>()

        for (i in uci.indices) {
            val moveIndex = i + 1
            val fenBefore = board.fen
            val ev = evalByIndex[moveIndex]
            if (ev != null && moveIndex % 2 == parity) {
                classify(ev, playerIsWhite)?.let { kind ->
                    out += NotablePosition(
                        gameId       = gameId,
                        moveIndex    = moveIndex,
                        fen          = fenBefore,
                        kind         = kind.name,
                        playedMove   = uci[i],
                        bestMove     = ev.pvLine.split(' ').firstOrNull().orEmpty(),
                        evalBeforeCp = MoveMetrics.moverEvalBefore(ev, playerIsWhite),
                    )
                }
            }
            val move = runCatching { Move(uci[i], board.sideToMove) }.getOrNull() ?: break
            if (!runCatching { board.doMove(move) }.getOrDefault(false)) break
        }

        // Keep the most decisive moments (largest eval swing magnitude) up to the cap.
        return out.sortedByDescending { abs(it.evalBeforeCp) }.take(MAX_PER_GAME)
    }

    private fun classify(ev: GameEvaluation, isWhite: Boolean): NotablePosition.Kind? {
        val moverEvalBefore = MoveMetrics.moverEvalBefore(ev, isWhite)
        val quality = MoveClassifier.classify(MoveMetrics.cpl(ev, isWhite))
        val blundered = quality == MoveClassifier.Quality.BLUNDER || quality == MoveClassifier.Quality.MISTAKE
        return when {
            moverEvalBefore >= WINNING_CP && blundered -> NotablePosition.Kind.MISSED_SIMPLIFICATION
            ev.motif !in TACTIC_MOTIFS -> null
            MoveMetrics.isBestMove(ev, isWhite) -> NotablePosition.Kind.TACTIC_FOUND
            blundered -> NotablePosition.Kind.TACTIC_MISSED
            else -> null
        }
    }
}
