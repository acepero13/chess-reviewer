package com.acepero13.android.gamereviewer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.chess.core.data.db.PositionAnnotationDao
import com.acepero13.chess.core.data.model.PositionAnnotation
import com.acepero13.chess.core.engine.EngineResult
import com.acepero13.chess.core.engine.StockfishEngine
import androidx.compose.ui.graphics.Color
import com.acepero13.chess.core.ui.board.Arrow
import com.acepero13.chess.core.ui.board.BoardState
import com.acepero13.chess.core.ui.board.MarkedSquare
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveList
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnalysisUiState(
    val game: ReviewGame? = null,
    val moveIndex: Int = 0,              // 0 = before first move
    val boardState: BoardState = BoardState(fen = START_FEN),
    val totalMoves: Int = 0,
    val engineResult: EngineResult? = null,
    val isAnalysing: Boolean = false,
    val evalHistory: List<Int> = emptyList(),  // cp eval after each move (from White's side)
)

private const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

class AnalysisViewModel(
    private val gameId: Long,
    private val repo: GameRepository,
    private val annotationDao: PositionAnnotationDao,
    private val engine: StockfishEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    /** The parsed UCI move list for the current game, built once on load. */
    private var uciMoves: List<String> = emptyList()
    private val gson = Gson()

    init { loadGame() }

    // ── Public API ────────────────────────────────────────────────────────────

    fun goToMove(index: Int) {
        val clamped = index.coerceIn(0, uciMoves.size)
        applyMoveSequence(clamped)
        analyseCurrentPosition()
    }

    fun stepForward()  { goToMove(_uiState.value.moveIndex + 1) }
    fun stepBackward() { goToMove(_uiState.value.moveIndex - 1) }
    fun goToStart()    { goToMove(0) }
    fun goToEnd()      { goToMove(uciMoves.size) }

    fun onArrowDrawn(from: Square, to: Square) {
        val current = _uiState.value.boardState
        val arrow = Arrow(from, to, Color(0xCCF0A500.toInt()))   // amber with some transparency
        val updated = if (current.userArrows.any { it.from == from && it.to == to }) {
            current.copy(userArrows = current.userArrows.filter { !(it.from == from && it.to == to) })
        } else {
            current.copy(userArrows = current.userArrows + arrow)
        }
        _uiState.update { it.copy(boardState = updated) }
        persistAnnotation(updated)
    }

    fun onSquareMarked(square: Square) {
        val current = _uiState.value.boardState
        val updated = if (current.markedSquares.any { it.square == square }) {
            current.copy(markedSquares = current.markedSquares.filter { it.square != square })
        } else {
            current.copy(markedSquares = current.markedSquares + MarkedSquare(square, Color(0x88F0A500.toInt())))
        }
        _uiState.update { it.copy(boardState = updated) }
        persistAnnotation(updated)
    }

    fun updateMoveComment(comment: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val fen = _uiState.value.boardState.fen
            val existing = annotationDao.getByFen(fen)
            val updated = (existing ?: PositionAnnotation(fen = fen)).copy(moveComment = comment)
            annotationDao.upsert(updated)
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun loadGame() {
        viewModelScope.launch(Dispatchers.IO) {
            val game = repo.findById(gameId) ?: return@launch
            uciMoves = game.movesUci.split(' ').filter { it.isNotBlank() }
            _uiState.update {
                it.copy(
                    game       = game,
                    totalMoves = uciMoves.size,
                    boardState = it.boardState.copy(fen = START_FEN),
                )
            }
            applyMoveSequence(0)
            analyseCurrentPosition()
        }
    }

    private fun applyMoveSequence(targetIndex: Int) {
        val board = Board()
        board.loadFromFen(START_FEN)
        var lastMove: Move? = null
        for (i in 0 until targetIndex) {
            val uci = uciMoves.getOrNull(i) ?: break
            val move = uciToMove(board, uci) ?: break
            board.doMove(move)
            lastMove = move
        }
        // Load annotation for this position
        viewModelScope.launch(Dispatchers.IO) {
            val annotation = annotationDao.getByFen(board.fen)
            val arrows = annotation?.arrowsJson?.let { parseArrows(it) } ?: emptyList()
            val marks  = annotation?.markedSquaresJson?.let { parseMarks(it) } ?: emptyList()
            _uiState.update {
                it.copy(
                    moveIndex  = targetIndex,
                    boardState = it.boardState.copy(
                        fen           = board.fen,
                        lastMove      = lastMove,
                        selectedSquare = null,
                        legalMoves     = emptyList(),
                        userArrows     = arrows,
                        markedSquares  = marks,
                        isEditorMode   = true,
                    ),
                )
            }
        }
    }

    private fun analyseCurrentPosition() {
        val fen = _uiState.value.boardState.fen
        _uiState.update { it.copy(isAnalysing = true, engineResult = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { engine.analyzePosition(fen, depth = 18) }.getOrNull()
            _uiState.update { it.copy(isAnalysing = false, engineResult = result) }
        }
    }

    private fun persistAnnotation(boardState: BoardState) {
        viewModelScope.launch(Dispatchers.IO) {
            val fen = boardState.fen
            val existing = annotationDao.getByFen(fen)
            val annotation = (existing ?: PositionAnnotation(fen = fen)).copy(
                arrowsJson        = gson.toJson(boardState.userArrows),
                markedSquaresJson = gson.toJson(boardState.markedSquares),
            )
            annotationDao.upsert(annotation)
        }
    }

    private fun parseArrows(json: String): List<Arrow> = runCatching {
        gson.fromJson<List<Arrow>>(json, object : TypeToken<List<Arrow>>() {}.type)
    }.getOrDefault(emptyList())

    private fun parseMarks(json: String): List<MarkedSquare> = runCatching {
        gson.fromJson<List<MarkedSquare>>(json, object : TypeToken<List<MarkedSquare>>() {}.type)
    }.getOrDefault(emptyList())

    private fun uciToMove(board: Board, uci: String): Move? {
        if (uci.length < 4) return null
        return runCatching {
            val from = Square.valueOf(uci.substring(0, 2).uppercase())
            val to   = Square.valueOf(uci.substring(2, 4).uppercase())
            if (uci.length == 5) {
                val side = board.sideToMove
                val prom = when (uci[4].lowercaseChar()) {
                    'r' -> if (side == com.github.bhlangonijr.chesslib.Side.WHITE) Piece.WHITE_ROOK   else Piece.BLACK_ROOK
                    'b' -> if (side == com.github.bhlangonijr.chesslib.Side.WHITE) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
                    'n' -> if (side == com.github.bhlangonijr.chesslib.Side.WHITE) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
                    else -> if (side == com.github.bhlangonijr.chesslib.Side.WHITE) Piece.WHITE_QUEEN  else Piece.BLACK_QUEEN
                }
                Move(from, to, prom)
            } else Move(from, to)
        }.getOrNull()
    }
}
