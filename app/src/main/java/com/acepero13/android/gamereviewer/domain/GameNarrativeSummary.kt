package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.data.model.ReviewGame

/**
 * Derives a human-readable narrative about a game from its result, player identity,
 * and engine-flagged critical moments.
 *
 * The headline answers "what happened?" in one sentence; the details list gives
 * 2-3 supporting facts the user can act on.
 */
object GameNarrativeSummary {

    enum class GamePhase { OPENING, MIDDLEGAME, ENDGAME }
    enum class Outcome   { WIN, LOSS, DRAW, UNKNOWN }

    data class Summary(
        val headline: String,
        val details:  List<String>,
        val phase:    GamePhase?,
        val outcome:  Outcome,
    )

    // Half-move (ply) boundaries for phase classification
    private const val OPENING_PLY_END    = 20  // ply 1-20  → moves 1-10
    private const val MIDDLEGAME_PLY_END = 60  // ply 21-60 → moves 11-30

    fun build(
        game:            ReviewGame,
        criticalMoments: List<CriticalMoment>,
        username:        String,
    ): Summary {
        val outcome     = resolveOutcome(game, username)
        val userIsWhite = username.isNotBlank() &&
            game.whitePlayer.equals(username, ignoreCase = true)
        val userIsBlack = username.isNotBlank() &&
            game.blackPlayer.equals(username, ignoreCase = true)

        // White moves on odd plies (1,3,5…), Black on even plies (2,4,6…)
        val userMoments = when {
            userIsWhite -> criticalMoments.filter { it.moveIndex % 2 == 1 }
            userIsBlack -> criticalMoments.filter { it.moveIndex % 2 == 0 }
            else        -> criticalMoments
        }

        if (userMoments.isEmpty()) {
            return Summary(
                headline = cleanHeadline(outcome, game, username),
                details  = buildEvalFallbackDetails(criticalMoments, outcome),
                phase    = null,
                outcome  = outcome,
            )
        }

        val phase     = dominantPhase(userMoments)
        val topReason = topReason(userMoments)
        return Summary(
            headline = buildHeadline(outcome, phase, topReason, userMoments),
            details  = buildDetails(userMoments, criticalMoments, phase, topReason, outcome),
            phase    = phase,
            outcome  = outcome,
        )
    }

    // ── Outcome ───────────────────────────────────────────────────────────────

    private fun resolveOutcome(game: ReviewGame, username: String): Outcome {
        val userIsWhite = username.isNotBlank() &&
            game.whitePlayer.equals(username, ignoreCase = true)
        val userIsBlack = username.isNotBlank() &&
            game.blackPlayer.equals(username, ignoreCase = true)
        return when {
            userIsWhite && game.result == "1-0"   -> Outcome.WIN
            userIsWhite && game.result == "0-1"   -> Outcome.LOSS
            userIsBlack && game.result == "0-1"   -> Outcome.WIN
            userIsBlack && game.result == "1-0"   -> Outcome.LOSS
            game.result == "1/2-1/2"              -> Outcome.DRAW
            game.result == "1-0"                  -> Outcome.WIN
            game.result == "0-1"                  -> Outcome.LOSS
            else                                  -> Outcome.UNKNOWN
        }
    }

    // ── Phase & reason ────────────────────────────────────────────────────────

    private fun phaseOf(ply: Int) = when {
        ply <= OPENING_PLY_END    -> GamePhase.OPENING
        ply <= MIDDLEGAME_PLY_END -> GamePhase.MIDDLEGAME
        else                      -> GamePhase.ENDGAME
    }

    private fun dominantPhase(moments: List<CriticalMoment>): GamePhase =
        moments.groupBy { phaseOf(it.moveIndex) }
            .maxByOrNull { (_, list) -> list.sumOf { it.severity } }
            ?.key ?: GamePhase.MIDDLEGAME

    private fun topReason(moments: List<CriticalMoment>): CriticalMoment.ReasonCategory =
        moments.groupBy { it.toReason() }
            .maxByOrNull { (_, list) -> list.sumOf { it.severity } }
            ?.key ?: CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE

    // ── Headline ──────────────────────────────────────────────────────────────

