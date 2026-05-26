package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.CriticalMoment

/**
 * Aggregates [CriticalMoment] records across **all** imported games to surface the
 * player's top recurring psychological failure patterns (Task 4.3).
 *
 * The failure taxonomy maps [CriticalMoment.ReasonCategory] values to named failure
 * archetypes and then ranks them by frequency.  Two special compound syndromes are
 * also detected:
 *
 * - **Time Pressure Syndrome** — player has ≥ 3 blunders tagged [TIME_PRESSURE].
 * - **Wishful Thinking**       — player frequently marks a position as critical (USER_MARKED)
 *                                 but the engine also marks it, yet the severity is low,
 *                                 suggesting over-confidence in quiet positions.
 */
object BehavioralDiagnostic {

    // ── Model ───────────────────────────────────────────────────────────────────

    /**
     * One identified failure archetype, ranked by occurrence across all games.
     *
     * @param rank        1-based rank (1 = most frequent failure).
     * @param emoji       Decorative emoji for the profile card.
     * @param title       Short display name (e.g. "Tactical Blindspot").
     * @param description Concise coaching note explaining the pattern and how to address it.
     * @param frequency   Number of games in which this pattern appeared.
     * @param totalCount  Raw occurrence count across all games.
     */
    data class FailureTrend(
        val rank: Int,
        val emoji: String,
        val title: String,
        val description: String,
        val frequency: Int,    // distinct-game count
        val totalCount: Int,   // raw occurrence count
    )

    // ── Archetype definitions ────────────────────────────────────────────────

    private data class Archetype(
        val id: String,
        val emoji: String,
        val title: String,
        val description: String,
        val triggers: Set<CriticalMoment.ReasonCategory>,
    )

    private val ARCHETYPES = listOf(
        Archetype(
            id          = "tactical_blindspot",
            emoji       = "🎯",
            title       = "Tactical Blindspot",
            description = "You consistently miss forcing sequences — forks, pins, and skewers. " +
                "Before each move, ask: can my opponent capture anything undefended? " +
                "Solve 10 puzzles daily to sharpen pattern recognition.",
            triggers    = setOf(
                CriticalMoment.ReasonCategory.MISSED_TACTIC,
                CriticalMoment.ReasonCategory.HANGING_PIECE,
            ),
        ),
        Archetype(
            id          = "king_safety_neglect",
            emoji       = "👑",
            title       = "King Safety Neglect",
            description = "Your king is repeatedly left exposed in critical moments. " +
                "Prioritise completing your kingside development and castling early. " +
                "Always count attackers vs. defenders on your king's file before committing.",
            triggers    = setOf(
                CriticalMoment.ReasonCategory.KING_SAFETY,
            ),
        ),
        Archetype(
            id          = "opening_drift",
            emoji       = "🗺️",
            title       = "Opening Blueprint Drift",
            description = "Your games regularly derail from sound opening principles in the first 15 moves. " +
                "Study the core ideas behind your preferred openings rather than memorising lines. " +
                "A wrong plan in a known position is often worse than an unknown position with good principles.",
            triggers    = setOf(
                CriticalMoment.ReasonCategory.OPENING_DEVIATION,
            ),
        ),
        Archetype(
            id          = "endgame_weakness",
            emoji       = "♟️",
            title       = "Endgame Execution Weakness",
            description = "You frequently squander winning endgame positions. " +
                "Study Rook endgame techniques (Lucena, Philidor) and King + Pawn endings. " +
                "The endgame rewards precision over creativity; one tempo can decide the game.",
            triggers    = setOf(
                CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE,
            ),
        ),
        Archetype(
            id          = "missed_win",
            emoji       = "🏆",
            title       = "Missed Winning Moments",
            description = "You often transition into a won game but miss the decisive blow. " +
                "When your opponent has no active counterplay, look for forcing moves — checks, " +
                "captures, threats — before deciding on a positional plan.",
            triggers    = setOf(
                CriticalMoment.ReasonCategory.MISSED_WIN,
            ),
        ),
        Archetype(
            id          = "time_pressure",
            emoji       = "⏱️",
            title       = "Time Pressure Syndrome",
            description = "You accumulate clock debt early, then blunder when seconds remain. " +
                "Use no more than 5 minutes on any single move unless you are calculating a forced line. " +
                "Practice with increment to build a sustainable pace.",
            triggers    = setOf(
                CriticalMoment.ReasonCategory.TIME_PRESSURE,
            ),
        ),
        Archetype(
            id          = "strategic_drift",
            emoji       = "🧭",
            title       = "Strategic Drift",
            description = "Your moves frequently lack a cohesive plan; you react rather than direct. " +
                "Before every move, name your plan in one sentence. " +
                "Study masterclass games in your favourite structure to internalise long-term thinking.",
            triggers    = setOf(
                CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE,
            ),
        ),
    )

    // ── Public API ──────────────────────────────────────────────────────────────

    /**
     * Derives up to [topN] (default 3) recurring failure trends from the full
     * history of [moments].
     *
     * Algorithm:
     * 1. Map each ENGINE_MARKED moment to its matching [Archetype] (by ReasonCategory).
     * 2. Count occurrences per archetype and the number of distinct games affected.
     * 3. Sort by total occurrence count descending.
     * 4. Assign rank 1, 2, 3 and return.
     *
     * Returns an empty list if no engine-marked moments exist.
     */
    fun diagnose(
        moments: List<CriticalMoment>,
        topN: Int = 3,
    ): List<FailureTrend> {
        val engineMoments = moments.filter { it.type == CriticalMoment.Type.ENGINE_MARKED.name }
        if (engineMoments.isEmpty()) return emptyList()

        // Count occurrences and distinct games per archetype
        data class AccRecord(var totalCount: Int = 0, val gameIds: MutableSet<Long> = mutableSetOf())
        val acc = mutableMapOf<String, AccRecord>()

        for (moment in engineMoments) {
            val reason = moment.toReason()
            for (arch in ARCHETYPES) {
                if (reason in arch.triggers) {
                    acc.getOrPut(arch.id) { AccRecord() }.let {
                        it.totalCount++
                        it.gameIds.add(moment.gameId)
                    }
                }
            }
        }

        return ARCHETYPES
            .mapNotNull { arch -> acc[arch.id]?.let { arch to it } }
            .sortedByDescending { (_, rec) -> rec.totalCount }
            .take(topN)
            .mapIndexed { rankIdx, (arch, rec) ->
                FailureTrend(
                    rank        = rankIdx + 1,
                    emoji       = arch.emoji,
                    title       = arch.title,
                    description = arch.description,
                    frequency   = rec.gameIds.size,
                    totalCount  = rec.totalCount,
                )
            }
    }

    /**
     * Returns `true` if the player shows signs of "Wishful Thinking":
     * they USER_MARKED ≥ [threshold] positions as critical that the engine
     * considered low-severity (< 50 cp), implying they over-estimated threats.
     */
    fun hasWishfulThinking(moments: List<CriticalMoment>, threshold: Int = 3): Boolean {
        val overlap = moments.count { m ->
            m.type == CriticalMoment.Type.USER_MARKED.name && m.severity < 50
        }
        return overlap >= threshold
    }
}

