package com.acepero13.android.gamereviewer.domain

import android.util.Log
import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.engine.highlights.BoardAttackHelper
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square

private const val TAG = "SafetyDetector"

internal object SafetyDetector {

    fun detect(
        board: Board,
        eval: GameEvaluation,
        playerIsWhite: Boolean,
        prevBoard: Board? = null,
        pfx: String = "",
    ): CoachingTrigger.Safety? {
        val movingSide   = if (playerIsWhite) Side.WHITE else Side.BLACK
        val opponentSide = if (playerIsWhite) Side.BLACK else Side.WHITE
        val kingSquare   = BoardAttackHelper.kingSquare(board, movingSide) ?: run {
            Log.d(TAG, "$pfx Safety: SUPPRESS no king for side=$movingSide")
            return null
        }
        val adjacent      = adjacentSquares(kingSquare)
        val defenderCount = adjacent.count { sq ->
            val p = board.getPiece(sq)
            p != Piece.NONE && p.pieceSide == movingSide && p.pieceType != PieceType.KING
        }
        if (defenderCount > CoachingThresholds.KING_MIN_ADJACENT_DEFENDERS) {
            Log.d(TAG, "$pfx Safety: SUPPRESS defenderCount=$defenderCount king=$kingSquare")
            return null
        }
        val attacksBySquare   = adjacent.associateWith { sq -> BoardAttackHelper.attackersOf(board, sq, opponentSide).size }
        val mostAttackedEntry = attacksBySquare.maxByOrNull { (_, cnt) -> cnt }
        val hasDirectAttack   = (mostAttackedEntry?.value ?: 0) > 0
        val moverDelta        = if (playerIsWhite) eval.evalDelta else -eval.evalDelta
        val isSignificantDrop = moverDelta <= -CoachingThresholds.SAFETY_MIN_CP_DROP
        if (!isSignificantDrop && !hasDirectAttack) {
            Log.d(TAG, "$pfx Safety: SUPPRESS no drop and no direct attack (moverDelta=$moverDelta)")
            return null
        }
        if (hasDirectAttack && !isSignificantDrop && prevBoard != null) {
            val prevKingSquare   = BoardAttackHelper.kingSquare(prevBoard, movingSide) ?: kingSquare
            val prevMaxAttackers = adjacentSquares(prevKingSquare).maxOfOrNull { sq ->
                BoardAttackHelper.attackersOf(prevBoard, sq, opponentSide).size
            } ?: 0
            val currentMax = mostAttackedEntry?.value ?: 0
            if (currentMax <= prevMaxAttackers) {
                Log.d(TAG, "$pfx Safety: SUPPRESS pre-existing attack (current=$currentMax <= prev=$prevMaxAttackers)")
                return null
            }
        }
        val threatSquare = if (hasDirectAttack) mostAttackedEntry?.key?.name else null
        return CoachingTrigger.Safety(eval.moveIndex, kingSquare.name, threatSquare)
    }

    private fun adjacentSquares(square: Square): List<Square> {
        val file = BoardAttackHelper.fileOf(square)
        val rank = BoardAttackHelper.rankOf(square)
        val result = mutableListOf<Square>()
        for (df in -1..1) for (dr in -1..1) {
            if (df == 0 && dr == 0) continue
            BoardAttackHelper.squareAt(file + df, rank + dr)?.let { result.add(it) }
        }
        return result
    }
}
