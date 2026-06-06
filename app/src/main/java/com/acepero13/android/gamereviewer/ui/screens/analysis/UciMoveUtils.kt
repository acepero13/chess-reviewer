package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move

internal object UciMoveUtils {

    fun uciToMove(board: Board, uci: String): Move? {
        if (uci.length < 4) return null
        return runCatching {
            val from = Square.valueOf(uci.substring(0, 2).uppercase())
            val to   = Square.valueOf(uci.substring(2, 4).uppercase())
            if (uci.length == 5) Move(from, to, promotionPiece(board, uci[4])) else Move(from, to)
        }.getOrNull()
    }

    fun uciToMoveFromFens(
        uciMoves: List<String>,
        fenSequence: List<String>,
        startFen: String,
        index: Int,
    ): Move? {
        if (index < 1 || index > uciMoves.size) return null
        val uci   = uciMoves.getOrNull(index - 1) ?: return null
        val board = Board().apply { loadFromFen(fenSequence.getOrElse(index - 1) { startFen }) }
        return uciToMove(board, uci)
    }

    private fun promotionPiece(board: Board, char: Char): Piece = when (char.lowercaseChar()) {
        'r'  -> if (board.sideToMove == Side.WHITE) Piece.WHITE_ROOK   else Piece.BLACK_ROOK
        'b'  -> if (board.sideToMove == Side.WHITE) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
        'n'  -> if (board.sideToMove == Side.WHITE) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
        else -> if (board.sideToMove == Side.WHITE) Piece.WHITE_QUEEN  else Piece.BLACK_QUEEN
    }
}
