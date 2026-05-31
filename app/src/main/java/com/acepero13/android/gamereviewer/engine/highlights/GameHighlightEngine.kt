package com.acepero13.android.gamereviewer.engine.highlights

import com.acepero13.android.gamereviewer.data.model.MoveTimeData
import com.acepero13.android.gamereviewer.domain.TruthMapEntry
import com.acepero13.android.gamereviewer.engine.highlights.rules.*

private const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
private const val CP_TO_PAWNS = 100f

/**
 * Builds a [HighlightRuleContext] chain from the truth map and move data,
 * then runs every registered [HighlightRule] to produce [GameHighlight]s.
 */
object GameHighlightEngine {

    private val rules: List<HighlightRule> = listOf(
        // Evaluation-drop family
        EvaluationDropRule(),
        BlunderedPieceRule(),
        MomentumShiftRule(),
        InitiativeRule(),
        PositionalImprovementRule(),
        // Tactics
        ForkRule(),
        PinRule(),
        SkewerRule(),
        DiscoveredAttackRule(),
        ZwischenzugRule(),
        ForcingCombinationRule(),
        TacticalResourceRule(),
        BatteryRule(),
        BreakthroughSacrificeRule(),
        ExchangeSacrificeRule(),
        ExchangeRule(),
        // Positional
        BackRankWeaknessRule(),
        KnightOutpostRule(),
        WeakSquareRule(),
        BishopPairRule(),
        CentralizationRule(),
        IsolatedPawnRule(),
        MaterialImbalanceRule(),
        PawnBreakRule(),
        PawnPromotionThreatRule(),
        PawnStormRule(),
        PieceCoordinationRule(),
        RookLiftRule(),
        SimplificationRule(),
        TempoGainRule(),
        // King
        KingActivityRule(),
        // Endgame
        ZugzwangRule(),
        PerpetualCheckRule(),
        // Structural
        CastlingRule(),
        // Time management
        RushedBlunderRule(),
        OverthinkingRule(),
        TimePressureMistakeRule(),
        CarefulBlunderRule(),
        QuickBrilliantMoveRule(),
    ).distinctBy { it.ruleType }

    /**
     * Runs all rules over the game and returns every highlight produced.
     *
     * @param truthMap  One entry per half-move, in order (moveIndex 1-based).
     * @param sanMoves  SAN for each half-move (index 0 = move 1).
     * @param fenSequence FEN list (index 0 = start position, index n = after move n).
     * @param moveTimes Optional clock data; keyed by moveIndex.
     */
    fun run(
        truthMap: List<TruthMapEntry>,
        sanMoves: List<String>,
        fenSequence: List<String>,
        moveTimes: Map<Int, MoveTimeData> = emptyMap(),
    ): List<GameHighlight> {
        if (truthMap.isEmpty()) return emptyList()

        // Build context list (without prev/next links yet)
        val contexts = truthMap.mapIndexed { i, entry ->
            buildContext(entry, i, sanMoves, fenSequence, moveTimes, truthMap)
        }

        // Link prev/next — done as a mutable patching step
        val linked = Array<HighlightRuleContext?>(contexts.size) { null }
        contexts.forEachIndexed { i, ctx ->
            linked[i] = ctx.copy(
                prevContext = if (i > 0) linked[i - 1] else null,
            )
        }
        // Back-fill nextContext references
        for (i in linked.indices.reversed()) {
            val cur  = linked[i] ?: continue
            val next = if (i + 1 < linked.size) linked[i + 1] else null
            linked[i] = cur.copy(nextContext = next)
        }

        // Run rules
        return linked.filterNotNull().flatMap { ctx ->
            rules.flatMap { rule ->
                runCatching { rule.evaluate(ctx) }.getOrDefault(emptyList())
            }
        }
    }

    // ── Context builder ───────────────────────────────────────────────────────────

    private fun buildContext(
        entry: TruthMapEntry,
        i: Int,
        sanMoves: List<String>,
        fenSequence: List<String>,
        moveTimes: Map<Int, MoveTimeData>,
        truthMap: List<TruthMapEntry>,
    ): HighlightRuleContext {
        val san       = sanMoves.getOrElse(i) { "" }
        val fenBefore = fenSequence.getOrElse(entry.moveIndex - 1) { START_FEN }
        val fenAfter  = fenSequence.getOrElse(entry.moveIndex) { START_FEN }

        val prevEvalCp = if (i > 0) truthMap[i - 1].evalCp else 0
        val evalBefore = prevEvalCp / CP_TO_PAWNS
        val evalAfter  = entry.evalCp / CP_TO_PAWNS
        val playerDelta = entry.playerEvalDelta / CP_TO_PAWNS

        val phase = detectPhase(entry.moveIndex, fenAfter)

        val time = moveTimes[entry.moveIndex]

        return HighlightRuleContext(
            moveIndex             = entry.moveIndex,
            moveNumber            = (entry.moveIndex + 1) / 2,
            isWhiteMove           = entry.isWhiteMove,
            moveSan               = san,
            fenBefore             = fenBefore,
            fenAfter              = fenAfter,
            evalBefore            = evalBefore,
            evalAfter             = evalAfter,
            playerDelta           = playerDelta,
            phase                 = phase,
            isCapture             = 'x' in san,
            isCheck               = san.endsWith('+') || san.endsWith('#'),
            isPawnMove            = san.isNotEmpty() && san[0].isLowerCase() && !san.startsWith("O"),
            timeSpentSeconds      = time?.timeSpentSeconds,
            clockRemainingSeconds = time?.clockRemainingSeconds,
        )
    }

    private fun detectPhase(moveIndex: Int, fenAfter: String): GamePhase {
        if (moveIndex <= 20) return GamePhase.OPENING
        // Count non-pawn, non-king pieces
        val majorMinorCount = fenAfter.substringBefore(' ')
            .count { it in "nNbBrRqQ" }
        return if (majorMinorCount <= 6) GamePhase.ENDGAME else GamePhase.MIDDLEGAME
    }
}
