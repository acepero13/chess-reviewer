package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.data.model.GameStats
import com.acepero13.android.gamereviewer.data.model.ReviewGame

/**
 * Aggregated, blunder-focused view of the user's analyzed games — backs the **Blunders** tab.
 *
 * All inputs are already-cached aggregates ([GameStats]) and engine-flagged moments
 * ([CriticalMoment]); nothing here re-runs Stockfish. Reframes the same data the Insights tab
 * uses, but tells a single story: *blunders are what cost you games.*
 */
data class BlunderReport(
    val gamesAnalyzed: Int,
    val totalBlunders: Int,
    val blundersPerGame: Float,
    val classification: GameClassCounts,
    /** Share of games with no blunders at all (Spotless + Clean), 0–100. */
    val cleanRatePct: Int,
    val causes: List<BlunderCause>,
    val phaseOpening: Int,
    val phaseMiddlegame: Int,
    val phaseEndgame: Int,
    val direction: BlunderDirection,
    val ratingLeak: RatingLeak,
) {
    val hasData: Boolean get() = gamesAnalyzed > 0
}

/** Per-game quality buckets driving the Clean-Rate donut and classification strip. */
data class GameClassCounts(
    val spotless: Int,
    val clean: Int,
    val flawed: Int,
    val gameThrowing: Int,
) {
    val total: Int = spotless + clean + flawed + gameThrowing
}

/** A single labelled blunder cause (e.g. "King safety") and how often it occurred. */
data class BlunderCause(val label: String, val count: Int)

/**
 * Whether blunders came from the user creating a weakness ([allowed]) or failing to punish the
 * opponent's ([missed]).
 */
data class BlunderDirection(val allowed: Int, val missed: Int) {
    val total: Int = allowed + missed
}

/** Headline "rating leak" estimate — a deliberately rough heuristic, surfaced as such in the UI. */
data class RatingLeak(
    val estimatedEloLost: Int,
    val gamesDecidedByBlunder: Int,
    val lossesFromBlunder: Int,
    val decidedRatePct: Int,
    val headline: String,
    val body: String,
    val coachingTip: String,
)

object BlunderAnalyzer {

    /** A single catastrophic blunder of this size (cp) flips a game; flags it "game-throwing". */
    private const val CATASTROPHIC_CP = 300

    /** Severity (cp) for a flagged moment to count as a blunder at all. */
    private const val BLUNDER_CP = 100

    /** Without flagged moments, this many blunders in one game is treated as game-throwing. */
    private const val MULTI_BLUNDER_THROW = 3

    /**
     * Rough net rating swing of a single game lost to a catastrophic blunder — converting one such
     * loss into a non-loss is worth roughly half a K-factor (K≈32) in Elo terms.
     */
    private const val RATING_PER_LOSS = 16

    private const val COACHING_TIP =
        "When you're ahead or the position is tense, slow down. Before committing to a move, do " +
            "one final blunder check: are any of your pieces left undefended? Is your opponent " +
            "threatening something you missed?"

    fun analyze(
        stats: List<GameStats>,
        moments: List<CriticalMoment>,
        games: List<ReviewGame>,
    ): BlunderReport {
        val blunderMoments = moments.filter {
            it.toType() == CriticalMoment.Type.ENGINE_MARKED && it.severity >= BLUNDER_CP
        }
        val catastrophicByGame = moments
            .filter { it.toType() == CriticalMoment.Type.ENGINE_MARKED && it.severity >= CATASTROPHIC_CP }
            .map { it.gameId }
            .toSet()
        val gamesWithMoments = moments.map { it.gameId }.toSet()
        val resultByGame = games.associateBy({ it.id }, { it.result })

        val classification = classify(stats, catastrophicByGame, gamesWithMoments)
        val causes = causes(blunderMoments, stats)
        val direction = direction(blunderMoments)
        val ratingLeak = ratingLeak(stats, catastrophicByGame, gamesWithMoments, resultByGame)
        val totalBlunders = stats.sumOf { it.blunders }

        return BlunderReport(
            gamesAnalyzed = stats.size,
            totalBlunders = totalBlunders,
            blundersPerGame = if (stats.isEmpty()) 0f else totalBlunders.toFloat() / stats.size,
            classification = classification,
            cleanRatePct = if (classification.total == 0) 0
                else (classification.spotless + classification.clean) * 100 / classification.total,
            causes = causes,
            phaseOpening = blunderMoments.count { it.gamePhase() == Phase.OPENING },
            phaseMiddlegame = blunderMoments.count { it.gamePhase() == Phase.MIDDLEGAME },
            phaseEndgame = blunderMoments.count { it.gamePhase() == Phase.ENDGAME },
            direction = direction,
            ratingLeak = ratingLeak,
        )
    }

