package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.CriticalMoment

/**
 * Computes a post-session coaching summary from data across a batch of recently-reviewed games.
 *
 * A "session" is defined as all games imported within the last 24 hours.  The summary surfaces:
 * - Time-based patterns (rushed blunders, slow blunders) from [TimeAnalyzer]
 * - The top behavioural failure trend from [BehavioralDiagnostic]
 * - A personalised coaching message + a concrete drill recommendation for next session
 */
object SessionDebrief {

    private const val SESSION_WINDOW_MS = 24 * 60 * 60 * 1000L

    fun sessionCutoff(): Long = System.currentTimeMillis() - SESSION_WINDOW_MS

    data class Summary(
        val gameCount: Int,
        val hasTimeData: Boolean,
        val totalFastMoves: Int,
        val rushedBlunders: Int,
        val carefulBlunders: Int,
        val totalBlunders: Int,
        val topTrend: BehavioralDiagnostic.FailureTrend?,
        val coachMessage: String,
        val drillTitle: String,
        val drillCategoryNames: List<String>,
    ) {
        val hasData: Boolean get() = gameCount > 0
    }

    fun summarize(
        gameCount: Int,
        allDecisions: List<TimeAnalyzer.MoveDecision>,
        sessionMoments: List<CriticalMoment>,
    ): Summary {
        val hasTime       = allDecisions.isNotEmpty()
        val rushed        = TimeAnalyzer.countRushedBlunders(allDecisions)
        val careful       = TimeAnalyzer.countCarefulBlunders(allDecisions)
        val totalBlunders = allDecisions.count { it.isBlunder }
        val fastMoves     = allDecisions.count { it.timeSpentSeconds <= TimeAnalyzer.FAST_MOVE_SECONDS }

        val topTrend = BehavioralDiagnostic.diagnose(sessionMoments, topN = 1).firstOrNull()

        val (message, drillTitle, drillCats) = buildCoachingPlan(
            gameCount     = gameCount,
            hasTime       = hasTime,
            rushed        = rushed,
            careful       = careful,
            fastMoves     = fastMoves,
            totalBlunders = totalBlunders,
            topTrend      = topTrend,
        )

        return Summary(
            gameCount          = gameCount,
            hasTimeData        = hasTime,
            totalFastMoves     = fastMoves,
            rushedBlunders     = rushed,
            carefulBlunders    = careful,
            totalBlunders      = totalBlunders,
            topTrend           = topTrend,
            coachMessage       = message,
            drillTitle         = drillTitle,
            drillCategoryNames = drillCats,
        )
    }

    private data class CoachingPlan(
        val message: String,
        val drillTitle: String,
        val drillCats: List<String>,
    )

    private fun buildCoachingPlan(
        gameCount: Int,
        hasTime: Boolean,
        rushed: Int,
        careful: Int,
        fastMoves: Int,
        totalBlunders: Int,
        topTrend: BehavioralDiagnostic.FailureTrend?,
    ): CoachingPlan {
        val gamesLabel = if (gameCount == 1) "1 game" else "$gameCount games"

        // Primary signal: rushed blunders dominate
        if (hasTime && rushed >= 2 && rushed > careful) {
            val pct = if (fastMoves > 0) (rushed * 100) / fastMoves else 0
            return CoachingPlan(
                message = "In today's session across $gamesLabel, you made $rushed moves " +
                    "in under ${TimeAnalyzer.FAST_MOVE_SECONDS}s that turned into blunders " +
                    "($pct% of your fast moves). This points to an Impulse Control gap — " +
                    "playing before the position has been fully checked. " +
                    "Start tomorrow's session with slow, methodical piece-safety checks before each move.",
                drillTitle = "Impulse Control — Pre-Move Safety",
                drillCats  = listOf(
                    CriticalMoment.ReasonCategory.MISSED_TACTIC.name,
                    CriticalMoment.ReasonCategory.HANGING_PIECE.name,
                ),
            )
        }

        // Secondary signal: careful blunders dominate (slow moves still fail)
        if (hasTime && careful >= 2 && careful >= rushed) {
            return CoachingPlan(
                message = "In today's session across $gamesLabel, you spent significant time " +
                    "on $careful moves that still ended in blunders. Spending time doesn't help " +
                    "when the candidate selection is wrong. " +
                    "Start tomorrow's session by practising elimination of bad candidates first.",
                drillTitle = "Calculation Accuracy — Candidate Screening",
                drillCats  = listOf(
                    CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE.name,
                    CriticalMoment.ReasonCategory.MISSED_TACTIC.name,
                ),
            )
        }

        // Fallback: use topTrend from engine analysis
        if (topTrend != null) {
            val cats = topTrend.triggerCategories.map { it.name }
            return CoachingPlan(
                message = "In today's session across $gamesLabel, your most recurring issue " +
                    "was \"${topTrend.title}\". ${topTrend.description} " +
                    "Focus your next session on the targeted drill below.",
                drillTitle = "${topTrend.title} Drill",
                drillCats  = cats,
            )
        }

        // No data yet
        return CoachingPlan(
            message = "You've reviewed $gamesLabel in this session. " +
                "Import games with clock data or analyse more positions to unlock personalised coaching.",
            drillTitle = "",
            drillCats  = emptyList(),
        )
    }
}
