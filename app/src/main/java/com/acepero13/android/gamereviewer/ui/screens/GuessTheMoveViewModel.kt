package com.acepero13.android.gamereviewer.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.db.GuessMoveProgressDao
import com.acepero13.android.gamereviewer.data.db.GuessMoveSessionDao
import com.acepero13.android.gamereviewer.data.model.GuessMoveProgress
import com.acepero13.android.gamereviewer.data.model.GuessMoveSession
import com.acepero13.android.gamereviewer.data.model.Snippet
import com.acepero13.android.gamereviewer.data.repository.SettingsRepository
import com.acepero13.android.gamereviewer.data.repository.SnippetRepository
import com.acepero13.android.gamereviewer.domain.extractMoveAnnotations
import com.acepero13.android.gamereviewer.domain.extractPreambleAnnotation
import com.acepero13.android.gamereviewer.domain.pgnToUciMoves
import com.acepero13.android.gamereviewer.ui.components.OpeningExplorerUiState
import com.acepero13.android.gamereviewer.ui.screens.analysis.OpeningExplorerController
import com.acepero13.chess.core.data.db.PositionAnnotationDao
import com.acepero13.chess.core.data.model.ChessConstants
import com.acepero13.chess.core.data.model.PositionAnnotation
import com.acepero13.chess.core.engine.StockfishEngine
import com.acepero13.chess.core.pgn.PgnImporter
import com.acepero13.chess.core.ui.board.Arrow
import com.acepero13.chess.core.ui.board.BoardState
import com.acepero13.chess.core.ui.board.MarkedSquare
import com.acepero13.chess.core.util.ChessUtils
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Square
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

