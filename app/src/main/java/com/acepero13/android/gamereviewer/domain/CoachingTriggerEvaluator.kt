package com.acepero13.android.gamereviewer.domain

import android.util.Log
import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.github.bhlangonijr.chesslib.Board

private const val TAG = "CoachTriggerEval"

object CoachingTriggerEvaluator {

    fun evaluate(
        evaluations: List<GameEvaluation>,
        fenByMoveIndex: (Int) -> String,
        timeByMoveIndex: (Int) -> Int? = { null },
        playerIsWhite: Boolean = true,
        gameId: Long = 0L,
        weakTriggerTypes: Set<String> = emptySet(),
    ): Map<Int, List<CoachingTrigger>> {
        val result       = mutableMapOf<Int, MutableList<CoachingTrigger>>()
        val sorted       = evaluations.sortedBy { it.moveIndex }
        val worstPiece   = WorstPieceDetector()
        val coordination = CoordinationDetector()
        val calibration  = CalibrationDetector()
        val recentDeltas = ArrayDeque<Int>(10)
        val recentCps    = ArrayDeque<Int>(6)

        sorted.forEachIndexed { i, eval ->
            val prevEval  = if (i > 0) sorted[i - 1] else null
            val isWhite   = eval.moveIndex % 2 == 1
            val moverLoss = if (isWhite) -eval.evalDelta else eval.evalDelta
            val pfx = "[game=$gameId move=${eval.moveIndex} ${if (isWhite) "W" else "B"} evalCp=${eval.evalCp} delta=${eval.evalDelta}]"
            Log.d(TAG, "$pfx --- evaluating triggers ---")

            updateWindows(recentDeltas, recentCps, eval)
            val board    = loadBoard(fenByMoveIndex(eval.moveIndex))
            val pressure = if (board != null) NarrativeContextBuilder.computePressureScore(board) else 0f
            val ctx      = NarrativeContextBuilder.build(eval.evalCp, moverLoss, eval.motif, recentDeltas.toList(), recentCps.toList(), pressure, playerIsWhite, isWhite, weakTriggerTypes)
            Log.d(TAG, "$pfx Narrative: volatile=${ctx.isVolatile} highPressure=${ctx.isHighPressure} cruising=${ctx.isCruising} stumbling=${ctx.isStumbling}")

            val triggers = collectTriggers(eval, prevEval, board, isWhite, playerIsWhite, moverLoss, ctx, timeByMoveIndex, worstPiece, coordination, calibration, i, sorted, fenByMoveIndex, pfx)

            TriggerSuppressor.apply(triggers, board, eval, isWhite, playerIsWhite, moverLoss, pfx)
            TriggerPrioritizer.apply(triggers, weakTriggerTypes, pfx)

            if (triggers.isNotEmpty()) {
                result[eval.moveIndex] = triggers
                if (triggers.any { it is CoachingTrigger.EvalCalibration }) calibration.onCalibrationFired(eval.moveIndex)
            }
            Log.d(TAG, "$pfx RESULT final=${triggers.map { it.typeName() }}")
        }
        Log.d(TAG, "evaluate: ${sorted.size} evals → ${result.size} trigger positions | types: ${result.values.flatten().groupBy { it.typeName() }.mapValues { it.value.size }}")
        return result
    }

