package com.acepero13.android.gamereviewer.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.db.GuessMoveSessionDao
import com.acepero13.android.gamereviewer.data.model.GuessMoveSession
import com.acepero13.android.gamereviewer.domain.extractMoveAnnotations
import com.acepero13.android.gamereviewer.domain.pgnToUciMoves
import com.acepero13.chess.core.data.db.PositionAnnotationDao
import com.acepero13.chess.core.data.model.ChessConstants
import com.acepero13.chess.core.data.model.PositionAnnotation
import com.acepero13.chess.core.engine.StockfishEngine
import com.acepero13.chess.core.pgn.ChessComFetcher
import com.acepero13.chess.core.pgn.LichessFetcher
import com.acepero13.chess.core.pgn.PgnImporter
import com.acepero13.chess.core.pgn.SolutionTreeBuilder
import com.acepero13.chess.core.ui.board.Arrow
import com.acepero13.chess.core.ui.board.BoardState
import com.acepero13.chess.core.ui.board.MarkedSquare
import com.acepero13.chess.core.util.ChessUtils
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "GuessTheMoveVM"
private const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
private const val MIN_GAME_HALF_MOVES = 20

enum class GuessingSide { BOTH, WHITE_ONLY, BLACK_ONLY }

enum class GuessTheMovePhase { CHOOSING_SIDE, SELECTING, LOADING, GUESSING, MOVE_REVEALED, GAME_COMPLETE, REVIEWING }

sealed class MasterGameSource {
    object Offline : MasterGameSource()
    data class OnlineFamousPlayer(
        val displayName: String,
        val username: String,
        val platform: String,
    ) : MasterGameSource()
    data class OnlineCustom(val username: String, val platform: String) : MasterGameSource()
}

val FAMOUS_MASTERS = listOf(
    MasterGameSource.OnlineFamousPlayer("Magnus Carlsen",     "DrNykterstein",   "lichess"),
    MasterGameSource.OnlineFamousPlayer("Hikaru Nakamura",    "Hikaru",           "lichess"),
    MasterGameSource.OnlineFamousPlayer("Fabiano Caruana",    "FabianoCaruana",   "lichess"),
    MasterGameSource.OnlineFamousPlayer("Ding Liren",         "DingLiren",        "lichess"),
    MasterGameSource.OnlineFamousPlayer("Praggnanandhaa",     "rpchess",          "lichess"),
    MasterGameSource.OnlineFamousPlayer("Alireza Firouzja",   "Alireza2003",      "lichess"),
    MasterGameSource.OnlineFamousPlayer("Ian Nepomniachtchi", "lachesisQ",        "lichess"),
    MasterGameSource.OnlineFamousPlayer("Anish Giri",         "anishgiri",        "lichess"),
    MasterGameSource.OnlineFamousPlayer("Wesley So",          "GMWesleyso",       "chesscom"),
    MasterGameSource.OnlineFamousPlayer("Levon Aronian",      "LevonAronian",     "chesscom"),
)

data class GuessTheMoveUiState(
    val phase: GuessTheMovePhase = GuessTheMovePhase.SELECTING,
    // ── Selection ────────────────────────────────────────────────────────────
    val selectedSource: MasterGameSource = MasterGameSource.Offline,
    val customUsername: String = "",
    val selectedPlatform: String = "lichess",
    val selectedSide: GuessingSide = GuessingSide.BOTH,
    val fetchError: String? = null,
    // ── Game in progress ─────────────────────────────────────────────────────
    val gameDescription: String = "",
    val sourceLabel: String = "",
    val whitePlayer: String = "",
    val blackPlayer: String = "",
    val masterMoves: List<String> = emptyList(),
    val moveAnnotations: Map<Int, String> = emptyMap(),
    val currentMoveIndex: Int = 0,
    val boardState: BoardState = BoardState(),
    val isEditorMode: Boolean = false,
    // ── Comparison (MOVE_REVEALED) ────────────────────────────────────────────
    val userMoveSan: String = "",
    val masterMoveSan: String = "",
    val wasExactMatch: Boolean = false,
    val originalAnnotation: String? = null,
    val showOriginalAnnotation: Boolean = false,
    // ── User reflection annotation ────────────────────────────────────────────
    val currentUserComment: String = "",
    val currentArrowColor: Color = Color(0xFFFFD700),
    // ── Score ─────────────────────────────────────────────────────────────────
    val exactMatches: Int = 0,
    val totalPresented: Int = 0,
    // ── Engine (on-demand only) ───────────────────────────────────────────────
    val engineThinking: Boolean = false,
    val engineArrow: Arrow? = null,
    val engineEvalCp: Int? = null,
    // ── Review mode ──────────────────────────────────────────────────────────
    val fenHistory: List<String> = emptyList(),
    val masterSanHistory: List<String> = emptyList(),
    val reviewIndex: Int = 0,
)

