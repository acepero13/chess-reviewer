package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.engine.highlights.BoardAttackHelper
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side

internal object NarrativeContextBuilder {

    fun build(
        evalCp: Int,
        moverLoss: Int,
        motif: String,
        recentDeltas: List<Int>,
        recentCps: List<Int>,
        pressure: Float,
        playerIsWhite: Boolean,
        isWhiteMove: Boolean,
        weakTriggerTypes: Set<String>,
    ): GameNarrativeContext {
        val volatility = if (recentDeltas.isEmpty()) 0f
                         else recentDeltas.map { kotlin.math.abs(it) }.average().toFloat()
        val slopeWhite = if (recentCps.size >= 2)
            (recentCps.last() - recentCps.first()).toFloat() / (recentCps.size - 1)
        else 0f
        val complexityGap = when {
            motif != "mixed"                                            -> maxOf(moverLoss, FORCED_MOVE_GAP_CP)
            moverLoss >= CoachingThresholds.CANDIDATE_SEARCH_CLARITY_CP -> moverLoss
            else                                                        -> minOf(moverLoss, STRATEGIC_GAP_MAX_CP)
        }
        return GameNarrativeContext(
            volatility       = volatility,
            pressure         = pressure,
            playerEvalSlope  = if (playerIsWhite) slopeWhite else -slopeWhite,
            complexityGap    = complexityGap,
            playerEvalCp     = if (playerIsWhite) evalCp else -evalCp,
            weakTriggerTypes = weakTriggerTypes,
        )
    }

    fun computePressureScore(board: Board): Float {
        val pieces = BoardAttackHelper.allPieces(board)
        if (pieces.isEmpty()) return 0f
        val totalAttackers = pieces.sumOf { (sq, piece) ->
            val attackerSide = if (piece.pieceSide == Side.WHITE) Side.BLACK else Side.WHITE
            BoardAttackHelper.attackersOf(board, sq, attackerSide).size
        }
        return totalAttackers.toFloat() / pieces.size
    }

    fun effectiveSubPriority(trigger: CoachingTrigger, weakTypes: Set<String>): Int =
        if (trigger.typeName() in weakTypes) trigger.subPriority() - 5 else trigger.subPriority()
}
