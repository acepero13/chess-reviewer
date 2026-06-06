package com.acepero13.android.gamereviewer.domain

import android.util.Log
import com.acepero13.android.gamereviewer.engine.highlights.BoardAttackHelper
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side

private const val TAG = "WorstPieceDetector"

internal class WorstPieceDetector {

    private val streakTracker = mutableMapOf<String, Int>()

    fun reset() = streakTracker.clear()

    fun detect(board: Board, moveIndex: Int, playerIsWhite: Boolean, pfx: String = ""): CoachingTrigger.WorstPiece? {
        val side         = if (playerIsWhite) Side.WHITE else Side.BLACK
        val opponentSide = if (playerIsWhite) Side.BLACK else Side.WHITE
        val candidateTypes = setOf(PieceType.KNIGHT, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN)

        val restricted = BoardAttackHelper.piecesOf(board, side)
            .filter { (_, piece) -> piece.pieceType in candidateTypes }
            .map    { (sq, _)    -> sq to BoardAttackHelper.attacksFrom(board, sq).size }
            .filter { (_, mob)   -> mob <= CoachingThresholds.WORST_PIECE_MAX_MOBILITY }

        if (restricted.isEmpty()) {
            Log.d(TAG, "$pfx WorstPiece: SUPPRESS no restricted piece")
            streakTracker.clear(); return null
        }
        val (worstSq, mobility) = restricted.minByOrNull { (_, mob) -> mob } ?: return null

        if (BoardAttackHelper.attackersOf(board, worstSq, opponentSide).isNotEmpty()) {
            Log.d(TAG, "$pfx WorstPiece: SUPPRESS $worstSq under attack — deferring to PreMoveChecklist")
            streakTracker.clear(); return null
        }

        val key       = worstSq.name
        val prevStreak = streakTracker[key] ?: 0
        streakTracker.clear()
        val newStreak  = prevStreak + 1
        streakTracker[key] = newStreak

        return if (newStreak >= CoachingThresholds.WORST_PIECE_STREAK_NEEDED)
            CoachingTrigger.WorstPiece(moveIndex, key, mobility)
        else {
            Log.d(TAG, "$pfx WorstPiece: SUPPRESS streak=$newStreak/${CoachingThresholds.WORST_PIECE_STREAK_NEEDED} sq=$key")
            null
        }
    }
}
