package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.domain.InsightReconciler
import com.acepero13.chess.core.data.model.ChessConstants
import com.acepero13.chess.core.engine.MotifClassifier
import com.acepero13.chess.core.engine.StockfishEngine
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class SandboxMoveAnalyzer(
    private val session: GameSession,
    private val engine: StockfishEngine,
    private val engineOverlay: EngineOverlayController,
) {

    suspend fun analyze(preFen: String, move: Move, postBoard: Board, postFen: String) {
        val playerWasWhite = postBoard.sideToMove == Side.BLACK
        val preEvalWhite = resolvePreEval(preFen)
        val postResult = runCatching { engine.analyzePosition(postFen, depth = 10) }.getOrNull()
        val postEvalWhite = postResult?.toWhitePerspective(postBoard)
        val cpLoss = computeCpLoss(preEvalWhite, postEvalWhite, playerWasWhite)
        val motif = postResult?.let { MotifClassifier.classify(it, postFen) } ?: "mixed"
        if (cpLoss >= ChessConstants.BLUNDER_THRESHOLD_CP) {
            handleBlunder(InsightReconciler.forBlunder(motif, cpLoss), preFen, cpLoss)
        } else {
            handleGoodMove(postFen, postEvalWhite)
        }
    }

    fun triggerEngineReply(fen: String) {
        session.uiState.update { it.copy(sandboxEngineThinking = true) }
        session.scope.launch(Dispatchers.Default) {
            delay(ChessConstants.OPPONENT_REPLY_DELAY_MS)
            val result = runCatching { engine.analyzePosition(fen, depth = 15) }.getOrNull()
            val bestUci = result?.bestMoveUci ?: run {
                session.uiState.update { it.copy(sandboxEngineThinking = false) }
                return@launch
            }
            val board = Board().apply { loadFromFen(fen) }
            val move = UciMoveUtils.uciToMove(board, bestUci) ?: run {
                session.uiState.update { it.copy(sandboxEngineThinking = false) }
                return@launch
            }
            applyEngineReply(board, move)
        }
    }

    private suspend fun applyEngineReply(board: Board, move: Move) {
        board.doMove(move)
        val replyFen = board.fen
        val replyResult = runCatching { engine.analyzePosition(replyFen, depth = 10) }.getOrNull()
        session.sandboxEvalCp = replyResult?.toWhitePerspective(board)
        session.uiState.update {
            it.copy(
                boardState = it.boardState.copy(fen = replyFen, lastMove = move, arrows = emptyList()),
                sandboxEngineThinking = false,
            )
        }
        if (session.uiState.value.bestMoveVisible) {
            session.scope.launch(Dispatchers.Default) { engineOverlay.fetchBestMove(replyFen) }
        }
    }

    private suspend fun resolvePreEval(preFen: String): Int? =
        session.sandboxEvalCp ?: runCatching { engine.analyzePosition(preFen, depth = 10) }
            .getOrNull()?.toWhitePerspective(Board().apply { loadFromFen(preFen) })

    private fun computeCpLoss(pre: Int?, post: Int?, playerWasWhite: Boolean): Int {
        if (pre == null || post == null) return 0
        return maxOf(0, if (playerWasWhite) pre - post else post - pre)
    }

    private suspend fun handleBlunder(insight: InsightReconciler.Insight, preFen: String, cpLoss: Int) {
        session.uiState.update {
            it.copy(
                sandboxEngineThinking = false, blunderGuardActive = true,
                blunderReflectionMode = true, blunderReflectionInsight = insight,
                blunderPreMoveFen = preFen, blunderCpLoss = cpLoss,
                boardState = it.boardState.copy(showingFlash = true),
            )
        }
        delay(ChessConstants.BLIND_INTER_MOVE_FLASH_MS)
        session.uiState.update { it.copy(boardState = it.boardState.copy(showingFlash = false)) }
    }

    private suspend fun handleGoodMove(postFen: String, postEvalWhite: Int?) {
        session.sandboxEvalCp = postEvalWhite
        session.uiState.update { it.copy(sandboxEngineThinking = false, blunderGuardActive = false) }
        if (session.uiState.value.bestMoveVisible) {
            session.scope.launch(Dispatchers.Default) { engineOverlay.fetchBestMove(postFen) }
        }
        triggerEngineReply(postFen)
    }
}
