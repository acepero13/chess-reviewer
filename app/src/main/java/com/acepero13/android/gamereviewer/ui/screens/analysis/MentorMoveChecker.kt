package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.ui.screens.MentorMoveResult
import com.acepero13.chess.core.data.model.ChessConstants
import com.acepero13.chess.core.engine.StockfishEngine
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.flow.update

internal class MentorMoveChecker(
    private val session: GameSession,
    private val engine: StockfishEngine,
    private val quizController: ClassificationQuizController,
) {

    suspend fun check(preFen: String, move: Move, postBoard: Board, postFen: String) {
        val playerUci = "${move.from.name.lowercase()}${move.to.name.lowercase()}"
        val playerWasWhite = postBoard.sideToMove == Side.BLACK
        val preResult = runCatching { engine.analyzePosition(preFen, ChessConstants.DEFAULT_ANALYSIS_DEPTH) }.getOrNull()
        val bestUci = preResult?.bestMoveUci ?: ""
        val preEvalWp = preResult?.let { it.toWhitePerspective(Board().apply { loadFromFen(preFen) }) } ?: 0
        val postResult = runCatching { engine.analyzePosition(postFen, depth = 10) }.getOrNull()
        val postEvalWp = postResult?.toWhitePerspective(postBoard) ?: preEvalWp
        val cpLoss = maxOf(0, if (playerWasWhite) preEvalWp - postEvalWp else postEvalWp - preEvalWp)
        val result = when {
            playerUci == bestUci -> MentorMoveResult.CORRECT
            cpLoss <= 50 -> MentorMoveResult.CLOSE
            else -> MentorMoveResult.INCORRECT
        }
        applyResult(result, cpLoss, preFen)
        quizController.triggerClassificationQuiz()
    }

    private fun applyResult(result: MentorMoveResult, cpLoss: Int, preFen: String) {
        val feedback = when (result) {
            MentorMoveResult.CORRECT -> "That's the engine's best move! Great find."
            MentorMoveResult.CLOSE -> "Good move — only ${cpLoss}cp from the best. You found the right idea."
            MentorMoveResult.INCORRECT -> "That loses about ${cpLoss}cp. Think again or use a hint."
        }
        if (result == MentorMoveResult.INCORRECT) {
            session.uiState.update {
                it.copy(mentorMoveChecking = false, mentorMoveResult = result, mentorMoveFeedback = feedback,
                    guidedDiscoveryInsightRevealed = true,
                    boardState = it.boardState.copy(fen = preFen, lastMove = null,
                        selectedSquare = null, legalMoves = emptyList(), isEditorMode = false))
            }
        } else {
            session.uiState.update {
                it.copy(mentorMoveChecking = false, mentorMoveResult = result, mentorMoveFeedback = feedback,
                    mentorMoveInputActive = false, guidedDiscoveryInsightRevealed = true,
                    boardState = it.boardState.copy(isEditorMode = false))
            }
        }
    }
}
