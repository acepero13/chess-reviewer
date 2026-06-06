package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.repository.SnippetRepository
import com.acepero13.android.gamereviewer.ui.screens.analysis.AnnotationParser
import com.acepero13.android.gamereviewer.ui.screens.analysis.OrphanAnnotationController
import com.acepero13.android.gamereviewer.ui.screens.analysis.OrphanBoardController
import com.acepero13.android.gamereviewer.ui.screens.analysis.OrphanEngineController
import com.acepero13.chess.core.data.db.PositionAnnotationDao
import com.acepero13.chess.core.engine.StockfishEngine
import com.acepero13.chess.core.ui.board.BoardState
import com.github.bhlangonijr.chesslib.Square
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OrphanSnippetViewModel(
    private val snippetId: Long,
    private val snippetRepo: SnippetRepository,
    private val annotationDao: PositionAnnotationDao,
    private val engine: StockfishEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(OrphanSnippetUiState())
    val uiState: StateFlow<OrphanSnippetUiState> = _state.asStateFlow()

    private val gson       = Gson()
    private val boardCtrl  = OrphanBoardController(_state, annotationDao, gson, viewModelScope)
    private val annotCtrl  = OrphanAnnotationController(_state, annotationDao, gson, viewModelScope)
    private val engineCtrl = OrphanEngineController(_state, engine, viewModelScope)

    init {
        boardCtrl.onFenChanged = {
            if (_state.value.engineVisible) engineCtrl.fetchBestMove()
            if (_state.value.evalBarVisible) engineCtrl.fetchEval()
        }
        viewModelScope.launch(Dispatchers.IO) {
            val snippet = snippetRepo.findById(snippetId) ?: return@launch
            val fen     = snippet.fen
            val annot   = annotationDao.getByFen(fen)
            val arrows  = annot?.arrowsJson?.let { AnnotationParser.parseArrows(gson, it) } ?: emptyList()
            val marks   = annot?.markedSquaresJson?.let { AnnotationParser.parseMarks(gson, it) } ?: emptyList()
            boardCtrl.initTree(fen)
            _state.update { it.copy(
                boardState     = BoardState(fen = fen, userArrows = arrows, markedSquares = marks),
                currentComment = annot?.moveComment ?: "",
                startFen       = fen,
            ) }
        }
    }

    fun toggleEditMode() {
        val isEdit = !_state.value.isEditMode
        _state.update { it.copy(
            isEditMode = isEdit,
            boardState = it.boardState.copy(
                isEditorMode   = isEdit,
                selectedSquare = null,
                legalMoves     = emptyList(),
            ),
        ) }
    }

    fun toggleEngine() {
        val now = !_state.value.engineVisible
        _state.update { it.copy(
            engineVisible = now,
            boardState    = it.boardState.copy(arrows = emptyList()),
        ) }
        if (now) engineCtrl.fetchBestMove()
    }

    fun toggleEvalBar() {
        val now = !_state.value.evalBarVisible
        _state.update { it.copy(evalBarVisible = now) }
        if (now) engineCtrl.fetchEval()
    }

    fun onArrowDrawn(from: Square, to: Square) = annotCtrl.onArrowDrawn(from, to)
    fun onSquareMarked(square: Square)          = annotCtrl.onSquareMarked(square)
    fun updateComment(comment: String)          = annotCtrl.updateComment(comment)
    fun undoLastArrow()                         = annotCtrl.undoLastArrow()
    fun clearAnnotations()                      = annotCtrl.clearAnnotations()
    fun setArrowColor(color: Color)             = _state.update { it.copy(currentArrowColor = color) }

    fun onSquareTap(square: Square) {
        if (!_state.value.isEditMode) boardCtrl.onSquareTap(square)
    }

    fun onMoveNodeClick(nodeId: Long) = boardCtrl.navigateTo(nodeId)

    fun stepBack()     = boardCtrl.stepBack()
    fun resetToStart() = boardCtrl.resetToStart(_state.value.startFen)
}
