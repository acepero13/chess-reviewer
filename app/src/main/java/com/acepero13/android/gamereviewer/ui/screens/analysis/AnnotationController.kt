package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.chess.core.data.db.PositionAnnotationDao
import com.acepero13.chess.core.data.model.PositionAnnotation
import com.acepero13.chess.core.ui.board.Arrow
import com.acepero13.chess.core.ui.board.BoardState
import com.acepero13.chess.core.ui.board.MarkedSquare
import com.github.bhlangonijr.chesslib.Square
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AnnotationController(
    private val session: GameSession,
    private val annotationDao: PositionAnnotationDao,
    private val treeBuilder: MoveTreeBuilder,
) {

    fun onArrowDrawn(from: Square, to: Square) {
        val cur   = session.uiState.value.boardState
        val color = session.uiState.value.currentArrowColor
        val upd   = if (cur.userArrows.any { it.from == from && it.to == to })
            cur.copy(userArrows = cur.userArrows.filter { !(it.from == from && it.to == to) })
        else
            cur.copy(userArrows = cur.userArrows + Arrow(from, to, color))
        session.uiState.update { it.copy(boardState = upd) }
        persistAnnotation(upd)
    }

    fun onSquareMarked(square: Square) {
        val cur   = session.uiState.value.boardState
        val color = session.uiState.value.currentArrowColor.copy(alpha = 0x88 / 255f)
        val upd   = if (cur.markedSquares.any { it.square == square })
            cur.copy(markedSquares = cur.markedSquares.filter { it.square != square })
        else
            cur.copy(markedSquares = cur.markedSquares + MarkedSquare(square, color))
        session.uiState.update { it.copy(boardState = upd) }
        persistAnnotation(upd)
    }

    fun undoLastArrow() {
        val cur = session.uiState.value.boardState
        if (cur.userArrows.isEmpty()) return
        val upd = cur.copy(userArrows = cur.userArrows.dropLast(1))
        session.uiState.update { it.copy(boardState = upd) }
        persistAnnotation(upd)
    }

    fun clearArrows() {
        val cur = session.uiState.value.boardState
        if (cur.userArrows.isEmpty()) return
        val upd = cur.copy(userArrows = emptyList())
        session.uiState.update { it.copy(boardState = upd) }
        persistAnnotation(upd)
    }

    fun updateMoveComment(comment: String) {
        session.uiState.update { it.copy(currentComment = comment) }
        session.scope.launch(Dispatchers.IO) {
            val fen = session.uiState.value.boardState.fen
            val upd = (session.annotationCache[fen] ?: PositionAnnotation(fen = fen))
                .copy(moveComment = comment)
            annotationDao.upsert(upd)
            session.annotationCache[fen] = upd
            treeBuilder.refreshTreeItems()
        }
    }

    fun persistAnnotation(boardState: BoardState) {
        session.scope.launch(Dispatchers.IO) {
            val fen      = boardState.fen
            val existing = session.annotationCache[fen]
            val upd      = (existing ?: PositionAnnotation(fen = fen)).copy(
                arrowsJson        = session.gson.toJson(boardState.userArrows),
                markedSquaresJson = session.gson.toJson(boardState.markedSquares),
            )
            annotationDao.upsert(upd)
            session.annotationCache[fen] = upd
            val hasAnnot = upd.moveComment.isNotBlank() || upd.arrowsJson.length > 2 || upd.markedSquaresJson.length > 2
            session.uiState.update { it.copy(hasAnnotationAtCurrent = hasAnnot) }
            treeBuilder.refreshTreeItems()
        }
    }
}
