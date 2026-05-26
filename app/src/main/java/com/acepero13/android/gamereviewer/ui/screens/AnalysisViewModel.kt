package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.db.CriticalMomentDao
import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.domain.TruthMapBuilder
import com.acepero13.android.gamereviewer.domain.TruthMapEntry
import com.acepero13.chess.core.data.model.ChessConstants
import com.acepero13.chess.core.data.db.PositionAnnotationDao
import com.acepero13.chess.core.data.model.PositionAnnotation
import com.acepero13.chess.core.engine.StockfishEngine
import com.acepero13.chess.core.opening.OpeningClassifier
import com.acepero13.chess.core.ui.board.Arrow
import com.acepero13.chess.core.ui.board.BoardState
import com.acepero13.chess.core.ui.board.MarkedSquare
import com.acepero13.chess.core.ui.components.TreeDisplayItem
import com.acepero13.chess.core.util.ChessUtils
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
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

// ── State ─────────────────────────────────────────────────────────────────────

data class AnalysisUiState(
    val game: ReviewGame? = null,
    val moveIndex: Int = 0,             // 0 = before first move (starting position)
    val totalMoves: Int = 0,
    val boardState: BoardState = BoardState(fen = START_FEN),

    // Move tree
    val treeItems: List<TreeDisplayItem> = emptyList(),

    // Opening summary (non-judgmental header)
    val openingSummary: String = "",
    val phaseSummary: String = "",

    // Background analysis (hidden from user)
    val backgroundAnalysisProgress: Float = 0f,     // 0..1
    val isBackgroundAnalysisDone: Boolean = false,

    // Annotation for the current position
    val currentComment: String = "",
    val hasAnnotationAtCurrent: Boolean = false,

    // Missed Moment intervention
    val showMissedMomentBanner: Boolean = false,
    val missedMomentMoveIndex: Int? = null,

    // Critical moment bottom-sheet questionnaire
    val showCriticalSheet: Boolean = false,

    // Sandbox mode (Milestone 2)
    val sandboxMode: Boolean = false,
    val sandboxEngineThinking: Boolean = false,
    val blunderGuardActive: Boolean = false,        // board flash
    val blunderGuardMessage: String = "",

    // Critical moments for this game
    val criticalMoments: List<CriticalMoment> = emptyList(),
)

private const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

// ── ViewModel ─────────────────────────────────────────────────────────────────

