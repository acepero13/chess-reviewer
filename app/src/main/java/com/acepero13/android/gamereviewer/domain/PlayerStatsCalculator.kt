package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.data.model.MoveTimeData
import com.acepero13.android.gamereviewer.data.model.PlayerStats
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Computes per-side accuracy and quality statistics using the Lichess accuracy formula.
 *
 * Win% = 50 + 50 * (2 / (1 + exp(-0.00368208 * cp)) - 1)
 * Move accuracy = 103.1668 * exp(-0.04354 * (prevWin - win)) - 3.1669,  clamped to [0, 100]
 *
 * evalDelta stored in DB is White-perspective change: negative means White's eval decreased.
 * For Black's moves a positive evalDelta means White gained (Black lost), so we negate for Black.
 */
object PlayerStatsCalculator {

    private fun winChance(cp: Int): Double =
        50.0 + 50.0 * (2.0 / (1.0 + exp(-0.00368208 * cp)) - 1.0)

    /**
     * Accuracy (0–100) of a single move using the Lichess formula. Exposed so the
     * cross-game stats ([GameStatsCalculator]) can compute per-phase accuracy without
     * duplicating the win%/decay math.
     */
    fun moveAccuracy(evalBeforeCp: Int, evalAfterCp: Int, isWhiteMove: Boolean): Float {
        val prevSigned = if (isWhiteMove) evalBeforeCp else -evalBeforeCp
        val signed     = if (isWhiteMove) evalAfterCp  else -evalAfterCp
        val prevWin    = winChance(prevSigned)
        val win        = winChance(signed)
        val raw        = 103.1668 * exp(-0.04354 * (prevWin - win)) - 3.1669
        return max(0.0, min(100.0, raw)).toFloat()
    }

    fun compute(
        game: ReviewGame,
        evaluations: List<GameEvaluation>,
        moveTimes: List<MoveTimeData>,
    ): Pair<PlayerStats, PlayerStats> {
        val (whiteRating, blackRating) = parseRatings(game.pgn)

        val whiteEvals = evaluations.filter { it.moveIndex % 2 == 1 }
        val blackEvals = evaluations.filter { it.moveIndex % 2 == 0 }

        val whiteTimesMap = moveTimes.filter { it.moveIndex % 2 == 1 }.associateBy { it.moveIndex }
        val blackTimesMap = moveTimes.filter { it.moveIndex % 2 == 0 }.associateBy { it.moveIndex }

        val white = computeForSide(
            name      = game.whitePlayer,
            rating    = whiteRating,
            isWhite   = true,
            evals     = whiteEvals,
            timesMap  = whiteTimesMap,
        )
        val black = computeForSide(
            name      = game.blackPlayer,
            rating    = blackRating,
            isWhite   = false,
            evals     = blackEvals,
            timesMap  = blackTimesMap,
        )
        return white to black
    }

    private fun computeForSide(
        name: String,
        rating: String,
        isWhite: Boolean,
        evals: List<GameEvaluation>,
        timesMap: Map<Int, MoveTimeData>,
    ): PlayerStats {
        if (evals.isEmpty()) return PlayerStats(
            name = name, rating = rating, isWhite = isWhite,
            totalMoves = 0, accuracy = 0f, avgClockSeconds = null,
            excellentMovePercent = 0f, goodMovePercent = 0f, blunderRate = 0f,
        )

        val accuracies = evals.map { ev ->
            val evalBefore = ev.evalCp - ev.evalDelta
            moveAccuracy(evalBefore, ev.evalCp, isWhite)
        }

        val avg        = accuracies.average().toFloat()
        val excellentPct = accuracies.count { it >= 90f } * 100f / accuracies.size
        val goodPct      = accuracies.count { it >= 70f } * 100f / accuracies.size
        // playerLoss: White => -evalDelta, Black => +evalDelta  (positive = bad for mover)
        val blunderPct = evals.count { ev ->
            val loss = if (isWhite) -ev.evalDelta else ev.evalDelta
            loss >= 150
        } * 100f / evals.size

        val avgClock = timesMap.values
            .map { it.timeSpentSeconds }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toFloat()

        return PlayerStats(
            name            = name,
            rating          = rating,
            isWhite         = isWhite,
            totalMoves           = evals.size,
            accuracy             = avg,
            avgClockSeconds      = avgClock,
            excellentMovePercent = excellentPct,
            goodMovePercent      = goodPct,
            blunderRate          = blunderPct,
        )
    }

    private val whiteEloRe = Regex("""^\[WhiteElo\s+"([^"]+)"\]""", RegexOption.MULTILINE)
    private val blackEloRe = Regex("""^\[BlackElo\s+"([^"]+)"\]""", RegexOption.MULTILINE)

    private fun parseRatings(pgn: String): Pair<String, String> {
        val w = whiteEloRe.find(pgn)?.groupValues?.get(1)?.takeIf { it != "?" } ?: "?"
        val b = blackEloRe.find(pgn)?.groupValues?.get(1)?.takeIf { it != "?" } ?: "?"
        return w to b
    }
}
