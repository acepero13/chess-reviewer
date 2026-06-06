package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.ui.screens.OrphanSnippetUiState
import com.acepero13.chess.core.data.db.PositionAnnotationDao
import com.acepero13.chess.core.ui.board.BoardState
import com.acepero13.chess.core.util.ChessUtils
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class OrphanBoardController(
    private val state: MutableStateFlow<OrphanSnippetUiState>,
    private val annotationDao: PositionAnnotationDao,
    private val gson: Gson,
    private val scope: CoroutineScope,
) {
    private var tree: SnippetMoveTree? = null

    var onFenChanged: (() -> Unit)? = null

    fun initTree(startFen: String) {
        tree = SnippetMoveTree(startFen)
    }

    fun onSquareTap(square: Square) {
        val cur = state.value.boardState
        val selected = cur.selectedSquare
        if (selected == null) selectPiece(cur, square) else attemptMove(cur, selected, square)
    }

    private fun selectPiece(cur: BoardState, square: Square) {
        val board = Board().apply { loadFromFen(cur.fen) }
        val piece = board.getPiece(square)
        if (piece != Piece.NONE && piece.pieceSide == board.sideToMove) {
            state.update { it.copy(boardState = cur.copy(
                selectedSquare = square,
                legalMoves     = board.legalMoves().filter { it.from == square },
            )) }
        } else {
            state.update { it.copy(boardState = cur.copy(selectedSquare = null, legalMoves = emptyList())) }
        }
    }

    private fun attemptMove(cur: BoardState, selected: Square, target: Square) {
        val t = tree ?: return
        val board = Board().apply { loadFromFen(cur.fen) }
        val move  = ChessUtils.buildMove(board, selected, target, solutionUci = null)
        if (!board.legalMoves().contains(move)) { selectPiece(cur, target); return }
        val san = ChessUtils.uciToSan(board, ChessUtils.ghostMoveUci(move))
        board.doMove(move)
        val postFen = board.fen
        t.addMove(san, postFen)
        applyFen(postFen, canGoBack = true, rebuildTree = true)
        onFenChanged?.invoke()
        loadAnnotationsFor(postFen)
    }

    fun navigateTo(nodeId: Long) {
        val t = tree ?: return
        val fen = t.navigateTo(nodeId) ?: return
        applyFen(fen, canGoBack = t.canGoBack(), rebuildTree = true)
        onFenChanged?.invoke()
        loadAnnotationsFor(fen)
    }

    fun stepBack() {
        val t = tree ?: return
        val fen = t.stepBack() ?: return
        applyFen(fen, canGoBack = t.canGoBack(), rebuildTree = true)
        onFenChanged?.invoke()
        loadAnnotationsFor(fen)
    }

    fun resetToStart(startFen: String) {
        val t = tree ?: return
        if (startFen.isBlank()) return
        t.resetToRoot()
        applyFen(startFen, canGoBack = false, rebuildTree = true)
        onFenChanged?.invoke()
        loadAnnotationsFor(startFen)
    }

    private fun applyFen(fen: String, canGoBack: Boolean, rebuildTree: Boolean) {
        state.update { s -> s.copy(
            boardState = s.boardState.copy(
                fen            = fen,
                selectedSquare = null,
                legalMoves     = emptyList(),
                userArrows     = emptyList(),
                markedSquares  = emptyList(),
                arrows         = emptyList(),
            ),
            currentComment = "",
            canGoBack      = canGoBack,
            engineThinking = false,
            treeItems      = if (rebuildTree) tree?.buildTreeItems() ?: s.treeItems else s.treeItems,
        ) }
    }

    private fun loadAnnotationsFor(fen: String) {
        scope.launch(Dispatchers.IO) {
            val annot  = annotationDao.getByFen(fen)
            val arrows = annot?.arrowsJson?.let { AnnotationParser.parseArrows(gson, it) } ?: emptyList()
            val marks  = annot?.markedSquaresJson?.let { AnnotationParser.parseMarks(gson, it) } ?: emptyList()
            state.update { s ->
                if (s.boardState.fen == fen)
                    s.copy(
                        boardState     = s.boardState.copy(userArrows = arrows, markedSquares = marks),
                        currentComment = annot?.moveComment ?: "",
                    )
                else s
            }
        }
    }
}
