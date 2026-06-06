package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.engine.highlights.BoardAttackHelper
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.move.MoveGenerator

internal class BoardScanDetector(private val session: GameSession) {

    fun detectAllHangingSquares(): Pair<List<String>, Set<String>> {
        val fen = session.uiState.value.boardState.fen
        if (fen.isBlank()) return Pair(emptyList(), emptySet())
        val board = runCatching { Board().apply { loadFromFen(fen) } }.getOrNull()
            ?: return Pair(emptyList(), emptySet())
        val userSide = if (session.boardFlippedForBlack) Side.BLACK else Side.WHITE
        val hanging = BoardAttackHelper.allPieces(board)
            .filter { (_, piece) -> piece.pieceType != PieceType.KING }
            .filter { (sq, piece) -> BoardAttackHelper.isGenuinelyHanging(board, sq, piece) }
        val all = hanging.map { (sq, _) -> sq.name }
        val own = hanging.filter { (_, p) -> p.pieceSide == userSide }.map { (sq, _) -> sq.name }.toSet()
        return Pair(all, own)
    }

    fun detectOpponentCctSquares(): List<String> {
        val fen = session.uiState.value.boardState.fen
        if (fen.isBlank()) return emptyList()
        val board = runCatching { Board().apply { loadFromFen(fen) } }.getOrNull() ?: return emptyList()
        val sideToMove = board.sideToMove
        return MoveGenerator.generateLegalMoves(board).mapNotNull { move ->
            val targetPiece = board.getPiece(move.to)
            val isCapture = targetPiece != Piece.NONE && targetPiece.pieceSide != sideToMove
            val givesCheck = runCatching {
                Board().apply { loadFromFen(fen) }.also { it.doMove(move) }.isKingAttacked
            }.getOrDefault(false)
            when {
                givesCheck -> move.to.name
                isCapture && isMateriallyFavorable(board, move, fen) -> move.to.name
                else -> null
            }
        }.distinct()
    }

    private fun isMateriallyFavorable(board: Board, move: com.github.bhlangonijr.chesslib.move.Move, fen: String): Boolean {
        val capturingValue = pieceValue(board.getPiece(move.from))
        val capturedValue = pieceValue(board.getPiece(move.to))
        if (capturedValue >= capturingValue) return true
        val afterCapture = runCatching { Board().apply { loadFromFen(fen) }.also { it.doMove(move) } }.getOrNull() ?: return true
        return !MoveGenerator.generateLegalMoves(afterCapture).any { it.to == move.to }
    }

    private fun pieceValue(piece: Piece): Int = when (piece.pieceType) {
        PieceType.PAWN -> 100; PieceType.KNIGHT -> 320; PieceType.BISHOP -> 330
        PieceType.ROOK -> 500; PieceType.QUEEN -> 900; PieceType.KING -> 20000
        else -> 0
    }
}
