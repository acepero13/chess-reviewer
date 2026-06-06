package com.acepero13.android.gamereviewer.domain

import android.util.Log
import com.acepero13.android.gamereviewer.engine.highlights.BoardAttackHelper
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square

private const val TAG = "RookActivationDetector"

internal object RookActivationDetector {

    fun detect(board: Board, moveIndex: Int, playerIsWhite: Boolean, pfx: String = ""): CoachingTrigger.RookActivation? {
        val side      = if (playerIsWhite) Side.WHITE else Side.BLACK
        val rookPiece = if (side == Side.WHITE) Piece.WHITE_ROOK else Piece.BLACK_ROOK
        val rooks     = BoardAttackHelper.piecesOf(board, side).filter { (_, p) -> p == rookPiece }
        if (rooks.isEmpty()) { Log.d(TAG, "$pfx RookActivation: SUPPRESS no rooks"); return null }

        for ((rookSq, _) in rooks) {
            val rookFile = BoardAttackHelper.fileOf(rookSq)
            val rookRank = BoardAttackHelper.rankOf(rookSq)
            val homeRank = if (side == Side.WHITE) 0 else 7
            if (rookRank == homeRank && (rookFile == 0 || rookFile == 7)) {
                val developed = countDevelopedMinors(board, side)
                if (developed < CoachingThresholds.ROOK_ACTIVATION_MIN_DEVELOPED_MINORS) {
                    Log.d(TAG, "$pfx RookActivation: SUPPRESS $rookSq on start, developedMinors=$developed"); continue
                }
            }
            val rookFileHasPawn = (0..7).any { rank ->
                val sq = BoardAttackHelper.squareAt(rookFile, rank) ?: return@any false
                val p  = board.getPiece(sq)
                p != Piece.NONE && p.pieceType == PieceType.PAWN
            }
            if (!rookFileHasPawn) { Log.d(TAG, "$pfx RookActivation: SUPPRESS $rookSq already on open file"); continue }
            if (BoardAttackHelper.attacksFrom(board, rookSq).size >= 6) {
                Log.d(TAG, "$pfx RookActivation: SUPPRESS $rookSq mobility >= 6"); continue
            }
            val betterFile = (0..7).firstOrNull { file ->
                if (file == rookFile) return@firstOrNull false
                (0..7).none { rank ->
                    val sq = BoardAttackHelper.squareAt(file, rank) ?: return@none false
                    val p  = board.getPiece(sq)
                    p != Piece.NONE && p.pieceType == PieceType.PAWN && p.pieceSide == side
                }
            }
            if (betterFile != null) return CoachingTrigger.RookActivation(moveIndex, rookSq.name, betterFile)
            Log.d(TAG, "$pfx RookActivation: SUPPRESS $rookSq no better open file")
        }
        return null
    }

    private fun countDevelopedMinors(board: Board, side: Side): Int {
        val homeSquares = if (side == Side.WHITE)
            listOf(Square.B1 to Piece.WHITE_KNIGHT, Square.G1 to Piece.WHITE_KNIGHT,
                   Square.C1 to Piece.WHITE_BISHOP, Square.F1 to Piece.WHITE_BISHOP)
        else
            listOf(Square.B8 to Piece.BLACK_KNIGHT, Square.G8 to Piece.BLACK_KNIGHT,
                   Square.C8 to Piece.BLACK_BISHOP, Square.F8 to Piece.BLACK_BISHOP)
        return homeSquares.count { (sq, expected) -> board.getPiece(sq) != expected }
    }
}
