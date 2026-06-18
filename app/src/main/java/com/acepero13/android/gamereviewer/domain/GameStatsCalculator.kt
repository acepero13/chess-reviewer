package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.data.model.GameStats
import com.acepero13.android.gamereviewer.data.model.MoveTimeData
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import kotlin.math.sqrt

/**
 * Computes Chess.com-style per-game statistics from the **user's** perspective,
 * reusing the Lichess accuracy math in [PlayerStatsCalculator] and the move
 * quality thresholds in [MoveClassifier].
 *
 * Output is a single [GameStats] row cached by the shallow background analysis pass.
 */
object GameStatsCalculator {

    // Phase boundaries by half-move index — aligned with GameReportViewModel.computePhaseAccuracy.
    private const val OPENING_LAST_PLY = 20
    private const val MIDDLEGAME_LAST_PLY = 60

    /** Mover-perspective eval (cp) above which a middlegame move counts as "attacking". */
    private const val ATTACK_CP = 50
    /** Mover-perspective eval (cp) below which a middlegame move counts as "defensive". */
    private const val DEFENSE_CP = -50
    /** Mover-perspective eval (cp) above which the position is "winning" (conversion). */
    private const val WINNING_CP = 200

    /**
     * @param username the player's handle (from SettingsRepository). When it matches neither
     *                 side (or is blank) the user is assumed to be White.
     */
    fun compute(
        game: ReviewGame,
        evaluations: List<GameEvaluation>,
        moveTimes: List<MoveTimeData>,
        username: String,
        analysisDepth: Int,
    ): GameStats {
        val playerIsWhite = when {
            game.blackPlayer.equals(username.trim(), ignoreCase = true) &&
                !game.whitePlayer.equals(username.trim(), ignoreCase = true) -> false
            else -> true
        }

        // The user's own moves: White plays odd half-moves, Black plays even.
        val parity = if (playerIsWhite) 1 else 0
        val userEvals = evaluations.filter { it.moveIndex % 2 == parity }

        val accuracies = userEvals.map { accuracyOf(it, playerIsWhite) }
        val avgAccuracy = accuracies.avgOrZero()

        val cpls = userEvals.map { cplOf(it, playerIsWhite) }
        val acpl = if (cpls.isEmpty()) 0 else cpls.average().toInt()

        var blunders = 0; var mistakes = 0; var inaccuracies = 0
        cpls.forEach { cpl ->
            when (MoveClassifier.classify(cpl)) {
                MoveClassifier.Quality.BLUNDER    -> blunders++
                MoveClassifier.Quality.MISTAKE    -> mistakes++
                MoveClassifier.Quality.INACCURACY -> inaccuracies++
                else -> {}
            }
        }

        val opening    = userEvals.filter { it.moveIndex <= OPENING_LAST_PLY }
        val middlegame = userEvals.filter { it.moveIndex in (OPENING_LAST_PLY + 1)..MIDDLEGAME_LAST_PLY }
        val endgame    = userEvals.filter { it.moveIndex > MIDDLEGAME_LAST_PLY }

        // Attacking vs defensive middlegame: bucket by the eval *before* the move,
        // viewed from the mover's perspective.
        val attackMg  = middlegame.filter { moverEvalBefore(it, playerIsWhite) >= ATTACK_CP }
        val defenseMg = middlegame.filter { moverEvalBefore(it, playerIsWhite) <= DEFENSE_CP }
        val winning   = userEvals.filter { moverEvalBefore(it, playerIsWhite) >= WINNING_CP }

        val decisions = TimeAnalyzer.analyze(evaluations, moveTimes)
            .filter { it.moveIndex % 2 == parity }
        val rushedBlunderRate =
            if (decisions.isEmpty()) 0f
            else TimeAnalyzer.countRushedBlunders(decisions).toFloat() / decisions.size

        return GameStats(
            gameId                    = game.id,
            playerIsWhite             = playerIsWhite,
            accuracy                  = avgAccuracy,
            acpl                      = acpl,
            blunders                  = blunders,
            mistakes                  = mistakes,
            inaccuracies              = inaccuracies,
            openingAccuracy           = opening.accuracy(playerIsWhite),
            middlegameAccuracy        = middlegame.accuracy(playerIsWhite),
            endgameAccuracy           = endgame.accuracy(playerIsWhite),
            middlegameAttackAccuracy  = attackMg.accuracy(playerIsWhite),
            middlegameDefenseAccuracy = defenseMg.accuracy(playerIsWhite),
            conversionAccuracy        = winning.accuracy(playerIsWhite),
            accuracyStdDev            = accuracies.stdDev(),
            rushedBlunderRate         = rushedBlunderRate,
            openingEco                = game.openingEco,
            openingName               = game.openingName,
            analysisDepth             = analysisDepth,
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** White-perspective eval before the move was played. */
    private fun evalBefore(ev: GameEvaluation): Int = ev.evalCp - ev.evalDelta

    /** Eval before the move from the mover's own perspective. */
    private fun moverEvalBefore(ev: GameEvaluation, isWhite: Boolean): Int =
        if (isWhite) evalBefore(ev) else -evalBefore(ev)

    private fun accuracyOf(ev: GameEvaluation, isWhite: Boolean): Float =
        PlayerStatsCalculator.moveAccuracy(evalBefore(ev), ev.evalCp, isWhite)

    /** Centipawn loss for the mover (>= 0). */
    private fun cplOf(ev: GameEvaluation, isWhite: Boolean): Int =
        maxOf(0, if (isWhite) -ev.evalDelta else ev.evalDelta)

    private fun List<GameEvaluation>.accuracy(isWhite: Boolean): Float =
        map { accuracyOf(it, isWhite) }.avgOrZero()

    private fun List<Float>.avgOrZero(): Float =
        if (isEmpty()) 0f else average().toFloat()

    private fun List<Float>.stdDev(): Float {
        if (size < 2) return 0f
        val mean = average().toFloat()
        return sqrt(map { (it - mean) * (it - mean) }.average().toFloat())
    }
}