class GuessTheMoveViewModel(
    context: Context,
    importer: PgnImporter,
    private val dao: GuessMoveSessionDao,
    private val progressDao: GuessMoveProgressDao,
    private val annotationDao: PositionAnnotationDao,
    private val engine: StockfishEngine,
    private val snippetRepo: SnippetRepository,
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuessTheMoveUiState())
    val uiState: StateFlow<GuessTheMoveUiState> = _uiState.asStateFlow()

    private val gson        = Gson()
    private val gameService = MasterGameService(context, importer)
    private val moveEngine  = GuessTheMoveGameEngine()

    private var currentPostFen: String  = START_FEN
    private var allFenSequence: List<String> = listOf(START_FEN)
    private var allSanSequence: List<String> = emptyList()
    private var currentGameIndex: Int = -1

    private var _lichessToken: String = ""

    private val explorerController = OpeningExplorerController(
        scope           = viewModelScope,
        onArrowsChanged = {},
        lichessToken    = { _lichessToken.ifBlank { null } },
    )
    val explorerState: StateFlow<OpeningExplorerUiState> = explorerController.state

    init {
        viewModelScope.launch { settingsRepo.lichessApiToken.collect { _lichessToken = it } }
    }

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
                val (pgn, label) = gameService.fetch(st.selectedSource, st.customUsername, st.selectedPlatform)
                val picked = gameService.pickGame(gameService.splitGames(pgn))
                    ?: error("No suitable game found")
                startGameFromPgn(picked, label, st.selectedSide)
            }.onFailure { e ->
                Log.e(TAG, "startSession failed", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(phase = GuessTheMovePhase.SELECTING, fetchError = e.message ?: "Failed to load game") }
                }
            }
        }
    }

    fun startWithGameAtIndex(index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = runCatching { progressDao.findByGameIndex(index) }.getOrNull()
            if (existing != null) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(pendingResume = existing) }
                }
                return@launch
            }
            val side = _uiState.value.selectedSide
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(phase = GuessTheMovePhase.LOADING, fetchError = null) }
            }
            runCatching {
                currentGameIndex = index
                startGameFromPgn(gameService.loadGameAtIndex(index), "Offline", side)
            }.onFailure { e ->
                Log.e(TAG, "startWithGameAtIndex failed", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(phase = GuessTheMovePhase.SELECTING, fetchError = e.message ?: "Failed to load game") }
                }
            }
        }
    }

    fun confirmResume() {
        val progress = _uiState.value.pendingResume ?: return
        _uiState.update { it.copy(pendingResume = null, phase = GuessTheMovePhase.LOADING) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val side = GuessingSide.valueOf(progress.guessingSide)
                currentGameIndex = progress.gameIndex
                startGameFromPgn(gameService.loadGameAtIndex(progress.gameIndex), progress.sourceLabel, side)
                val resumeFen = allFenSequence.getOrElse(progress.currentMoveIndex) { START_FEN }
                currentPostFen = resumeFen
                val masterMoves = _uiState.value.masterMoves
                val lastMove = if (progress.currentMoveIndex > 0)
                    moveEngine.resolveLastMove(allFenSequence, masterMoves, progress.currentMoveIndex)
                else null
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            phase = GuessTheMovePhase.GUESSING,
                            currentMoveIndex = progress.currentMoveIndex,
                            exactMatches = progress.exactMatches,
                            totalPresented = progress.totalPresented,
                            boardState = it.boardState.copy(fen = resumeFen, lastMove = lastMove,
                                selectedSquare = null, legalMoves = emptyList()),
                        )
                    }
                    maybeAutoAdvance(progress.currentMoveIndex, masterMoves, side)
                }
            }.onFailure { e ->
                Log.e(TAG, "confirmResume failed", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(phase = GuessTheMovePhase.SELECTING, fetchError = e.message ?: "Failed to resume") }
                }
            }
        }
    }

    fun startFresh() {
        val progress = _uiState.value.pendingResume ?: return
        _uiState.update { it.copy(pendingResume = null) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { progressDao.deleteByGameIndex(progress.gameIndex) }
        }
        startWithGameAtIndex(progress.gameIndex)
    }

    private suspend fun startGameFromPgn(gameText: String, sourceLabel: String, side: GuessingSide) {
        val parsed = gameService.parseGame(gameText) ?: error("Failed to parse selected game")
        val uciMoves = pgnToUciMoves(parsed.movesPgn).split(" ").filter { it.isNotBlank() }
        if (uciMoves.isEmpty()) error("Game has no parseable moves")

        val annotations = extractMoveAnnotations(parsed.movesPgn)
        val preamble    = extractPreambleAnnotation(parsed.movesPgn)
        val gameDesc    = gameService.buildGameDescription(parsed.headers)

        val seqs = withContext(Dispatchers.Default) { moveEngine.buildSequences(uciMoves) }
        allFenSequence = seqs.fens
        allSanSequence = seqs.sans

        withContext(Dispatchers.Main) {
            currentPostFen = START_FEN
            _uiState.update {
                it.copy(
                    phase = GuessTheMovePhase.CHOOSING_SIDE, gameDescription = gameDesc,
                    sourceLabel = sourceLabel,
                    whitePlayer = parsed.headers["White"] ?: "",
                    blackPlayer = parsed.headers["Black"] ?: "",
                    masterMoves = uciMoves, moveAnnotations = annotations,
                    preambleAnnotation = preamble, preambleDismissed = false,
                    currentMoveIndex = 0, boardState = BoardState(fen = START_FEN, isEditorMode = false),
                    isEditorMode = false, exactMatches = 0, totalPresented = 0,
                    userMoveSan = "", masterMoveSan = "", wasExactMatch = false,
                    originalAnnotation = null, currentUserComment = "",
                    engineArrow = null, engineEvalCp = null, selectedSide = side, treeItems = emptyList(),
                )
            }
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
            val board   = Board().apply { loadFromFen(cur.fen) }
            val move    = ChessUtils.buildMove(board, selected, square, solutionUci = null)
            val isLegal = board.legalMoves().contains(move)
            if (isLegal) submitUserMove(cur.fen, move)
            else _uiState.update { it.copy(boardState = cur.copy(selectedSquare = null, legalMoves = emptyList())) }
        }
    }

    fun onArrowDrawn(from: Square, to: Square) {
        val st      = _uiState.value
        val updated = st.boardState.userArrows.toMutableList()
        if (!updated.removeIf { it.from == from && it.to == to }) updated.add(Arrow(from, to, st.currentArrowColor))
        _uiState.update { it.copy(boardState = st.boardState.copy(userArrows = updated)) }
    }

    fun onSquareMarked(square: Square) {
        val st      = _uiState.value
        val updated = st.boardState.markedSquares.toMutableList()
        if (!updated.removeIf { it.square == square }) updated.add(MarkedSquare(square, st.currentArrowColor))
        _uiState.update { it.copy(boardState = st.boardState.copy(markedSquares = updated)) }
    }

    fun updateArrowColor(color: Color) { _uiState.update { it.copy(currentArrowColor = color) } }

    fun clearDrawings() {
        _uiState.update { it.copy(boardState = it.boardState.copy(userArrows = emptyList(), markedSquares = emptyList())) }
    }

    fun toggleEditorMode() {
        val newMode = !_uiState.value.isEditorMode
        _uiState.update { it.copy(isEditorMode = newMode, boardState = it.boardState.copy(isEditorMode = newMode)) }
    }

    fun skipMove() {
        val st        = _uiState.value
        if (st.phase != GuessTheMovePhase.GUESSING) return
        val masterUci = st.masterMoves.getOrNull(st.currentMoveIndex) ?: return
        saveUserAnnotation()
        val reveal    = moveEngine.computeSkipReveal(st.boardState.fen, masterUci)
        currentPostFen = reveal.postFen
        applyRevealState(reveal, isSkip = true, annotation = st.moveAnnotations[st.currentMoveIndex])
        loadAnnotationsForFen(reveal.postFen)
    }

    // ── Move submission ───────────────────────────────────────────────────────

    private fun submitUserMove(preFen: String, move: com.github.bhlangonijr.chesslib.move.Move) {
        val st        = _uiState.value
        val masterUci = st.masterMoves.getOrNull(st.currentMoveIndex) ?: return
        saveUserAnnotation()
        val reveal    = moveEngine.computeMoveReveal(preFen, move, masterUci)
        currentPostFen = reveal.postFen
        applyRevealState(reveal, isSkip = false, annotation = st.moveAnnotations[st.currentMoveIndex])
        loadAnnotationsForFen(reveal.postFen)
    }

    private fun applyRevealState(reveal: MoveRevealData, isSkip: Boolean, annotation: String?) {
        _uiState.update {
            it.copy(
                phase = GuessTheMovePhase.MOVE_REVEALED,
                boardState = it.boardState.copy(
                    fen = reveal.postFen, lastMove = reveal.masterMove,
                    selectedSquare = null, legalMoves = emptyList(),
                    userArrows = emptyList(), markedSquares = emptyList(),
                ),
                userMoveSan = if (isSkip) "—" else reveal.userSan,
                masterMoveSan = reveal.masterSan,
                wasExactMatch = reveal.isExact,
                originalAnnotation = annotation, opponentAnnotation = null,
                currentUserComment = "", engineArrow = null, engineEvalCp = null,
                exactMatches   = if (reveal.isExact) it.exactMatches + 1 else it.exactMatches,
                totalPresented = if (isSkip) it.totalPresented else it.totalPresented + 1,
            )
        }
        rebuildTreeItems()
        reloadExplorerIfActive(reveal.postFen)
        // Save progress pointing at the NEXT move so closing in MOVE_REVEALED resumes correctly
        saveProgress(_uiState.value.currentMoveIndex + 1)
    }

    private fun loadAnnotationsForFen(fen: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val saved  = runCatching { annotationDao.getByFen(fen) }.getOrNull()
            val arrows = saved?.arrowsJson?.let { parseArrows(it) } ?: emptyList()
            val marks  = saved?.markedSquaresJson?.let { parseMarks(it) } ?: emptyList()
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(currentUserComment = saved?.moveComment ?: "",
                        boardState = it.boardState.copy(userArrows = arrows, markedSquares = marks))
                }
            }
        }
    }

    fun dismissPreamble() { _uiState.update { it.copy(preambleDismissed = true) } }

    // ── MOVE_REVEALED actions ─────────────────────────────────────────────────

    fun updateUserComment(text: String) { _uiState.update { it.copy(currentUserComment = text) } }

    fun saveUserAnnotation() {
        val st  = _uiState.value
        val fen = currentPostFen
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val existing = annotationDao.getByFen(fen)
                val upd = (existing ?: PositionAnnotation(fen = fen)).copy(
                    moveComment = st.currentUserComment,
                    arrowsJson = gson.toJson(st.boardState.userArrows),
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
            val result = runCatching { engine.analyzePosition(fen, ChessConstants.DEFAULT_ANALYSIS_DEPTH) }.getOrNull()
            val arrow  = result?.bestMoveUci?.takeIf { it.length >= 4 }?.let { uci ->
                runCatching {
                    Arrow(Square.valueOf(uci.substring(0, 2).uppercase()),
                        Square.valueOf(uci.substring(2, 4).uppercase()), Color(0xFF2196F3))
                }.getOrNull()
            }
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(engineThinking = false, engineArrow = arrow, engineEvalCp = result?.score) }
            }
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun continueToNextMove() {
        saveUserAnnotation()
        val st        = _uiState.value
        val nextIndex = st.currentMoveIndex + 1
        if (nextIndex >= st.masterMoves.size) {
            _uiState.update { it.copy(phase = GuessTheMovePhase.GAME_COMPLETE, engineArrow = null, engineEvalCp = null) }
            saveSession()
            return
        }
        _uiState.update {
            it.copy(
                currentMoveIndex = nextIndex, phase = GuessTheMovePhase.GUESSING,
                isEditorMode = false, originalAnnotation = null,
                engineArrow = null, engineEvalCp = null, userMoveSan = "", masterMoveSan = "",
                currentUserComment = "",
                boardState = it.boardState.copy(selectedSquare = null, legalMoves = emptyList(), isEditorMode = false),
            )
        }
        saveProgress()
        loadAnnotationsForFen(currentPostFen)
        maybeAutoAdvance(nextIndex, st.masterMoves, st.selectedSide)
    }

    private fun maybeAutoAdvance(index: Int, moves: List<String>, side: GuessingSide) {
        if (index >= moves.size) return
        val isWhiteToMove = (index % 2 == 0)
        val shouldAdvance = index == 0 || when (side) {
            GuessingSide.BOTH       -> false
            GuessingSide.WHITE_ONLY -> !isWhiteToMove
            GuessingSide.BLACK_ONLY -> isWhiteToMove
        }
        if (!shouldAdvance) return

        viewModelScope.launch {
            delay(ChessConstants.OPPONENT_REPLY_DELAY_MS)
            val st        = _uiState.value
            val masterUci = moves.getOrNull(index) ?: return@launch
            val newFen    = moveEngine.applyUci(st.boardState.fen, masterUci) ?: return@launch
            val masterMove = moveEngine.resolveLastMove(listOf(st.boardState.fen, newFen), listOf(masterUci), 1)
            currentPostFen = newFen
            val nextIndex  = index + 1
            if (nextIndex >= moves.size) {
                _uiState.update {
                    it.copy(phase = GuessTheMovePhase.GAME_COMPLETE, currentMoveIndex = nextIndex,
                        boardState = it.boardState.copy(fen = newFen, lastMove = masterMove))
                }
                rebuildTreeItems(); saveSession(); return@launch
            }
            _uiState.update {
                it.copy(currentMoveIndex = nextIndex, phase = GuessTheMovePhase.GUESSING,
                    isEditorMode = false, opponentAnnotation = _uiState.value.moveAnnotations[index],
                    currentUserComment = "",
                    boardState = it.boardState.copy(fen = newFen, lastMove = masterMove,
                        selectedSquare = null, legalMoves = emptyList(), isEditorMode = false))
            }
            rebuildTreeItems()
            loadAnnotationsForFen(newFen)
            maybeAutoAdvance(nextIndex, moves, side)
        }
    }

    // ── Review mode ───────────────────────────────────────────────────────────

    fun startReview() {
        val fens = allFenSequence.takeIf { it.size > 1 } ?: listOf(START_FEN)
        _uiState.update {
            it.copy(
                phase = GuessTheMovePhase.REVIEWING, fenHistory = fens,
                masterSanHistory = allSanSequence, reviewIndex = fens.lastIndex,
                boardState = it.boardState.copy(fen = fens.last(),
                    lastMove = moveEngine.resolveLastMove(fens, it.masterMoves, fens.lastIndex),
                    selectedSquare = null, legalMoves = emptyList(),
                    isEditorMode = false, userArrows = emptyList(), markedSquares = emptyList()),
            )
        }
        rebuildTreeItems()
    }

    fun reviewGoTo(index: Int) {
        val clamped = index.coerceIn(0, _uiState.value.fenHistory.lastIndex)
        val newFen  = _uiState.value.fenHistory.getOrElse(clamped) { START_FEN }
        _uiState.update {
            it.copy(reviewIndex = clamped, boardState = it.boardState.copy(
                fen = newFen,
                lastMove = moveEngine.resolveLastMove(it.fenHistory, it.masterMoves, clamped),
                selectedSquare = null, legalMoves = emptyList()))
        }
        rebuildTreeItems()
        reloadExplorerIfActive(newFen)
    }

    fun exitReview() { _uiState.update { it.copy(phase = GuessTheMovePhase.GAME_COMPLETE) } }

    fun restartSelection() {
        currentPostFen = START_FEN
        _uiState.update {
            GuessTheMoveUiState(selectedSource = it.selectedSource, selectedSide = it.selectedSide,
                selectedPlatform = it.selectedPlatform, customUsername = it.customUsername)
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun saveProgress(moveIndexOverride: Int? = null) {
        if (currentGameIndex < 0) return
        val st = _uiState.value
        if (st.masterMoves.isEmpty()) return
        val indexToSave = moveIndexOverride ?: st.currentMoveIndex
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                progressDao.upsert(GuessMoveProgress(
                    gameIndex = currentGameIndex,
                    gameDescription = st.gameDescription,
                    sourceLabel = st.sourceLabel,
                    currentMoveIndex = indexToSave,
                    totalMoves = st.masterMoves.size,
                    exactMatches = st.exactMatches,
                    totalPresented = st.totalPresented,
                    guessingSide = st.selectedSide.name,
                ))
            }.onFailure { Log.e(TAG, "saveProgress failed", it) }
        }
    }

    private fun saveSession() {
        val st = _uiState.value
        if (st.totalPresented == 0) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                dao.insert(GuessMoveSession(gameDescription = st.gameDescription,
                    sourceLabel = st.sourceLabel, totalMoves = st.totalPresented,
                    exactMatches = st.exactMatches, guessingSide = st.selectedSide.name))
                if (currentGameIndex >= 0) progressDao.deleteByGameIndex(currentGameIndex)
            }.onFailure { Log.e(TAG, "saveSession failed", it) }
        }
    }

    // ── Move tree ─────────────────────────────────────────────────────────────

    fun onTreeNodeClick(nodeId: Long) {
        if (_uiState.value.phase == GuessTheMovePhase.REVIEWING) reviewGoTo(nodeId.toInt())
    }

    private fun rebuildTreeItems() {
        val st     = _uiState.value
        val posIdx = when (st.phase) {
            GuessTheMovePhase.MOVE_REVEALED -> st.currentMoveIndex + 1
            GuessTheMovePhase.REVIEWING     -> st.reviewIndex
            else                            -> st.currentMoveIndex
        }
        val upTo = if (st.phase == GuessTheMovePhase.REVIEWING) allSanSequence.size else posIdx
        val anns = if (st.phase == GuessTheMovePhase.REVIEWING) st.moveAnnotations else emptyMap<Int, String>()
        _uiState.update { it.copy(treeItems = moveEngine.buildTreeItems(upTo, posIdx, allSanSequence, allFenSequence, anns)) }
    }

    // ── Opening Explorer ──────────────────────────────────────────────────────

    fun toggleExplorer() {
        val nowShown = !_uiState.value.showExplorer
        _uiState.update { it.copy(showExplorer = nowShown) }
        if (nowShown) explorerController.load(_uiState.value.boardState.fen)
        else explorerController.clear()
    }

    private fun reloadExplorerIfActive(fen: String) {
        if (_uiState.value.showExplorer) explorerController.load(fen)
    }

    // ── Snippet bookmarking ───────────────────────────────────────────────────

    fun openBookmarkSheet()    { _uiState.update { it.copy(showBookmarkSheet = true) } }
    fun dismissBookmarkSheet() { _uiState.update { it.copy(showBookmarkSheet = false) } }

    fun saveSnippet(title: String, tags: String, notes: String) {
        val st  = _uiState.value
        val fen = st.boardState.fen
        val moveIndex = when (st.phase) {
            GuessTheMovePhase.MOVE_REVEALED -> st.currentMoveIndex
            GuessTheMovePhase.REVIEWING     -> st.reviewIndex
            else                            -> 0
        }
        dismissBookmarkSheet()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                snippetRepo.insert(Snippet(
                    title = title.ifBlank { "Position at move ${moveIndex + 1}" },
                    fen   = fen,
                    tags  = tags,
                    notes = notes,
                ))
            }.onFailure { Log.e(TAG, "saveSnippet failed", it) }
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
