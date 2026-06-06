package com.acepero13.android.gamereviewer.domain

import android.util.Log
import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.engine.highlights.BoardAttackHelper
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square

private const val TAG = "CoordinationDetector"

internal class CoordinationDetector {

    private var prevPlayerKingAttack   = 0
    private var prevOpponentKingAttack = 0
    private var prevPlayerHarmony      = 0
    private var prevOpponentHarmony    = 0

    fun detect(board: Board, eval: GameEvaluation, playerIsWhite: Boolean, moverLoss: Int, pfx: String): List<CoachingTrigger> {
        val playerSide   = if (playerIsWhite) Side.WHITE else Side.BLACK
        val opponentSide = if (playerIsWhite) Side.BLACK else Side.WHITE
        val newPKA = CoordinationAnalyzer.kingAttackScore(board, playerSide)
        val newOKA = CoordinationAnalyzer.kingAttackScore(board, opponentSide)
        val newPH  = CoordinationAnalyzer.generalCoordinationScore(board, playerSide)
        val newOH  = CoordinationAnalyzer.generalCoordinationScore(board, opponentSide)
        Log.d(TAG, "$pfx Coord: pKA=$newPKA(prev=$prevPlayerKingAttack) oKA=$newOKA(prev=$prevOpponentKingAttack) pH=$newPH(prev=$prevPlayerHarmony) oH=$newOH(prev=$prevOpponentHarmony)")

        val blundered    = moverLoss >= CoachingThresholds.COORDINATION_BLUNDER_SUPPRESS_CP
        val hasHanging   = BoardAttackHelper.allPieces(board)
            .filter { (_, p) -> p.pieceType != PieceType.KING }
            .any    { (sq, p) -> BoardAttackHelper.isGenuinelyHanging(board, sq, p) }
        val tacticallyClean = !hasHanging && eval.motif == "mixed"
        val minAdv   = CoachingThresholds.COORDINATION_EVAL_MIN_ADVANTAGE_CP
        val pAdv     = if (playerIsWhite) eval.evalCp else -eval.evalCp
        val oAdv     = if (playerIsWhite) -eval.evalCp else eval.evalCp
        val pBacked  = pAdv >= minAdv && !blundered && tacticallyClean
        val oBacked  = oAdv >= minAdv && !blundered && tacticallyClean

        val result = mutableListOf<CoachingTrigger>()
        result += attackTriggers(eval, playerSide, newPKA, prevPlayerKingAttack, isPlayer = true,  backed = pBacked, board, pfx)
        result += attackTriggers(eval, opponentSide, newOKA, prevOpponentKingAttack, isPlayer = false, backed = oBacked, board, pfx)
        result += harmonyTriggers(eval, playerSide, newPH, prevPlayerHarmony, isPlayer = true,  backed = pBacked, board, pfx, skip = result.any { it is CoachingTrigger.CoordinatedAttack && it.isPlayerSide })
        result += harmonyTriggers(eval, opponentSide, newOH, prevOpponentHarmony, isPlayer = false, backed = oBacked, board, pfx, skip = result.any { it is CoachingTrigger.CoordinatedAttack && !it.isPlayerSide })

        prevPlayerKingAttack = newPKA; prevOpponentKingAttack = newOKA
        prevPlayerHarmony    = newPH;  prevOpponentHarmony    = newOH
        return result
    }

    private fun attackTriggers(eval: GameEvaluation, side: Side, newScore: Int, prevScore: Int, isPlayer: Boolean, backed: Boolean, board: Board, pfx: String): List<CoachingTrigger> {
        val fire  = CoachingThresholds.KING_ATTACK_FIRE_THRESHOLD
        val lossT = CoachingThresholds.KING_ATTACK_LOSS_THRESHOLD
        val label = if (isPlayer) "player" else "opponent"
        return when {
            newScore >= fire && prevScore < fire -> {
                if (backed) {
                    val d = CoordinationAnalyzer.kingAttackDetail(board, side)
                    listOf(CoachingTrigger.CoordinatedAttack(eval.moveIndex, isPlayer, false, newScore, d.attackerSquares, d.targetSquares.firstOrNull()))
                        .also { Log.d(TAG, "$pfx CoordAttack($label gain): FIRE") }
                } else { Log.d(TAG, "$pfx CoordAttack($label gain): SUPPRESS backed=$backed"); emptyList() }
            }
            newScore <= lossT && prevScore >= fire -> {
                listOf(CoachingTrigger.CoordinatedAttack(eval.moveIndex, isPlayer, true, prevScore))
                    .also { Log.d(TAG, "$pfx CoordAttack($label loss): FIRE") }
            }
            else -> { Log.d(TAG, "$pfx CoordAttack($label): SKIP no transition"); emptyList() }
        }
    }

    private fun harmonyTriggers(eval: GameEvaluation, side: Side, newScore: Int, prevScore: Int, isPlayer: Boolean, backed: Boolean, board: Board, pfx: String, skip: Boolean): List<CoachingTrigger> {
        if (skip) return emptyList()
        val fire  = CoachingThresholds.HARMONY_FIRE_THRESHOLD
        val lossT = CoachingThresholds.HARMONY_LOSS_THRESHOLD
        val minD  = CoachingThresholds.HARMONY_MIN_DELTA
        val delta = newScore - prevScore
        val label = if (isPlayer) "player" else "opponent"
        return when {
            newScore >= fire && prevScore < fire && delta >= minD -> {
                if (backed) {
                    val d = CoordinationAnalyzer.harmonyDetail(board, side)
                    listOf(CoachingTrigger.PieceHarmony(eval.moveIndex, isPlayer, false, newScore, d.attackerSquares, d.targetSquares))
                        .also { Log.d(TAG, "$pfx PieceHarmony($label gain): FIRE") }
                } else { Log.d(TAG, "$pfx PieceHarmony($label gain): SUPPRESS backed=$backed"); emptyList() }
            }
            newScore <= lossT && prevScore >= fire && kotlin.math.abs(delta) >= minD -> {
                listOf(CoachingTrigger.PieceHarmony(eval.moveIndex, isPlayer, true, prevScore))
                    .also { Log.d(TAG, "$pfx PieceHarmony($label loss): FIRE") }
            }
            else -> { Log.d(TAG, "$pfx PieceHarmony($label): SKIP no transition"); emptyList() }
        }
    }
}
