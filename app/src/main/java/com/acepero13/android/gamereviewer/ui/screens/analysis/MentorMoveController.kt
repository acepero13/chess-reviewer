package com.acepero13.android.gamereviewer.ui.screens.analysis

import android.util.Log
import com.acepero13.chess.core.engine.StockfishEngine
import com.acepero13.chess.core.util.ChessUtils
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "MentorTap"

internal class MentorMoveController(
    private val session: GameSession,
    private val engine: StockfishEngine,
    private val quizController: ClassificationQuizController,
) {

    private val checker = MentorMoveChecker(session, engine, quizController)

    fun toggleMentorMoveInput() {
        val nowActive = !session.uiState.value.mentorMoveInputActive
        Log.d(TAG, "toggleMentorMoveInput: nowActive=$nowActive")
        val fen = session.fenSequence.getOrElse(session.uiState.value.moveIndex) { session.startFen }
        session.uiState.update {
            it.copy(mentorMoveInputActive = nowActive, mentorMoveChecking = false,
                mentorMoveResult = null, mentorMoveFeedback = "",
                boardState = it.boardState.copy(fen = fen, lastMove = null,
                    selectedSquare = null, legalMoves = emptyList(), isEditorMode = false))
        }
    }

    fun onMentorSquareTap(square: Square) {
        val st = session.uiState.value
        if (!st.mentorMoveInputActive || st.mentorMoveChecking) return
        val cur = st.boardState
        val selected = cur.selectedSquare
        if (selected == null) selectPiece(cur, square)
        else attemptMoveFrom(cur, selected, square)
    }

    fun retryMentorMove() {
        val fen = session.fenSequence.getOrElse(session.uiState.value.moveIndex) { session.startFen }
        session.uiState.update {
            it.copy(mentorMoveResult = null, mentorMoveFeedback = "", mentorMoveInputActive = true,
                boardState = it.boardState.copy(fen = fen, lastMove = null,
                    selectedSquare = null, legalMoves = emptyList(), isEditorMode = false))
        }
    }

    private fun selectPiece(cur: com.acepero13.chess.core.ui.board.BoardState, square: Square) {
        val board = Board().apply { loadFromFen(cur.fen) }
        val piece = board.getPiece(square)
        if (piece != Piece.NONE && piece.pieceSide == board.sideToMove) {
            session.uiState.update {
                it.copy(boardState = cur.copy(selectedSquare = square,
                    legalMoves = board.legalMoves().filter { it.from == square }))
            }
        }
    }

    private fun attemptMoveFrom(cur: com.acepero13.chess.core.ui.board.BoardState, selected: Square, square: Square) {
        val board = Board().apply { loadFromFen(cur.fen) }
        val move = ChessUtils.buildMove(board, selected, square, solutionUci = null)
        if (board.legalMoves().contains(move)) {
            launchMentorCheck(cur.fen, move)
        } else {
            session.uiState.update { it.copy(boardState = cur.copy(selectedSquare = null, legalMoves = emptyList())) }
        }
    }

    private fun launchMentorCheck(preFen: String, move: Move) {
        val postBoard = Board().apply { loadFromFen(preFen) }
        postBoard.doMove(move)
        val postFen = postBoard.fen
        session.uiState.update {
            it.copy(mentorMoveChecking = true,
                boardState = it.boardState.copy(fen = postFen, lastMove = move, selectedSquare = null, legalMoves = emptyList()))
        }
        session.scope.launch(Dispatchers.Default) { checker.check(preFen, move, postBoard, postFen) }
    }
}
