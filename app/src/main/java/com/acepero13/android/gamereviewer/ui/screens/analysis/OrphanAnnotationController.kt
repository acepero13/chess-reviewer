package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.ui.screens.OrphanSnippetUiState
import com.acepero13.chess.core.data.db.PositionAnnotationDao
import com.acepero13.chess.core.data.model.PositionAnnotation
import com.acepero13.chess.core.ui.board.Arrow
import com.acepero13.chess.core.ui.board.BoardState
import com.acepero13.chess.core.ui.board.MarkedSquare
import com.github.bhlangonijr.chesslib.Square
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class OrphanAnnotationController(
    private val state: MutableStateFlow<OrphanSnippetUiState>,
    private val annotationDao: PositionAnnotationDao,
    private val gson: Gson,
    private val scope: CoroutineScope,
) {

    fun onArrowDrawn(from: Square, to: Square) {
        val cur   = state.value.boardState
        val color = state.value.currentArrowColor
        val upd   = if (cur.userArrows.any { it.from == from && it.to == to })
            cur.copy(userArrows = cur.userArrows.filter { !(it.from == from && it.to == to) })
        else
            cur.copy(userArrows = cur.userArrows + Arrow(from, to, color))
        state.update { it.copy(boardState = upd) }
        persist(upd)
    }

    fun onSquareMarked(square: Square) {
        val cur   = state.value.boardState
        val color = state.value.currentArrowColor.copy(alpha = 0x88 / 255f)
        val upd   = if (cur.markedSquares.any { it.square == square })
            cur.copy(markedSquares = cur.markedSquares.filter { it.square != square })
        else
            cur.copy(markedSquares = cur.markedSquares + MarkedSquare(square, color))
        state.update { it.copy(boardState = upd) }
        persist(upd)
    }

    fun undoLastArrow() {
        val cur = state.value.boardState
        if (cur.userArrows.isEmpty()) return
        val upd = cur.copy(userArrows = cur.userArrows.dropLast(1))
        state.update { it.copy(boardState = upd) }
        persist(upd)
    }

    fun clearAnnotations() {
        val cur = state.value.boardState
        if (cur.userArrows.isEmpty() && cur.markedSquares.isEmpty()) return
        val upd = cur.copy(userArrows = emptyList(), markedSquares = emptyList())
        state.update { it.copy(boardState = upd) }
        persist(upd)
    }

    fun updateComment(comment: String) {
        state.update { it.copy(currentComment = comment) }
        scope.launch(Dispatchers.IO) {
            val fen      = state.value.boardState.fen
            val existing = annotationDao.getByFen(fen)
            annotationDao.upsert((existing ?: PositionAnnotation(fen = fen)).copy(moveComment = comment))
        }
    }

    private fun persist(boardState: BoardState) {
        scope.launch(Dispatchers.IO) {
            val fen      = boardState.fen
            val existing = annotationDao.getByFen(fen)
            annotationDao.upsert(
                (existing ?: PositionAnnotation(fen = fen)).copy(
                    arrowsJson        = gson.toJson(boardState.userArrows),
                    markedSquaresJson = gson.toJson(boardState.markedSquares),
                )
            )
        }
    }
}