    private fun buildHeadline(
        outcome:    Outcome,
        phase:      GamePhase,
        reason:     CriticalMoment.ReasonCategory,
        userMoments: List<CriticalMoment>,
    ): String {
        val phaseName = phase.name.lowercase()
        return when (outcome) {
            Outcome.LOSS -> when (reason) {
                CriticalMoment.ReasonCategory.MISSED_TACTIC,
                CriticalMoment.ReasonCategory.HANGING_PIECE ->
                    "You missed a tactical opportunity that cost you the game"
                CriticalMoment.ReasonCategory.TIME_PRESSURE ->
                    "Time pressure led to the decisive mistake in the $phaseName"
                CriticalMoment.ReasonCategory.KING_SAFETY ->
                    "King safety issues in the $phaseName proved decisive"
                CriticalMoment.ReasonCategory.OPENING_DEVIATION ->
                    "An opening mistake set you back from the start"
                CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE ->
                    "The endgame technique let you down"
                CriticalMoment.ReasonCategory.MISSED_WIN ->
                    "You had a winning position but couldn't convert it"
                else ->
                    "You lost this game in the $phaseName"
            }
            Outcome.WIN -> when {
                userMoments.isEmpty() -> "You won — a clean performance"
                userMoments.sumOf { it.severity } > 500 ->
                    "You won despite some shaky moments in the $phaseName"
                else -> "You won this game in the $phaseName"
            }
            Outcome.DRAW -> when (reason) {
                CriticalMoment.ReasonCategory.MISSED_WIN ->
                    "You had winning chances in the $phaseName but the game ended in a draw"
                else ->
                    "The game was drawn — critical moments balanced out"
            }
            Outcome.UNKNOWN -> "A critical moment in the $phaseName shaped this game"
        }
    }

    private fun cleanHeadline(outcome: Outcome, game: ReviewGame, username: String): String {
        return when (outcome) {
            Outcome.WIN    -> "Well played — no major mistakes detected"
            Outcome.LOSS   -> "A tough game — no critical blunders were flagged"
            Outcome.DRAW   -> "A balanced game ended in a draw"
            Outcome.UNKNOWN -> "No critical moments were detected in this game"
        }
    }

    // ── Details ───────────────────────────────────────────────────────────────

    private fun buildDetails(
        userMoments:  List<CriticalMoment>,
        allMoments:   List<CriticalMoment>,
        phase:        GamePhase,
        topReason:    CriticalMoment.ReasonCategory,
        outcome:      Outcome,
    ): List<String> {
        val details = mutableListOf<String>()

        // Critical moment count + move range
        val inPhase = userMoments.filter { phaseOf(it.moveIndex) == phase }
        if (inPhase.isNotEmpty()) {
            val firstMove = (inPhase.first().moveIndex + 1) / 2
            val lastMove  = (inPhase.last().moveIndex  + 1) / 2
            val moveRange = if (firstMove == lastMove) "move $firstMove"
                            else "moves $firstMove–$lastMove"
            val count = inPhase.size
            details += "$count critical mistake${if (count > 1) "s" else ""} in the ${phase.name.lowercase()} ($moveRange)"
        }

        // Primary weakness label
        details += "Primary weakness: ${reasonLabel(topReason)}"

        // Largest eval swing across the whole game
        val worstMoment = allMoments.maxByOrNull { it.severity }
        if (worstMoment != null && worstMoment.severity >= 200) {
            val fullMove = (worstMoment.moveIndex + 1) / 2
            val pawns    = "%.1f".format(worstMoment.severity / 100.0)
            details += "Biggest swing: ~${pawns} pawns at move $fullMove"
        }

        return details
    }

    private fun buildEvalFallbackDetails(
        moments: List<CriticalMoment>,
        outcome: Outcome,
    ): List<String> {
        if (moments.isEmpty()) return listOf("Run engine analysis to unlock insights.")
        val worstMoment = moments.maxByOrNull { it.severity }
        val details = mutableListOf<String>()
        if (worstMoment != null && worstMoment.severity >= 200) {
            val fullMove = (worstMoment.moveIndex + 1) / 2
            val pawns    = "%.1f".format(worstMoment.severity / 100.0)
            details += "Biggest swing: ~${pawns} pawns at move $fullMove (${phaseOf(worstMoment.moveIndex).name.lowercase()})"
        }
        details += "${moments.size} engine-flagged moment${if (moments.size > 1) "s" else ""} in this game"
        return details
    }

    private fun reasonLabel(reason: CriticalMoment.ReasonCategory) = when (reason) {
        CriticalMoment.ReasonCategory.MISSED_TACTIC     -> "Missed tactical shots"
        CriticalMoment.ReasonCategory.OPENING_DEVIATION -> "Opening preparation gap"
        CriticalMoment.ReasonCategory.HANGING_PIECE     -> "Hanging pieces"
        CriticalMoment.ReasonCategory.KING_SAFETY       -> "King safety"
        CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE -> "Endgame technique"
        CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE -> "Strategic miscalculation"
        CriticalMoment.ReasonCategory.TIME_PRESSURE     -> "Time pressure decisions"
        CriticalMoment.ReasonCategory.MISSED_WIN        -> "Missed winning chances"
    }
}