class GuessTheMoveViewModel(
    private val context: Context,
    private val importer: PgnImporter,
    private val dao: GuessMoveSessionDao,
    private val annotationDao: PositionAnnotationDao,
    private val engine: StockfishEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuessTheMoveUiState())
    val uiState: StateFlow<GuessTheMoveUiState> = _uiState.asStateFlow()

    private val gson = Gson()
    private val treeBuilder = SolutionTreeBuilder()

    // Tracks the current FEN for annotation saving (the master's post-move FEN)
    private var currentPostFen: String = START_FEN

    // ── Source selection ──────────────────────────────────────────────────────

    fun selectSource(source: MasterGameSource) {
        _uiState.update { it.copy(selectedSource = source, fetchError = null) }
    }

    fun updateCustomUsername(text: String) {
        _uiState.update { it.copy(customUsername = text, fetchError = null) }
    }

    fun updateSelectedPlatform(platform: String) {
        _uiState.update { it.copy(selectedPlatform = platform) }
    }

    fun updateSelectedSide(side: GuessingSide) {
        _uiState.update { it.copy(selectedSide = side) }
    }

    fun confirmSide(side: GuessingSide) {
        val st = _uiState.value
        _uiState.update { it.copy(selectedSide = side, phase = GuessTheMovePhase.GUESSING) }
        maybeAutoAdvance(0, st.masterMoves, side)
    }

    // ── Session start ─────────────────────────────────────────────────────────

    fun startSession() {
        val st = _uiState.value
        _uiState.update { it.copy(phase = GuessTheMovePhase.LOADING, fetchError = null) }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val (pgn, sourceLabel) = fetchPgn(st.selectedSource, st.customUsername, st.selectedPlatform)
                val games = importer.splitGames(pgn)
                val picked = pickGame(games) ?: error("No suitable game found (need ≥$MIN_GAME_HALF_MOVES moves)")
                startGameFromPgn(picked, sourceLabel, st.selectedSide)
            }.onFailure { e ->
                Log.e(TAG, "startSession failed", e)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            phase      = GuessTheMovePhase.SELECTING,
                            fetchError = e.message ?: "Failed to load game",
                        )
                    }
                }
            }
        }
    }

    fun startWithGameAtIndex(index: Int) {
        val side = _uiState.value.selectedSide
        _uiState.update { it.copy(phase = GuessTheMovePhase.LOADING, fetchError = null) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val pgn = context.assets.open("master_games.pgn").bufferedReader().readText()
                val games = importer.splitGames(pgn)
                val picked = games.getOrNull(index) ?: games.firstOrNull()
                    ?: error("No game found at index $index")
                startGameFromPgn(picked, "Offline", side)
            }.onFailure { e ->
                Log.e(TAG, "startWithGameAtIndex failed", e)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            phase      = GuessTheMovePhase.SELECTING,
                            fetchError = e.message ?: "Failed to load game",
                        )
                    }
                }
            }
        }
    }

    private suspend fun startGameFromPgn(gameText: String, sourceLabel: String, side: GuessingSide) {
        val parsed = importer.parseGame(gameText) ?: error("Failed to parse selected game")
        val uciMoves = pgnToUciMoves(parsed.movesPgn)
            .split(" ").filter { it.isNotBlank() }
        if (uciMoves.isEmpty()) error("Game has no parseable moves")

        val annotations = extractMoveAnnotations(parsed.movesPgn)
        val gameDesc    = buildGameDescription(parsed.headers)
        val white       = parsed.headers["White"] ?: ""
        val black       = parsed.headers["Black"] ?: ""

        withContext(Dispatchers.Main) {
            currentPostFen = START_FEN
            _uiState.update {
                it.copy(
                    phase                  = GuessTheMovePhase.CHOOSING_SIDE,
                    gameDescription        = gameDesc,
                    sourceLabel            = sourceLabel,
                    whitePlayer            = white,
                    blackPlayer            = black,
                    masterMoves            = uciMoves,
                    moveAnnotations        = annotations,
                    currentMoveIndex       = 0,
                    boardState             = BoardState(fen = START_FEN, isEditorMode = false),
                    isEditorMode           = false,
                    exactMatches           = 0,
                    totalPresented         = 0,
                    userMoveSan            = "",
                    masterMoveSan          = "",
                    wasExactMatch          = false,
                    originalAnnotation     = null,
                    showOriginalAnnotation = false,
                    currentUserComment     = "",
                    engineArrow            = null,
                    engineEvalCp           = null,
                    selectedSide           = side,
                )
            }
        }
    }

    private suspend fun fetchPgn(
        source: MasterGameSource,
        customUsername: String,
        platform: String,
    ): Pair<String, String> = when (source) {
        is MasterGameSource.Offline -> {
            val pgn = context.assets.open("master_games.pgn").bufferedReader().readText()
            pgn to "Offline"
        }
        is MasterGameSource.OnlineFamousPlayer -> {
            val pgn = if (source.platform == "lichess") {
                LichessFetcher.fetchPgn(source.username)
            } else {
                ChessComFetcher.fetchPgn(source.username)
            }
            pgn to "${source.platform.replaceFirstChar { it.uppercase() }}: ${source.displayName}"
        }
        is MasterGameSource.OnlineCustom -> {
            val pgn = if (platform == "lichess") {
                LichessFetcher.fetchPgn(customUsername)
            } else {
                ChessComFetcher.fetchPgn(customUsername)
            }
            pgn to "${platform.replaceFirstChar { it.uppercase() }}: $customUsername"
        }
    }

    private fun pickGame(games: List<String>): String? {
        val suitable = games.filter { game ->
            val uci = pgnToUciMoves(
                importer.parseGame(game)?.movesPgn ?: return@filter false
            )
            uci.split(" ").count { it.isNotBlank() } >= MIN_GAME_HALF_MOVES
        }
        return suitable.randomOrNull() ?: games.firstOrNull()
    }

    private fun buildGameDescription(headers: Map<String, String>): String {
        val white = headers["White"] ?: "?"
        val black = headers["Black"] ?: "?"
        val event = headers["Event"] ?: ""
        val date  = headers["Date"]?.substringBefore(".")  ?: ""
        return buildString {
            append("$white vs $black")
            if (event.isNotBlank()) append(" · $event")
            if (date.isNotBlank()) append(" · $date")
        }
    }

    // ── Board interaction ─────────────────────────────────────────────────────

    fun onSquareTap(square: Square) {
        val st = _uiState.value
        if (st.phase != GuessTheMovePhase.GUESSING) return

        val cur      = st.boardState
        val selected = cur.selectedSquare

        if (selected == null) {
            val board = Board().apply { loadFromFen(cur.fen) }
            val piece = board.getPiece(square)
            if (piece != Piece.NONE && piece.pieceSide == board.sideToMove) {
                val legal = board.legalMoves().filter { it.from == square }
                _uiState.update { it.copy(boardState = cur.copy(selectedSquare = square, legalMoves = legal)) }
            }
        } else {
            val board = Board().apply { loadFromFen(cur.fen) }
            val move  = ChessUtils.buildMove(board, selected, square, solutionUci = null)
            val isLegal = board.legalMoves().contains(move)
            if (isLegal) {
                submitUserMove(preFen = cur.fen, move = move)
            } else {
                _uiState.update { it.copy(boardState = cur.copy(selectedSquare = null, legalMoves = emptyList())) }
            }
        }
    }

    fun onArrowDrawn(from: Square, to: Square) {
        val st = _uiState.value
        val color = st.currentArrowColor
        val newArrow = Arrow(from, to, color)
        val updated = st.boardState.userArrows.toMutableList()
        if (!updated.removeIf { it.from == from && it.to == to }) updated.add(newArrow)
        _uiState.update { it.copy(boardState = st.boardState.copy(userArrows = updated)) }
    }

    fun onSquareMarked(square: Square) {
        val st = _uiState.value
        val color = st.currentArrowColor
        val newMark = MarkedSquare(square, color)
        val updated = st.boardState.markedSquares.toMutableList()
        if (!updated.removeIf { it.square == square }) updated.add(newMark)
        _uiState.update { it.copy(boardState = st.boardState.copy(markedSquares = updated)) }
    }

    fun updateArrowColor(color: Color) {
        _uiState.update { it.copy(currentArrowColor = color) }
    }

    fun toggleEditorMode() {
        val newMode = !_uiState.value.isEditorMode
        _uiState.update {
            it.copy(
                isEditorMode = newMode,
                boardState   = it.boardState.copy(isEditorMode = newMode),
            )
        }
    }

    fun skipMove() {
        val st = _uiState.value
        if (st.phase != GuessTheMovePhase.GUESSING) return
        val masterUci = st.masterMoves.getOrNull(st.currentMoveIndex) ?: return
        val preFen    = st.boardState.fen

        val masterPostFen = treeBuilder.applyUci(preFen, masterUci) ?: return
        val masterSan = runCatching {
            ChessUtils.uciToSan(Board().apply { loadFromFen(preFen) }, masterUci)
        }.getOrDefault(masterUci)
        val masterMove = runCatching {
            Board().apply { loadFromFen(preFen) }.legalMoves().firstOrNull { m ->
                val uci = "${m.from.name.lowercase()}${m.to.name.lowercase()}"
                uci == masterUci.take(4) || "${uci}${m.promotion.fenSymbol.lowercase()}" == masterUci
            }
        }.getOrNull()

        currentPostFen = masterPostFen
        _uiState.update {
            it.copy(
                phase                  = GuessTheMovePhase.MOVE_REVEALED,
                boardState             = it.boardState.copy(
                    fen            = masterPostFen,
                    lastMove       = masterMove,
                    selectedSquare = null,
                    legalMoves     = emptyList(),
                    userArrows     = emptyList(),
                    markedSquares  = emptyList(),
                ),
                userMoveSan            = "—",
                masterMoveSan          = masterSan,
                wasExactMatch          = false,
                originalAnnotation     = st.moveAnnotations[st.currentMoveIndex],
                showOriginalAnnotation = false,
                currentUserComment     = "",
                engineArrow            = null,
                engineEvalCp           = null,
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val saved  = runCatching { annotationDao.getByFen(masterPostFen) }.getOrNull()
            val arrows = saved?.arrowsJson?.let { parseArrows(it) } ?: emptyList()
            val marks  = saved?.markedSquaresJson?.let { parseMarks(it) } ?: emptyList()
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        currentUserComment = saved?.moveComment ?: "",
                        boardState         = it.boardState.copy(userArrows = arrows, markedSquares = marks),
                    )
                }
            }
        }
    }

    // ── Move submission ───────────────────────────────────────────────────────

    private fun submitUserMove(preFen: String, move: Move) {
        val st = _uiState.value
        val masterUci = st.masterMoves.getOrNull(st.currentMoveIndex) ?: return

        val userUci = "${move.from.name.lowercase()}${move.to.name.lowercase()}" +
            if (move.promotion != com.github.bhlangonijr.chesslib.Piece.NONE)
                move.promotion.fenSymbol.lowercase() else ""

        val preBoard = Board().apply { loadFromFen(preFen) }
        val userSan   = runCatching { ChessUtils.uciToSan(preBoard, userUci) }.getOrDefault(userUci)
        val masterSan = runCatching { ChessUtils.uciToSan(Board().apply { loadFromFen(preFen) }, masterUci) }.getOrDefault(masterUci)

        // Apply master's move to set the board position
        val masterPostFen = treeBuilder.applyUci(preFen, masterUci) ?: run {
            val b = Board().apply { loadFromFen(preFen) }
            b.doMove(move)
            b.fen
        }

        val masterMove = runCatching {
            val b = Board().apply { loadFromFen(preFen) }
            b.legalMoves().first { m ->
                val uci = "${m.from.name.lowercase()}${m.to.name.lowercase()}"
                uci == masterUci.take(4) || "${uci}${m.promotion.fenSymbol.lowercase()}" == masterUci
            }
        }.getOrNull()

        val isExact = userUci == masterUci
        val annotation = st.moveAnnotations[st.currentMoveIndex]

        currentPostFen = masterPostFen

        val existingUserComment = runCatching {
            // Load synchronously from cache; actual DB load is deferred
            ""
        }.getOrDefault("")

        _uiState.update {
            it.copy(
                phase            = GuessTheMovePhase.MOVE_REVEALED,
                boardState       = it.boardState.copy(
                    fen            = masterPostFen,
                    lastMove       = masterMove,
                    selectedSquare = null,
                    legalMoves     = emptyList(),
                    userArrows     = emptyList(),
                    markedSquares  = emptyList(),
                ),
                userMoveSan           = userSan,
                masterMoveSan         = masterSan,
                wasExactMatch         = isExact,
                originalAnnotation    = annotation,
                showOriginalAnnotation = false,
                currentUserComment    = existingUserComment,
                exactMatches          = if (isExact) it.exactMatches + 1 else it.exactMatches,
                totalPresented        = it.totalPresented + 1,
                engineArrow           = null,
                engineEvalCp          = null,
            )
        }

        // Load the user's existing annotation for this FEN from DB in the background
        viewModelScope.launch(Dispatchers.IO) {
            val saved = runCatching { annotationDao.getByFen(masterPostFen) }.getOrNull()
            val arrows = saved?.arrowsJson?.let { parseArrows(it) } ?: emptyList()
            val marks  = saved?.markedSquaresJson?.let { parseMarks(it) } ?: emptyList()
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        currentUserComment = saved?.moveComment ?: "",
                        boardState = it.boardState.copy(userArrows = arrows, markedSquares = marks),
                    )
                }
            }
        }
    }

    // ── MOVE_REVEALED actions ─────────────────────────────────────────────────

    fun toggleOriginalAnnotation() {
        _uiState.update { it.copy(showOriginalAnnotation = !it.showOriginalAnnotation) }
    }

    fun updateUserComment(text: String) {
        _uiState.update { it.copy(currentUserComment = text) }
    }

    fun saveUserAnnotation() {
        val st = _uiState.value
        val fen = currentPostFen
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val existing = annotationDao.getByFen(fen)
                val upd = (existing ?: PositionAnnotation(fen = fen)).copy(
                    moveComment       = st.currentUserComment,
                    arrowsJson        = gson.toJson(st.boardState.userArrows),
                    markedSquaresJson = gson.toJson(st.boardState.markedSquares),
                )
                annotationDao.upsert(upd)
            }.onFailure { Log.e(TAG, "saveUserAnnotation failed", it) }
        }
    }

    fun requestEngineAnalysis() {
        val fen = currentPostFen
        _uiState.update { it.copy(engineThinking = true, engineArrow = null, engineEvalCp = null) }
        viewModelScope.launch(Dispatchers.Default) {
            val result = runCatching {
                engine.analyzePosition(fen, ChessConstants.DEFAULT_ANALYSIS_DEPTH)
            }.getOrNull()

            val arrow = result?.bestMoveUci?.let { uci ->
                if (uci.length >= 4) {
                    runCatching {
                        val from = Square.valueOf(uci.substring(0, 2).uppercase())
                        val to   = Square.valueOf(uci.substring(2, 4).uppercase())
                        Arrow(from, to, Color(0xFF2196F3))
                    }.getOrNull()
                } else null
            }

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        engineThinking = false,
                        engineArrow    = arrow,
                        engineEvalCp   = result?.score,
                    )
                }
            }
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun continueToNextMove() {
        saveUserAnnotation()
        val st = _uiState.value
        val nextIndex = st.currentMoveIndex + 1

        if (nextIndex >= st.masterMoves.size) {
            _uiState.update {
                it.copy(
                    phase          = GuessTheMovePhase.GAME_COMPLETE,
                    engineArrow    = null,
                    engineEvalCp   = null,
                )
            }
            saveSession()
            return
        }

        _uiState.update {
            it.copy(
                currentMoveIndex       = nextIndex,
                phase                  = GuessTheMovePhase.GUESSING,
                isEditorMode           = false,
                originalAnnotation     = null,
                showOriginalAnnotation = false,
                engineArrow            = null,
                engineEvalCp           = null,
                userMoveSan            = "",
                masterMoveSan          = "",
                boardState             = it.boardState.copy(
                    selectedSquare = null,
                    legalMoves     = emptyList(),
                    isEditorMode   = false,
                ),
            )
        }

        maybeAutoAdvance(nextIndex, st.masterMoves, st.selectedSide)
    }

    private fun maybeAutoAdvance(index: Int, moves: List<String>, side: GuessingSide) {
        if (index >= moves.size) return

        // White moves are at even indices (0, 2, 4…), Black at odd (1, 3, 5…)
        val isWhiteToMove = (index % 2 == 0)
        // Always auto-play the very first move so the user can see the opening
        val shouldAutoAdvance = if (index == 0) {
            true
        } else {
            when (side) {
                GuessingSide.BOTH       -> false
                GuessingSide.WHITE_ONLY -> !isWhiteToMove
                GuessingSide.BLACK_ONLY -> isWhiteToMove
            }
        }

        if (shouldAutoAdvance) {
            viewModelScope.launch {
                delay(ChessConstants.OPPONENT_REPLY_DELAY_MS)
                val st = _uiState.value
                val masterUci = moves.getOrNull(index) ?: return@launch
                val newFen = treeBuilder.applyUci(st.boardState.fen, masterUci) ?: return@launch
                val masterMove = runCatching {
                    Board().apply { loadFromFen(st.boardState.fen) }.legalMoves().firstOrNull { m ->
                        val uci = "${m.from.name.lowercase()}${m.to.name.lowercase()}"
                        uci == masterUci.take(4) || "${uci}${m.promotion.fenSymbol.lowercase()}" == masterUci
                    }
                }.getOrNull()

                currentPostFen = newFen
                val nextIndex = index + 1

                if (nextIndex >= moves.size) {
                    _uiState.update {
                        it.copy(
                            phase        = GuessTheMovePhase.GAME_COMPLETE,
                            currentMoveIndex = nextIndex,
                            boardState   = it.boardState.copy(fen = newFen, lastMove = masterMove),
                        )
                    }
                    saveSession()
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        currentMoveIndex = nextIndex,
                        phase            = GuessTheMovePhase.GUESSING,
                        isEditorMode     = false,
                        boardState       = it.boardState.copy(
                            fen            = newFen,
                            lastMove       = masterMove,
                            selectedSquare = null,
                            legalMoves     = emptyList(),
                            isEditorMode   = false,
                        ),
                    )
                }
                // Recurse in case multiple consecutive opponent moves
                maybeAutoAdvance(nextIndex, moves, side)
            }
        }
    }

    // ── Review mode ───────────────────────────────────────────────────────────

    fun startReview() {
        val moves = _uiState.value.masterMoves
        viewModelScope.launch(Dispatchers.Default) {
            val fens  = mutableListOf(START_FEN)
            val sans  = mutableListOf<String>()
            var fen   = START_FEN
            for (uci in moves) {
                val board = Board().apply { loadFromFen(fen) }
                sans.add(runCatching { ChessUtils.uciToSan(board, uci) }.getOrDefault(uci))
                fen = treeBuilder.applyUci(fen, uci) ?: break
                fens.add(fen)
            }
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        phase             = GuessTheMovePhase.REVIEWING,
                        fenHistory        = fens,
                        masterSanHistory  = sans,
                        reviewIndex       = fens.lastIndex,
                        boardState        = it.boardState.copy(
                            fen            = fens.last(),
                            lastMove       = resolveLastMove(fens, moves, fens.lastIndex),
                            selectedSquare = null,
                            legalMoves     = emptyList(),
                            isEditorMode   = false,
                            userArrows     = emptyList(),
                            markedSquares  = emptyList(),
                        ),
                    )
                }
            }
        }
    }

    fun reviewGoTo(index: Int) {
        val st      = _uiState.value
        val clamped = index.coerceIn(0, st.fenHistory.lastIndex)
        val fen     = st.fenHistory.getOrElse(clamped) { START_FEN }
        _uiState.update {
            it.copy(
                reviewIndex = clamped,
                boardState  = it.boardState.copy(
                    fen            = fen,
                    lastMove       = resolveLastMove(it.fenHistory, it.masterMoves, clamped),
                    selectedSquare = null,
                    legalMoves     = emptyList(),
                ),
            )
        }
    }

    fun exitReview() {
        _uiState.update { it.copy(phase = GuessTheMovePhase.GAME_COMPLETE) }
    }

    private fun resolveLastMove(fens: List<String>, moves: List<String>, index: Int): Move? {
        if (index <= 0) return null
        val uci    = moves.getOrNull(index - 1) ?: return null
        val preFen = fens.getOrElse(index - 1) { START_FEN }
        return runCatching {
            Board().apply { loadFromFen(preFen) }.legalMoves().firstOrNull { m ->
                val u = "${m.from.name.lowercase()}${m.to.name.lowercase()}"
                u == uci.take(4) || "${u}${m.promotion.fenSymbol.lowercase()}" == uci
            }
        }.getOrNull()
    }

    fun restartSelection() {
        currentPostFen = START_FEN
        _uiState.update {
            GuessTheMoveUiState(
                selectedSource   = it.selectedSource,
                selectedSide     = it.selectedSide,
                selectedPlatform = it.selectedPlatform,
                customUsername   = it.customUsername,
            )
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun saveSession() {
        val st = _uiState.value
        if (st.totalPresented == 0) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                dao.insert(
                    GuessMoveSession(
                        gameDescription = st.gameDescription,
                        sourceLabel     = st.sourceLabel,
                        totalMoves      = st.totalPresented,
                        exactMatches    = st.exactMatches,
                        guessingSide    = st.selectedSide.name,
                    )
                )
            }.onFailure { Log.e(TAG, "saveSession failed", it) }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parseArrows(json: String): List<Arrow> = runCatching {
        gson.fromJson<List<Arrow>>(json, object : TypeToken<List<Arrow>>() {}.type)
    }.getOrDefault(emptyList())

    private fun parseMarks(json: String): List<MarkedSquare> = runCatching {
        gson.fromJson<List<MarkedSquare>>(json, object : TypeToken<List<MarkedSquare>>() {}.type)
    }.getOrDefault(emptyList())
}
