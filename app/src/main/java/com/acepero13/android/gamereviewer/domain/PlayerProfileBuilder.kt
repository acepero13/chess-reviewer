package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.data.model.GameStats
import kotlin.math.roundToInt

/**
 * One axis of the radar player-style profile.
 *
 * @param value 0–100 score (higher = stronger).
 * @param delta change vs the previous window of games (positive = improving).
 */
data class ProfileAxis(
    val name: String,
    val value: Int,
    val delta: Float,
)

data class PlayerProfile(
    val axes: List<ProfileAxis>,
    val archetype: String,
    val archetypeDescription: String,
    val gamesAnalyzed: Int,
) {
    fun axis(name: String): ProfileAxis? = axes.firstOrNull { it.name == name }
}

/**
 * Aggregates cached [GameStats] (plus engine-marked [CriticalMoment]s) into the
 * 9-axis player-style profile shown on the Insights tab.
 *
 * Axis scores are 0–100. Deltas compare the most-recent [WINDOW] games against the
 * window before that; with fewer than 2×[WINDOW] games all deltas are 0.
 *
 * The heuristics here are intentionally simple and tunable — they describe tendencies,
 * not absolute strength.
 */
object PlayerProfileBuilder {

    /** Number of most-recent games forming the "recent" trend window. */
    const val WINDOW = 10

    // Axis names (kept as constants so UI ordering matches the requested layout).
    const val MENTAL_STABILITY = "mental stability"
    const val CONVERSION = "conversion"
    const val TACTICS = "tactics"
    const val STRATEGY = "strategy"
    const val ATTACK = "attack"
    const val ENDGAME = "endgame"
    const val DEFENSE = "defense"
    const val OPENING = "opening"
    const val TIME_MANAGEMENT = "time management"

    /**
     * @param statsNewestFirst cached game stats ordered most-recent first (GameStatsDao.getAll()).
     * @param moments all engine-marked critical moments (for tactics/strategy axes).
     */
    fun build(
        statsNewestFirst: List<GameStats>,
        moments: List<CriticalMoment>,
    ): PlayerProfile? {
        if (statsNewestFirst.isEmpty()) return null

        val momentsByGame = moments
            .filter { it.type == CriticalMoment.Type.ENGINE_MARKED.name }
            .groupBy { it.gameId }

        val overall = axesFor(statsNewestFirst, momentsByGame)

        // Trend: recent window vs the window immediately before it.
        val deltas: Map<String, Float> =
            if (statsNewestFirst.size >= 2 * WINDOW) {
                val recent = axesFor(statsNewestFirst.take(WINDOW), momentsByGame)
                val prior  = axesFor(statsNewestFirst.drop(WINDOW).take(WINDOW), momentsByGame)
                recent.mapValues { (k, v) -> v - (prior[k] ?: v) }
            } else {
                emptyMap()
            }

        val axes = AXIS_ORDER.map { name ->
            ProfileAxis(
                name  = name,
                value = (overall[name] ?: 0f).roundToInt().coerceIn(0, 100),
                delta = deltas[name] ?: 0f,
            )
        }
        val (archetype, description) = deriveArchetype(overall)
        return PlayerProfile(axes, archetype, description, statsNewestFirst.size)
    }

    private val AXIS_ORDER = listOf(
        MENTAL_STABILITY, CONVERSION, TACTICS, STRATEGY, ATTACK,
        ENDGAME, DEFENSE, OPENING, TIME_MANAGEMENT,
    )

    /** Raw 0–100 axis values for a subset of games. */
    private fun axesFor(
        stats: List<GameStats>,
        momentsByGame: Map<Long, List<CriticalMoment>>,
    ): Map<String, Float> {
        if (stats.isEmpty()) return emptyMap()
        val n = stats.size

        fun avg(selector: (GameStats) -> Float) =
            stats.map(selector).filter { it > 0f }.let { if (it.isEmpty()) 0f else it.average().toFloat() }

        // Critical-moment rates per game, by reason category.
        val gameIds = stats.map { it.gameId }.toSet()
        val relevant = momentsByGame.filterKeys { it in gameIds }.values.flatten()
        fun ratePerGame(vararg cats: CriticalMoment.ReasonCategory): Float {
            val names = cats.map { it.name }.toSet()
            return relevant.count { it.reasonCategory in names }.toFloat() / n
        }

        // Map a "mistakes per game" rate to a 0–100 score (0 mistakes → 100, ~3+/game → 0).
        fun rateToScore(rate: Float, fullPenaltyAt: Float = 3f): Float =
            (100f * (1f - (rate / fullPenaltyAt))).coerceIn(0f, 100f)

        val tactics = rateToScore(
            ratePerGame(CriticalMoment.ReasonCategory.MISSED_TACTIC, CriticalMoment.ReasonCategory.HANGING_PIECE),
            fullPenaltyAt = 2f,
        )
        val strategyFromMoments = rateToScore(
            ratePerGame(CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE),
            fullPenaltyAt = 2f,
        )
        // Blend strategy signal with raw middlegame accuracy.
        val strategy = (strategyFromMoments + avg { it.middlegameAccuracy }) / 2f

        val stability = run {
            // Lower per-move accuracy variance → steadier. Std-dev ~30 → fully unstable.
            val avgStd = stats.map { it.accuracyStdDev }.average().toFloat()
            (100f * (1f - (avgStd / 30f))).coerceIn(0f, 100f)
        }

        val timeMgmt = run {
            val avgRushed = stats.map { it.rushedBlunderRate }.average().toFloat()
            (100f * (1f - avgRushed / 0.25f)).coerceIn(0f, 100f)
        }

        return mapOf(
            OPENING          to avg { it.openingAccuracy },
            ENDGAME          to avg { it.endgameAccuracy },
            ATTACK           to avg { it.middlegameAttackAccuracy },
            DEFENSE          to avg { it.middlegameDefenseAccuracy },
            CONVERSION       to avg { it.conversionAccuracy },
            TACTICS          to tactics,
            STRATEGY         to strategy,
            MENTAL_STABILITY to stability,
            TIME_MANAGEMENT  to timeMgmt,
        )
    }

    /** Rule-based style label from the axis vector. Picks the strongest distinguishing signal. */
    private fun deriveArchetype(axes: Map<String, Float>): Pair<String, String> {
        fun v(name: String) = axes[name] ?: 0f
        val attack = v(ATTACK); val defense = v(DEFENSE)
        val tactics = v(TACTICS); val strategy = v(STRATEGY)
        val endgame = v(ENDGAME); val stability = v(MENTAL_STABILITY)
        val timeMgmt = v(TIME_MANAGEMENT)

        return when {
            attack - defense >= 15 && tactics >= 55 ->
                "Aggressive Attacker" to "You thrive pressing the initiative and finding tactics, but watch your play when forced to defend."
            defense - attack >= 15 && endgame >= 60 ->
                "Solid Grinder" to "You defend resiliently and convert endgames — you win by outlasting opponents rather than overwhelming them."
            strategy - tactics >= 15 ->
                "Positional Player" to "You navigate strategic middlegames well, but sharp tactical positions are a relative weakness."
            tactics - strategy >= 15 ->
                "Tactician" to "You spot concrete tactics readily; investing more in long-term planning would round out your game."
            timeMgmt < 45 ->
                "Time Scrambler" to "Rushed decisions cost you points — slowing down in critical moments is your biggest lever."
            stability < 45 ->
                "Streaky Player" to "Your accuracy swings game to game; building consistency will raise your floor."
            else ->
                "All-Rounder" to "A balanced profile with no glaring weakness — keep sharpening your strongest phases."
        }
    }
}