class AnalysisViewModel(
    private val gameId: Long,
    private val repo: GameRepository,
    private val annotationDao: PositionAnnotationDao,
    private val criticalMomentDao: CriticalMomentDao,
    private val engine: StockfishEngine,
    private val opening: OpeningClassifier,
    private val truthMapBuilder: TruthMapBuilder,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    /** Full UCI move list for the loaded game. */
    private var uciMoves: List<String> = emptyList()

    /** Pre-computed SAN notation for every move (parallel to uciMoves). */
    private var sanMoves: List<String> = emptyList()

    /** Pre-computed FEN after each move. Index 0 = START_FEN, index n = FEN after move n. */
    private var fenSequence: List<String> = emptyList()

    /** The hidden Truth Map — never exposed in UiState. */
    private var truthMap: List<TruthMapEntry> = emptyList()

    /** Annotation cache keyed by FEN to reduce DB round-trips. */
    private val annotationCache = mutableMapOf<String, PositionAnnotation?>()

    private val gson = Gson()

    init {
        loadGame()
    }

    // ── Public navigation API ─────────────────────────────────────────────────

    fun goToMove(index: Int) {
        val clamped = index.coerceIn(0, uciMoves.size)
        val prev = _uiState.value.moveIndex

        // Detect missed moments: user skipped forward past a critical position
        if (clamped > prev) {
            checkMissedMoments(fromIndex = prev, toIndex = clamped)
        }

        applyMoveIndex(clamped)
    }

    fun stepForward()  = goToMove(_uiState.value.moveIndex + 1)
    fun stepBackward() = goToMove(_uiState.value.moveIndex - 1)
    fun goToStart()    = goToMove(0)
    fun goToEnd()      = goToMove(uciMoves.size)

    /** Called when the user taps a move in the MoveTree. */
    fun onMoveNodeClick(nodeId: Long) = goToMove(nodeId.toInt())

    // ── Annotation API ────────────────────────────────────────────────────────

    fun onArrowDrawn(from: Square, to: Square) {
        val current = _uiState.value.boardState
        val arrow = Arrow(from, to, Color(0xCCF0A500.toInt()))
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
        _uiState.update { it.copy(currentComment = comment) }
        viewModelScope.launch(Dispatchers.IO) {
            val fen = _uiState.value.boardState.fen
            val existing = getCachedAnnotation(fen)
            val updated = (existing ?: PositionAnnotation(fen = fen)).copy(moveComment = comment)
            annotationDao.upsert(updated)
            annotationCache[fen] = updated
            refreshTreeItems()          // dot-indicator may change
        }
    }

    // ── Critical moment / questionnaire API ──────────────────────────────────

    fun markCurrentAsCritical() {
        _uiState.update { it.copy(showCriticalSheet = true) }
    }

    fun dismissCriticalSheet() {
        _uiState.update { it.copy(showCriticalSheet = false) }
    }

    /**
     * Called when the user submits the BottomSheet questionnaire.
     * Serialises the answers into the position annotation and saves a USER_MARKED
     * critical moment.
     */
    fun saveCriticalAnswers(plan: String, threats: String, candidates: String) {
        val idx = _uiState.value.moveIndex
        val fen = _uiState.value.boardState.fen
        val comment = buildString {
            appendLine("### Self-Analysis")
            if (plan.isNotBlank())       appendLine("**My plan:** $plan")
            if (threats.isNotBlank())    appendLine("**Threats I see:** $threats")
            if (candidates.isNotBlank()) appendLine("**Candidates:** $candidates")
        }.trim()

        viewModelScope.launch(Dispatchers.IO) {
            // Persist annotation
            val existing = getCachedAnnotation(fen)
            val updated = (existing ?: PositionAnnotation(fen = fen)).copy(moveComment = comment)
            annotationDao.upsert(updated)
            annotationCache[fen] = updated

            // Save user-marked critical moment
            val moment = CriticalMoment(
                gameId         = gameId,
                moveIndex      = idx,
                type           = CriticalMoment.Type.USER_MARKED.name,
                severity       = 0,  // user doesn't know the eval yet
                reasonCategory = CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE.name,
                explanationState = CriticalMoment.ExplanationState.HIDDEN.name,
                fen            = fen,
            )
            criticalMomentDao.insert(moment)

            _uiState.update {
                it.copy(
                    showCriticalSheet  = false,
                    currentComment     = comment,
                    hasAnnotationAtCurrent = true,
                )
            }
            refreshTreeItems()
        }
    }

    // ── Missed Moment banner ──────────────────────────────────────────────────

    fun dismissMissedMomentBanner() {
        _uiState.update { it.copy(showMissedMomentBanner = false, missedMomentMoveIndex = null) }
    }

    /**
     * Jump back to the missed critical position so the user can annotate it.
     */
    fun reviewMissedMoment() {
        val idx = _uiState.value.missedMomentMoveIndex ?: return
        dismissMissedMomentBanner()
        goToMove(idx)
    }

    // ── Sandbox mode (Milestone 2) ────────────────────────────────────────────

    fun enterSandboxMode() {
        _uiState.update {
            it.copy(
                sandboxMode = true,
                boardState  = it.boardState.copy(isEditorMode = true),
            )
        }
    }

    fun exitSandboxMode() {
        _uiState.update { it.copy(sandboxMode = false, blunderGuardActive = false) }
        applyMoveIndex(_uiState.value.moveIndex) // re-apply main line position
    }

    fun dismissBlunderGuard() {
        _uiState.update { it.copy(blunderGuardActive = false, blunderGuardMessage = "") }
    }

    /**
     * User tapped a square in sandbox mode — handle piece selection + move execution.
     */
    fun onSandboxSquareTap(square: Square) {
        if (_uiState.value.sandboxEngineThinking) return
        val current = _uiState.value.boardState
        val selected = current.selectedSquare

        if (selected == null) {
            // Select piece if there's one belonging to the side to move
            val fen = current.fen
            val board = Board()
            board.loadFromFen(fen)
            val piece = board.getPiece(square)
            if (piece != Piece.NONE && piece.pieceSide == board.sideToMove) {
                val legalMoves = board.legalMoves().filter { it.from == square }
                _uiState.update {
                    it.copy(
                        boardState = current.copy(
                            selectedSquare = square,
                            legalMoves     = legalMoves,
                        )
                    )
                }
            }
        } else {
            // Attempt move
            val board = Board()
            board.loadFromFen(current.fen)
            val move = ChessUtils.buildMove(board, selected, square, solutionUci = null)
            val legalMoves = board.legalMoves()

            if (legalMoves.contains(move)) {
                executeSandboxMove(board, move)
            } else {
                // Deselect
                _uiState.update {
                    it.copy(boardState = current.copy(selectedSquare = null, legalMoves = emptyList()))
                }
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun loadGame() {
        viewModelScope.launch(Dispatchers.IO) {
            val game = repo.findById(gameId) ?: return@launch
            uciMoves = game.movesUci.split(' ').filter { it.isNotBlank() }

            // Pre-compute FEN sequence + SAN list
            buildFenAndSanSequence()

            // Load existing critical moments from DB
            val storedMoments = criticalMomentDao.getByGameId(gameId)

            // Opening summary (non-judgmental header — no accuracy scores)
            val openingEntry = runCatching {
                opening.classifyByMoves(uciMoves.take(20))
            }.getOrNull()

            _uiState.update {
                it.copy(
                    game            = game,
                    totalMoves      = uciMoves.size,
                    openingSummary  = if (openingEntry != null) "${openingEntry.eco} · ${openingEntry.name}" else "",
                    phaseSummary    = buildPhaseSummary(game),
                    criticalMoments = storedMoments,
                )
            }

            applyMoveIndex(0)

            // Launch hidden background Truth Map analysis
            launchBackgroundAnalysis(storedMoments)
        }
    }

    /**
     * Builds the FEN sequence (index 0 = start, index n = after move n) and the
     * parallel SAN list. Both are pre-computed once so navigation is O(1).
     */
    private fun buildFenAndSanSequence() {
        val fens = mutableListOf<String>(START_FEN)
        val sans = mutableListOf<String>()
        val board = Board()
        board.loadFromFen(START_FEN)

        for (uci in uciMoves) {
            val san = runCatching { ChessUtils.uciToSan(board, uci) }.getOrDefault(uci)
            val move = uciToMove(board, uci) ?: break
            board.doMove(move)
            sans.add(san)
            fens.add(board.fen)
        }
        fenSequence = fens
        sanMoves    = sans
    }

    /** Applies the position at [index] to the board and loads its annotation. */
    private fun applyMoveIndex(index: Int) {
        val fen        = fenSequence.getOrElse(index) { START_FEN }
        val lastMove   = if (index > 0) uciToMoveFromFens(index) else null
        val annotation = getCachedAnnotation(fen)
        val arrows     = annotation?.arrowsJson?.let { parseArrows(it) }  ?: emptyList()
        val marks      = annotation?.markedSquaresJson?.let { parseMarks(it) } ?: emptyList()
        val comment    = annotation?.moveComment ?: ""

        _uiState.update { state ->
            state.copy(
                moveIndex = index,
                boardState = state.boardState.copy(
                    fen            = fen,
                    lastMove       = lastMove,
                    selectedSquare = null,
                    legalMoves     = emptyList(),
                    userArrows     = arrows,
                    markedSquares  = marks,
                    isEditorMode   = true,
                ),
                currentComment         = comment,
                hasAnnotationAtCurrent = comment.isNotBlank() || arrows.isNotEmpty() || marks.isNotEmpty(),
                treeItems              = buildTreeItems(index),
                blunderGuardActive     = false,
            )
        }
    }

    /** Rebuilds the tree display list whenever the current index or annotations change. */
    private fun buildTreeItems(currentIndex: Int): List<TreeDisplayItem> {
        val items = mutableListOf<TreeDisplayItem>()
        var moveNumber = 1
        sanMoves.forEachIndexed { idx, san ->
            val moveIdx  = idx + 1          // move index is 1-based
            val isWhite  = idx % 2 == 0
            val fen      = fenSequence.getOrElse(moveIdx) { START_FEN }
            val annot    = getCachedAnnotation(fen)
            val comment  = annot?.moveComment ?: ""
            val hasAnnot = comment.isNotBlank() ||
                    (annot?.arrowsJson?.length ?: 0) > 2 ||
                    (annot?.markedSquaresJson?.length ?: 0) > 2

            items.add(
                TreeDisplayItem.MoveItem(
                    nodeId          = moveIdx.toLong(),
                    san             = san,
                    fen             = fen,
                    comment         = comment,
                    hasAnnotations  = hasAnnot,
                    isCurrentMove   = moveIdx == currentIndex,
                    depth           = 0,
                    moveNumber      = moveNumber,
                    isWhiteMove     = isWhite,
                    showMoveNumber  = isWhite,
                )
            )
            if (!isWhite) moveNumber++
        }
        return items
    }

    /** Rebuilds tree items and updates state (used after annotation saves). */
    private fun refreshTreeItems() {
        val idx = _uiState.value.moveIndex
        _uiState.update { it.copy(treeItems = buildTreeItems(idx)) }
    }

    // ── Background Truth Map analysis ─────────────────────────────────────────

    private fun launchBackgroundAnalysis(storedMoments: List<CriticalMoment>) {
        // Skip if already analyzed for this game
        if (storedMoments.any { it.type == CriticalMoment.Type.ENGINE_MARKED.name }) return

        viewModelScope.launch(Dispatchers.Default) {
            val map = truthMapBuilder.build(uciMoves) { processed, total ->
                _uiState.update {
                    it.copy(backgroundAnalysisProgress = processed.toFloat() / total)
                }
            }
            truthMap = map

            // Persist engine-marked critical moments to DB
            val moments = map
                .filter { it.isCritical || it.hasTacticalMotif }
                .map { entry ->
                    CriticalMoment(
                        gameId           = gameId,
                        moveIndex        = entry.moveIndex,
                        type             = CriticalMoment.Type.ENGINE_MARKED.name,
                        severity         = Math.abs(entry.playerEvalDelta),
                        reasonCategory   = motifToReasonCategory(entry.motif),
                        explanationState = CriticalMoment.ExplanationState.HIDDEN.name,
                        fen              = entry.fen,
                    )
                }
            if (moments.isNotEmpty()) {
                criticalMomentDao.insertAll(moments)
            }

            _uiState.update {
                it.copy(
                    isBackgroundAnalysisDone  = true,
                    backgroundAnalysisProgress = 1f,
                    criticalMoments            = criticalMomentDao.getByGameId(gameId),
                )
            }
        }
    }

    // ── Missed Moment detection ───────────────────────────────────────────────

    private fun checkMissedMoments(fromIndex: Int, toIndex: Int) {
        if (truthMap.isEmpty()) return // analysis not done yet

        for (idx in fromIndex until toIndex) {
            val entry = truthMap.getOrNull(idx - 1) ?: continue   // idx is 1-based in truthMap
            if (!entry.isCritical && !entry.hasTacticalMotif) continue

            val fen    = fenSequence.getOrElse(idx) { continue }
            val annot  = getCachedAnnotation(fen)
            val isAnnotated = (annot?.moveComment?.isNotBlank() == true) ||
                    (annot?.arrowsJson?.length ?: 0) > 2

            if (!isAnnotated) {
                _uiState.update {
                    it.copy(
                        showMissedMomentBanner = true,
                        missedMomentMoveIndex  = idx,
                    )
                }
                return // show one at a time
            }
        }
    }

    // ── Sandbox: execute a player move + engine response ──────────────────────

    private fun executeSandboxMove(board: Board, move: Move) {
        board.doMove(move)
        val newFen   = board.fen
        val lastMove = move

        val entryBefore = truthMap.find { it.fen == _uiState.value.boardState.fen }
        val entryAfter  = truthMap.find { it.fen == newFen }

        // Blunder Guard: if the move loses more than BLUNDER_THRESHOLD_CP from player's perspective
        val cpLoss = if (entryBefore != null && entryAfter != null) {
            val playerWasWhite = board.sideToMove == Side.BLACK // board already flipped
            val evalBefore = if (playerWasWhite) entryBefore.evalCp else -entryBefore.evalCp
            val evalAfter  = if (playerWasWhite) entryAfter.evalCp  else -entryAfter.evalCp
            evalBefore - evalAfter  // positive = lost eval
        } else 0

        val isBlunder = cpLoss >= ChessConstants.BLUNDER_THRESHOLD_CP

        _uiState.update {
            it.copy(
                boardState = it.boardState.copy(
                    fen        = newFen,
                    lastMove   = lastMove,
                    selectedSquare = null,
                    legalMoves = emptyList(),
                    showingFlash = isBlunder,
                ),
                blunderGuardActive  = isBlunder,
                blunderGuardMessage = if (isBlunder)
                    "This move loses ≥ ${ChessConstants.BLUNDER_THRESHOLD_CP / 100} pawns. " +
                    "Take a moment to reconsider your reasoning."
                else "",
            )
        }

        if (!isBlunder) {
            // Engine plays a response after a brief delay
            triggerEngineReply(newFen)
        }
    }

    private fun triggerEngineReply(fen: String) {
        _uiState.update { it.copy(sandboxEngineThinking = true) }
        viewModelScope.launch(Dispatchers.Default) {
            delay(ChessConstants.OPPONENT_REPLY_DELAY_MS)
            val result = runCatching {
                engine.analyzePosition(fen, depth = 15)
            }.getOrNull()

            val bestUci = result?.bestMoveUci
            if (bestUci != null) {
                val board = Board()
                board.loadFromFen(fen)
                val move = uciToMove(board, bestUci)
                if (move != null) {
                    board.doMove(move)
                    _uiState.update {
                        it.copy(
                            boardState = it.boardState.copy(
                                fen      = board.fen,
                                lastMove = move,
                            ),
                            sandboxEngineThinking = false,
                        )
                    }
                    return@launch
                }
            }
            _uiState.update { it.copy(sandboxEngineThinking = false) }
        }
    }

    // ── Annotation persistence ────────────────────────────────────────────────

    private fun persistAnnotation(boardState: BoardState) {
        viewModelScope.launch(Dispatchers.IO) {
            val fen      = boardState.fen
            val existing = getCachedAnnotation(fen)
            val updated  = (existing ?: PositionAnnotation(fen = fen)).copy(
                arrowsJson        = gson.toJson(boardState.userArrows),
                markedSquaresJson = gson.toJson(boardState.markedSquares),
            )
            annotationDao.upsert(updated)
            annotationCache[fen] = updated
            val hasAnnot = updated.moveComment.isNotBlank() ||
                    updated.arrowsJson.length > 2 ||
                    updated.markedSquaresJson.length > 2
            _uiState.update { it.copy(hasAnnotationAtCurrent = hasAnnot) }
            refreshTreeItems()
        }
    }

    private fun getCachedAnnotation(fen: String): PositionAnnotation? {
        return if (annotationCache.containsKey(fen)) {
            annotationCache[fen]
        } else {
            // Blocking load within IO context — acceptable since called from IO coroutines
            val result = kotlinx.coroutines.runBlocking { annotationDao.getByFen(fen) }
            annotationCache[fen] = result
            result
        }
    }

    // ── Parsing helpers ───────────────────────────────────────────────────────

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
                val prom = when (uci[4].lowercaseChar()) {
                    'r' -> if (board.sideToMove == Side.WHITE) Piece.WHITE_ROOK   else Piece.BLACK_ROOK
                    'b' -> if (board.sideToMove == Side.WHITE) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
                    'n' -> if (board.sideToMove == Side.WHITE) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
                    else -> if (board.sideToMove == Side.WHITE) Piece.WHITE_QUEEN  else Piece.BLACK_QUEEN
                }
                Move(from, to, prom)
            } else Move(from, to)
        }.getOrNull()
    }

    /** Reconstructs the Move object from the FEN sequence (used for lastMove highlighting). */
    private fun uciToMoveFromFens(index: Int): Move? {
        if (index < 1 || index > uciMoves.size) return null
        val uci = uciMoves.getOrNull(index - 1) ?: return null
        // We need a board at the position BEFORE the move
        val board = Board()
        board.loadFromFen(fenSequence.getOrElse(index - 1) { START_FEN })
        return uciToMove(board, uci)
    }

    // ── Summary helpers ───────────────────────────────────────────────────────

    private fun buildPhaseSummary(game: ReviewGame): String {
        val total = uciMoves.size
        return when {
            total <= 10  -> "Short game · ${total / 2} moves each side"
            total <= 30  -> "Middlegame battle · ${total} half-moves"
            else         -> "Full game · ${total / 2} moves"
        }
    }

    private fun motifToReasonCategory(motif: String): String = when (motif) {
        "checkmate" -> CriticalMoment.ReasonCategory.MISSED_WIN.name
        "hanging"   -> CriticalMoment.ReasonCategory.HANGING_PIECE.name
        "fork"      -> CriticalMoment.ReasonCategory.MISSED_TACTIC.name
        else        -> CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE.name
    }
}