    fun parseTriggers(encoded: String, moveIndex: Int): List<CoachingTrigger> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split(",").mapNotNull { CoachingTrigger.fromTypeName(it.trim(), moveIndex) }
    }

    private fun loadBoard(fen: String): Board? =
        if (fen.isBlank()) null else runCatching { Board().apply { loadFromFen(fen) } }.getOrNull()

    private fun updateWindows(deltas: ArrayDeque<Int>, cps: ArrayDeque<Int>, eval: GameEvaluation) {
        if (deltas.size >= 10) deltas.removeFirst()
        deltas.addLast(eval.evalDelta)
        if (cps.size >= 6) cps.removeFirst()
        cps.addLast(eval.evalCp)
    }

    private fun collectTriggers(
        eval: GameEvaluation, prevEval: GameEvaluation?, board: Board?,
        isWhite: Boolean, playerIsWhite: Boolean, moverLoss: Int, ctx: GameNarrativeContext,
        timeByMoveIndex: (Int) -> Int?, worstPiece: WorstPieceDetector,
        coordination: CoordinationDetector, calibration: CalibrationDetector,
        i: Int, sorted: List<GameEvaluation>, fenByMoveIndex: (Int) -> String, pfx: String,
    ): MutableList<CoachingTrigger> {
        val t   = mutableListOf<CoachingTrigger>()
        val th  = CoachingThresholds
        val mid = th.MIDDLEGAME_START..th.MIDDLEGAME_END
        val t4  = th.TIER4_OPENING_GATE..th.MIDDLEGAME_END

        if (board != null && eval.moveIndex in mid) {
            val prevBoard = prevEval?.let { loadBoard(fenByMoveIndex(it.moveIndex)) }
            SafetyDetector.detect(board, eval, playerIsWhite, prevBoard, pfx)?.let { t.add(it) }
        }
        if (eval.moveIndex in t4 && !ctx.isSuppressTier4 && !ctx.hasForcedBestMove && !ctx.isCruising)
            EvalPatternDetector.detectCandidate(eval, isWhite, pfx)?.let { t.add(it) }
        if (board != null && eval.moveIndex in t4 && !ctx.isSuppressTier4)
            worstPiece.detect(board, eval.moveIndex, playerIsWhite, pfx)?.let { t.add(it) }
        else if (eval.moveIndex < th.TIER4_OPENING_GATE) worstPiece.reset()
        TacticalMotifDetector.detectForcingMove(eval, isWhite, playerIsWhite, moverLoss, pfx)
            ?.takeIf { eval.moveIndex >= th.MIDDLEGAME_START || moverLoss >= -th.OPENING_TRIGGER_THRESHOLD_CP }
            ?.let { t.add(it) }
        if (prevEval != null && isWhite != playerIsWhite)
            TacticalMotifDetector.detectOpponentPlan(eval, isWhite, pfx)?.let { t.add(it) }
        if (board != null) TacticalMotifDetector.detectPreMoveChecklist(board, eval, moverLoss, pfx)
            ?.takeIf { eval.moveIndex >= th.MIDDLEGAME_START || moverLoss >= -th.OPENING_TRIGGER_THRESHOLD_CP }
            ?.let { t.add(it) }
        if (board != null && eval.moveIndex in th.ROOK_ACTIVATION_MIN_HALF_MOVE..th.MIDDLEGAME_END && !ctx.isHighPressure)
            RookActivationDetector.detect(board, eval.moveIndex, playerIsWhite, pfx)?.let { t.add(it) }
        if (isWhite == playerIsWhite) {
            TimePatternDetector.detectAll(eval, isWhite, timeByMoveIndex, pfx).forEach { t.add(it) }
            val prev3 = (0 until minOf(3, i)).map { k -> sorted[i - 1 - k] }
            calibration.detect(eval, prev3, pfx)?.let { t.add(it) }
        }
        if (isWhite != playerIsWhite && eval.moveIndex >= th.MIDDLEGAME_START)
            TacticalMotifDetector.detectPunishBlunder(eval, moverLoss, pfx)?.let { t.add(it) }
        if (eval.moveIndex in t4 && !ctx.isSuppressTier4 && !ctx.hasForcedBestMove && !ctx.isCruising)
            EvalPatternDetector.detectCandidateSearch(eval, isWhite, playerIsWhite, moverLoss, pfx)?.let { t.add(it) }
        if (eval.moveIndex >= th.MIDDLEGAME_START)
            EvalPatternDetector.detectCctCheck(eval, isWhite, playerIsWhite, moverLoss, pfx)?.let { t.add(it) }
        if (board != null && eval.moveIndex >= th.MIDDLEGAME_START)
            t.addAll(coordination.detect(board, eval, playerIsWhite, moverLoss, pfx))
        return t
    }
}