    private fun classify(
        stats: List<GameStats>,
        catastrophicByGame: Set<Long>,
        gamesWithMoments: Set<Long>,
    ): GameClassCounts {
        var spotless = 0; var clean = 0; var flawed = 0; var throwing = 0
        stats.forEach { gs ->
            when {
                isGameThrowing(gs, catastrophicByGame, gamesWithMoments) -> throwing++
                gs.blunders == 0 && gs.mistakes == 0 -> spotless++
                gs.blunders == 0 -> clean++
                else -> flawed++
            }
        }
        return GameClassCounts(spotless, clean, flawed, throwing)
    }

    private fun isGameThrowing(
        gs: GameStats,
        catastrophicByGame: Set<Long>,
        gamesWithMoments: Set<Long>,
    ): Boolean = gs.gameId in catastrophicByGame ||
        (gs.gameId !in gamesWithMoments && gs.blunders >= MULTI_BLUNDER_THROW)

    private fun causes(blunderMoments: List<CriticalMoment>, stats: List<GameStats>): List<BlunderCause> {
        val counts = linkedMapOf<String, Int>()
        blunderMoments.groupingBy { it.toReason().causeLabel() }.eachCount()
            .forEach { (label, n) -> counts.merge(label, n, Int::plus) }
        stats.sumOf { it.hangingBlunders }.takeIf { it > 0 }
            ?.let { counts.merge("Hanging material", it, Int::plus) }
        stats.sumOf { it.forkBlunders }.takeIf { it > 0 }
            ?.let { counts.merge("One-move tactic", it, Int::plus) }
        if (counts.isEmpty()) return emptyList()

        // Keep the four biggest causes; fold the rest into "Other" to match the dashboard layout.
        val sorted = counts.entries.sortedByDescending { it.value }
        val top = sorted.take(4).map { BlunderCause(it.key, it.value) }
        val otherCount = sorted.drop(4).sumOf { it.value }
        return if (otherCount > 0) top + BlunderCause("Other", otherCount) else top
    }

    private fun direction(blunderMoments: List<CriticalMoment>): BlunderDirection {
        val missed = blunderMoments.count {
            it.toReason() == CriticalMoment.ReasonCategory.MISSED_WIN ||
                it.toReason() == CriticalMoment.ReasonCategory.MISSED_TACTIC
        }
        return BlunderDirection(allowed = blunderMoments.size - missed, missed = missed)
    }

    private fun ratingLeak(
        stats: List<GameStats>,
        catastrophicByGame: Set<Long>,
        gamesWithMoments: Set<Long>,
        resultByGame: Map<Long, String>,
    ): RatingLeak {
        val thrown = stats.filter { isGameThrowing(it, catastrophicByGame, gamesWithMoments) }
        val losses = thrown.count { gs -> userLost(resultByGame[gs.gameId], gs.playerIsWhite) }
        // Per-game average so the figure reflects ongoing leakage and stays bounded, instead of a
        // runaway lifetime sum that only ever grows as more games are reviewed.
        val elo = if (stats.isEmpty()) 0
            else Math.round(losses.toFloat() * RATING_PER_LOSS / stats.size)
        val decidedRate = if (stats.isEmpty()) 0 else thrown.size * 100 / stats.size

        val body = if (losses == 0) {
            "No game has been decided by a single catastrophic blunder yet — keep that streak going."
        } else {
            "$losses of your ${stats.size} games were decided by a single catastrophic blunder " +
                "(a $decidedRate% rate). On average that's about $elo Elo bled per game."
        }
        return RatingLeak(
            estimatedEloLost = elo,
            gamesDecidedByBlunder = thrown.size,
            lossesFromBlunder = losses,
            decidedRatePct = decidedRate,
            headline = "Single moves are costing you games",
            body = body,
            coachingTip = COACHING_TIP,
        )
    }

    private fun userLost(result: String?, playerIsWhite: Boolean): Boolean = when (result) {
        "1-0" -> !playerIsWhite
        "0-1" -> playerIsWhite
        else  -> false
    }

    // ── Phase bucketing (mirrors InsightsViewModel.gamePhase) ───────────────────────────────────
    private enum class Phase { OPENING, MIDDLEGAME, ENDGAME }

    private fun CriticalMoment.gamePhase(): Phase = when (toReason()) {
        CriticalMoment.ReasonCategory.OPENING_DEVIATION -> Phase.OPENING
        CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE -> Phase.ENDGAME
        else -> when {
            moveIndex <= 15 -> Phase.OPENING
            moveIndex <= 40 -> Phase.MIDDLEGAME
            else            -> Phase.ENDGAME
        }
    }

    private fun CriticalMoment.ReasonCategory.causeLabel(): String = when (this) {
        CriticalMoment.ReasonCategory.MISSED_TACTIC     -> "One-move tactic"
        CriticalMoment.ReasonCategory.OPENING_DEVIATION -> "Opening slip"
        CriticalMoment.ReasonCategory.HANGING_PIECE     -> "Hanging material"
        CriticalMoment.ReasonCategory.KING_SAFETY       -> "King safety"
        CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE -> "Endgame technique"
        CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE -> "Strategic error"
        CriticalMoment.ReasonCategory.TIME_PRESSURE     -> "Time pressure"
        CriticalMoment.ReasonCategory.MISSED_WIN        -> "Missed win"
    }
}
