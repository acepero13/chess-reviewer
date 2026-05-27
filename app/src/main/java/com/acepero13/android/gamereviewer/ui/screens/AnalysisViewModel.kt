package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.db.CriticalMomentDao
import com.acepero13.android.gamereviewer.data.db.GameEvaluationDao
import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.domain.InsightReconciler
import com.acepero13.android.gamereviewer.domain.extractUciMovesFromFullPgn
import com.acepero13.android.gamereviewer.domain.TruthMapBuilder
import com.acepero13.android.gamereviewer.domain.TruthMapEntry
import com.acepero13.chess.core.data.db.PositionAnnotationDao
import com.acepero13.chess.core.data.model.ChessConstants
import com.acepero13.chess.core.data.model.PositionAnnotation
import com.acepero13.chess.core.engine.EngineResult
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
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "AnalysisVM"

// ── Mode enums ────────────────────────────────────────────────────────────────

enum class ReviewMode { NAVIGATE, ANALYSE, MENTOR }
enum class AnalyseSubMode { VIEW, EDIT, EXPLORE }

// ── State ─────────────────────────────────────────────────────────────────────

data class AnalysisUiState(
    val game: ReviewGame? = null,
    val moveIndex: Int = 0,              // 0 = before first move (starting position)
    val totalMoves: Int = 0,
    val boardState: BoardState = BoardState(fen = START_FEN),

    // Move tree
    val treeItems: List<TreeDisplayItem> = emptyList(),

    // Non-judgmental opening / game summary (no accuracy scores)
    val openingSummary: String = "",
    val phaseSummary: String = "",

    // Background analysis (entirely hidden from the user)
    val backgroundAnalysisProgress: Float = 0f,   // 0..1
    val isBackgroundAnalysisDone: Boolean = false,

    // Annotation for the current position
    val currentComment: String = "",
    val hasAnnotationAtCurrent: Boolean = false,

    // ── Missed Moment (Task 3.2) ─────────────────────────────────────────────
    val showMissedMomentBanner: Boolean = false,
    val missedMomentMoveIndex: Int? = null,

    // ── Guided Discovery (Task 3.3) ──────────────────────────────────────────
    /** True while the guided discovery panel is open; navigation is frozen. */
    val guidedDiscoveryMode: Boolean = false,
    val guidedDiscoveryInsight: InsightReconciler.Insight? = null,
    val guidedDiscoveryCriticalMoment: CriticalMoment? = null,
    /** User's free-text thoughts written inside the guided panel. */
    val guidedDiscoveryThoughts: String = "",
    /** True once the user requested the conceptual hint (HINTED state). */
    val guidedDiscoveryHintVisible: Boolean = false,
    /** True once the user requested the engine answer (REVEALED state).
     *  The best-move arrow appears in boardState.arrows. */
    val guidedDiscoveryAnswerRevealed: Boolean = false,
    val guidedDiscoveryRevealedEvalCp: Int? = null,
    val guidedDiscoveryEngineThinking: Boolean = false,

    // ── Critical-moment bottom-sheet (Milestone 2 questionnaire) ────────────
    val showCriticalSheet: Boolean = false,

    // ── Sandbox mode (Milestone 2) ───────────────────────────────────────────
    val sandboxMode: Boolean = false,
    val sandboxEngineThinking: Boolean = false,

    // ── Blunder Guard (Task 3.1) ─────────────────────────────────────────────
    /** True when the board border should flash (bad move detected in sandbox). */
    val blunderGuardActive: Boolean = false,
    /** True when the reflection panel is shown and further sandbox play is blocked. */
    val blunderReflectionMode: Boolean = false,
    val blunderReflectionInsight: InsightReconciler.Insight? = null,
    /** FEN to restore when the user presses "Try Again" after a blunder. */
    val blunderPreMoveFen: String = "",
    val blunderCpLoss: Int = 0,

    // ── Critical moments for this game ──────────────────────────────────────
    val criticalMoments: List<CriticalMoment> = emptyList(),

    // ── Mode system ──────────────────────────────────────────────────────────
    val reviewMode: ReviewMode = ReviewMode.NAVIGATE,
    val analyseSubMode: AnalyseSubMode = AnalyseSubMode.VIEW,
    /** Stored before entering MENTOR so exitMentorMode can return to the right place. */
    val previousReviewMode: ReviewMode = ReviewMode.NAVIGATE,
    /** True when the Mentor button should be enabled (ENGINE_MARKED moment at current index). */
    val mentorAvailable: Boolean = false,

    // ── Analyse mode engine toggles ──────────────────────────────────────────
    val evalBarVisible: Boolean = false,
    val bestMoveVisible: Boolean = false,
    /** Current centipawn eval (White-perspective) sourced from truth map; feeds EvalBar. */
    val currentEvalCp: Int? = null,

    // ── Edit sub-mode annotation color (shared by arrows and square marks) ──
    val currentArrowColor: Color = Color(0xCCF0A500.toInt()),   // ChessGold default
)

private const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

// ── ViewModel ─────────────────────────────────────────────────────────────────

class AnalysisViewModel(
    private val gameId: Long,
    private val repo: GameRepository,
    private val annotationDao: PositionAnnotationDao,
    private val criticalMomentDao: CriticalMomentDao,
    private val gameEvaluationDao: GameEvaluationDao,
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

    /** The hidden Truth Map — never exposed in UiState directly. */
    private var truthMap: List<TruthMapEntry> = emptyList()

    /** Running eval of the current sandbox position (White-perspective cp).
     *  Updated from the truth map or from on-demand engine analysis. */
    private var sandboxEvalCp: Int? = null

    /** Annotation cache keyed by FEN to reduce DB round-trips. */
    private val annotationCache = mutableMapOf<String, PositionAnnotation?>()

    private val gson = Gson()

    init { loadGame() }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC — Navigation
    // ═══════════════════════════════════════════════════════════════════════════

    /** Navigate to position [index]. Ignored when Mentor mode is active (navigation frozen). */
    fun goToMove(index: Int) {
        Log.d(TAG, "goToMove($index) — reviewMode=${_uiState.value.reviewMode} uciMoves.size=${uciMoves.size} fenSequence.size=${fenSequence.size}")
        if (_uiState.value.reviewMode == ReviewMode.MENTOR) {
            Log.d(TAG, "goToMove: BLOCKED (mentor/guided discovery active)")
            return
        }
        // Diagnostic: if uciMoves is still empty, log the raw DB fields so we can see why
        if (uciMoves.isEmpty()) {
            val game = _uiState.value.game
            Log.e(TAG, "goToMove: uciMoves empty! game=${game?.let { "'${it.whitePlayer} vs ${it.blackPlayer}' movesUci.length=${it.movesUci.length} movesUci_preview='${it.movesUci.take(60)}' pgn.length=${it.pgn.length} pgn_preview='${it.pgn.take(120)}'" } ?: "null"}")
        }
        val clamped = index.coerceIn(0, uciMoves.size)
        Log.d(TAG, "goToMove: clamped=$clamped  currentMoveIndex=${_uiState.value.moveIndex}")
        val prev    = _uiState.value.moveIndex
        if (clamped > prev) checkMissedMoments(fromIndex = prev, toIndex = clamped)
        viewModelScope.launch(Dispatchers.Default) { applyMoveIndex(clamped) }
    }

    fun stepForward()  { Log.d(TAG, "stepForward  moveIndex=${_uiState.value.moveIndex}"); goToMove(_uiState.value.moveIndex + 1) }
    fun stepBackward() { Log.d(TAG, "stepBackward moveIndex=${_uiState.value.moveIndex}"); goToMove(_uiState.value.moveIndex - 1) }
    fun goToStart()    { Log.d(TAG, "goToStart");  goToMove(0) }
    fun goToEnd()      { Log.d(TAG, "goToEnd");    goToMove(uciMoves.size) }

    /** Called when the user taps a move chip in the MoveTree. */
    fun onMoveNodeClick(nodeId: Long) = goToMove(nodeId.toInt())

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC — Annotation
    // ═══════════════════════════════════════════════════════════════════════════

    fun onArrowDrawn(from: Square, to: Square) {
        val cur   = _uiState.value.boardState
        val color = _uiState.value.currentArrowColor
        val arrow = Arrow(from, to, color)
        val upd   = if (cur.userArrows.any { it.from == from && it.to == to })
            cur.copy(userArrows = cur.userArrows.filter { !(it.from == from && it.to == to) })
        else
            cur.copy(userArrows = cur.userArrows + arrow)
        _uiState.update { it.copy(boardState = upd) }
        persistAnnotation(upd)
    }

    fun onSquareMarked(square: Square) {
        val cur   = _uiState.value.boardState
        // Derive a semi-transparent mark color from the arrow color (same hue, alpha ~0x88)
        val color = _uiState.value.currentArrowColor.copy(alpha = 0x88 / 255f)
        val upd   = if (cur.markedSquares.any { it.square == square })
            cur.copy(markedSquares = cur.markedSquares.filter { it.square != square })
        else
            cur.copy(markedSquares = cur.markedSquares + MarkedSquare(square, color))
        _uiState.update { it.copy(boardState = upd) }
        persistAnnotation(upd)
    }

    fun updateMoveComment(comment: String) {
        _uiState.update { it.copy(currentComment = comment) }
        viewModelScope.launch(Dispatchers.IO) {
            val fen = _uiState.value.boardState.fen
            val upd = (getCachedAnnotation(fen) ?: PositionAnnotation(fen = fen))
                .copy(moveComment = comment)
            annotationDao.upsert(upd)
            annotationCache[fen] = upd
            refreshTreeItems()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC — Critical-moment questionnaire (Milestone 2)
    // ═══════════════════════════════════════════════════════════════════════════

    fun markCurrentAsCritical() {
        _uiState.update { it.copy(showCriticalSheet = true) }
    }

    fun dismissCriticalSheet() {
        _uiState.update { it.copy(showCriticalSheet = false) }
    }

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
            val upd = (getCachedAnnotation(fen) ?: PositionAnnotation(fen = fen))
                .copy(moveComment = comment)
            annotationDao.upsert(upd)
            annotationCache[fen] = upd

            criticalMomentDao.insert(
                CriticalMoment(
                    gameId           = gameId,
                    moveIndex        = idx,
                    type             = CriticalMoment.Type.USER_MARKED.name,
                    severity         = 0,
                    reasonCategory   = CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE.name,
                    explanationState = CriticalMoment.ExplanationState.HIDDEN.name,
                    fen              = fen,
                )
            )
            _uiState.update {
                it.copy(
                    showCriticalSheet      = false,
                    currentComment         = comment,
                    hasAnnotationAtCurrent = true,
                )
            }
            refreshTreeItems()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC — Missed Moment banner (Task 3.2)
    // ═══════════════════════════════════════════════════════════════════════════

    fun dismissMissedMomentBanner() {
        _uiState.update { it.copy(showMissedMomentBanner = false, missedMomentMoveIndex = null) }
    }

    /**
     * Navigates to the missed critical position and activates Mentor mode
     * (Task 3.3) so the user cannot simply scroll past it again.
     */
    fun reviewMissedMoment() {
        val idx = _uiState.value.missedMomentMoveIndex ?: return
        dismissMissedMomentBanner()
        enterMentorMode(targetMoveIndex = idx)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC — Guided Discovery (Task 3.3)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Navigates to [moveIndex], freezes navigation, and opens the Guided Discovery panel.
     * Chooses questions based on the [CriticalMoment.ReasonCategory] for this move.
     */
    fun enterGuidedDiscovery(moveIndex: Int) {
        val moment  = _uiState.value.criticalMoments
            .firstOrNull { it.moveIndex == moveIndex && it.type == CriticalMoment.Type.ENGINE_MARKED.name }
        val reason  = moment?.toReason() ?: CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE
        val insight = InsightReconciler.forReason(reason)
        val clamped = moveIndex.coerceIn(0, uciMoves.size)

        viewModelScope.launch(Dispatchers.Default) {
            applyMoveIndex(clamped)
            _uiState.update {
                it.copy(
                    guidedDiscoveryMode           = true,
                    guidedDiscoveryInsight        = insight,
                    guidedDiscoveryCriticalMoment = moment,
                    guidedDiscoveryThoughts       = "",
                    guidedDiscoveryHintVisible    = false,
                    guidedDiscoveryAnswerRevealed = false,
                    guidedDiscoveryRevealedEvalCp = null,
                    guidedDiscoveryEngineThinking = false,
                    boardState                    = it.boardState.copy(
                        isEditorMode = false,
                        arrows       = emptyList(),
                    ),
                    showMissedMomentBanner = false,
                )
            }
        }
    }

    /** Exits guided discovery and restores normal navigation. */
    fun exitGuidedDiscovery() {
        val idx = _uiState.value.moveIndex
        _uiState.update {
            it.copy(
                guidedDiscoveryMode           = false,
                guidedDiscoveryInsight        = null,
                guidedDiscoveryCriticalMoment = null,
                guidedDiscoveryAnswerRevealed = false,
                guidedDiscoveryHintVisible    = false,
                guidedDiscoveryRevealedEvalCp = null,
            )
        }
        // Re-apply with editor mode re-enabled; must run off the main thread
        viewModelScope.launch(Dispatchers.Default) { applyMoveIndex(idx) }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC — New mode system (Navigate / Analyse / Mentor)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Switches to Analyse mode (VIEW sub-mode). Editor mode off until user taps Edit. */
    fun enterAnalyseMode() {
        _uiState.update {
            it.copy(
                reviewMode     = ReviewMode.ANALYSE,
                analyseSubMode = AnalyseSubMode.VIEW,
                boardState     = it.boardState.copy(isEditorMode = false),
            )
        }
    }

    /** Generic mode setter — used by the "← Back" button to return to Navigate. */
    fun setReviewMode(mode: ReviewMode) {
        val needsEditorOff = mode != ReviewMode.ANALYSE ||
            _uiState.value.analyseSubMode != AnalyseSubMode.EDIT
        _uiState.update {
            it.copy(
                reviewMode = mode,
                boardState = it.boardState.copy(
                    isEditorMode = if (needsEditorOff) false else it.boardState.isEditorMode,
                ),
            )
        }
    }

    /**
     * Switches between VIEW / EDIT / EXPLORE sub-modes inside Analyse.
     * Entering EDIT activates [BoardState.isEditorMode]; leaving it deactivates.
     * Entering EXPLORE is handled by [enterSandboxMode] instead.
     */
    fun setAnalyseSubMode(sub: AnalyseSubMode) {
        if (sub == AnalyseSubMode.EXPLORE) { enterSandboxMode(); return }
        _uiState.update {
            it.copy(
                analyseSubMode = sub,
                boardState     = it.boardState.copy(isEditorMode = sub == AnalyseSubMode.EDIT),
            )
        }
    }

    /**
     * Stores the current review mode, then opens Mentor mode at [targetMoveIndex]
     * (defaults to the current move index). Navigation is frozen while in MENTOR.
     * All state is set atomically inside the coroutine to avoid race conditions.
     */
    fun enterMentorMode(targetMoveIndex: Int = _uiState.value.moveIndex) {
        val from    = _uiState.value.reviewMode
        val moment  = _uiState.value.criticalMoments
            .firstOrNull { it.moveIndex == targetMoveIndex && it.type == CriticalMoment.Type.ENGINE_MARKED.name }
        val reason  = moment?.toReason() ?: CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE
        val insight = InsightReconciler.forReason(reason)
        val clamped = targetMoveIndex.coerceIn(0, uciMoves.size)

        viewModelScope.launch(Dispatchers.Default) {
            applyMoveIndex(clamped)
            _uiState.update {
                it.copy(
                    reviewMode                    = ReviewMode.MENTOR,
                    previousReviewMode            = from,
                    guidedDiscoveryMode           = true,
                    guidedDiscoveryInsight        = insight,
                    guidedDiscoveryCriticalMoment = moment,
                    guidedDiscoveryThoughts       = "",
                    guidedDiscoveryHintVisible    = false,
                    guidedDiscoveryAnswerRevealed = false,
                    guidedDiscoveryRevealedEvalCp = null,
                    guidedDiscoveryEngineThinking = false,
                    boardState                    = it.boardState.copy(
                        isEditorMode = false,
                        arrows       = emptyList(),
                    ),
                    showMissedMomentBanner = false,
                )
            }
        }
    }

    /** Exits Mentor mode and returns to [previousReviewMode]. All state reset atomically. */
    fun exitMentorMode() {
        val returnTo = _uiState.value.previousReviewMode
        val idx      = _uiState.value.moveIndex
        _uiState.update {
            it.copy(
                reviewMode                    = returnTo,
                guidedDiscoveryMode           = false,
                guidedDiscoveryInsight        = null,
                guidedDiscoveryCriticalMoment = null,
                guidedDiscoveryAnswerRevealed = false,
                guidedDiscoveryHintVisible    = false,
                guidedDiscoveryRevealedEvalCp = null,
            )
        }
        viewModelScope.launch(Dispatchers.Default) { applyMoveIndex(idx) }
    }

    /** Toggles the evaluation bar visibility. Reads eval from the truth map. */
    fun toggleEvalBar() {
        _uiState.update { it.copy(evalBarVisible = !it.evalBarVisible) }
    }

    /**
     * Toggles best-move arrow visibility. When turning on, fires a quick engine analysis
     * to fetch the best move UCI and draws it as an arrow on the board.
     */
    fun toggleBestMove() {
        val newVisible = !_uiState.value.bestMoveVisible
        _uiState.update { it.copy(bestMoveVisible = newVisible) }
        if (newVisible) {
            val fen = _uiState.value.boardState.fen
            viewModelScope.launch(Dispatchers.Default) {
                val result = runCatching { engine.analyzePosition(fen, depth = 10) }.getOrNull()
                val arrow  = result?.toArrow()
                _uiState.update { s ->
                    s.copy(
                        boardState = s.boardState.copy(
                            arrows = if (arrow != null) listOf(arrow) else emptyList(),
                        ),
                    )
                }
            }
        } else {
            _uiState.update { it.copy(boardState = it.boardState.copy(arrows = emptyList())) }
        }
    }

    /** Sets the annotation color used for arrows and square marks in Edit sub-mode.
     *  Arrows use the full color; square marks automatically get a softer alpha (0x88). */
    fun setArrowColor(color: Color) {
        _uiState.update { it.copy(currentArrowColor = color) }
    }

    fun updateGuidedThoughts(text: String) {
        _uiState.update { it.copy(guidedDiscoveryThoughts = text) }
    }

    /**
     * Shows the conceptual hint (no engine move revealed).
     * Updates the [CriticalMoment] in the DB to [CriticalMoment.ExplanationState.HINTED].
     */
    fun revealGuidedHint() {
        _uiState.update { it.copy(guidedDiscoveryHintVisible = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val moment = _uiState.value.guidedDiscoveryCriticalMoment ?: return@launch
            criticalMomentDao.update(
                moment.copy(explanationState = CriticalMoment.ExplanationState.HINTED.name)
            )
        }
    }

    /**
     * Runs engine analysis on the current position and shows the best-move arrow on the board.
     * Updates the [CriticalMoment] to [CriticalMoment.ExplanationState.REVEALED].
     * Saves the user's thoughts as a position annotation.
     */
    fun revealGuidedAnswer() {
        val fen      = _uiState.value.boardState.fen
        val thoughts = _uiState.value.guidedDiscoveryThoughts
        _uiState.update { it.copy(guidedDiscoveryEngineThinking = true) }

        viewModelScope.launch(Dispatchers.Default) {
            val result = runCatching {
                engine.analyzePosition(fen, depth = ChessConstants.DEFAULT_ANALYSIS_DEPTH)
            }.getOrNull()

            val engineArrow = result?.toArrow()
            val evalCp      = result?.let { r ->
                val b = Board()
                b.loadFromFen(fen)
                r.toWhitePerspective(b)
            }

            // Persist the user's thoughts + mark as REVEALED in DB
            if (thoughts.isNotBlank()) {
                val header = "### Guided Discovery Notes\n"
                launch(Dispatchers.IO) {
                    val existing = getCachedAnnotation(fen)
                    val newComment = buildString {
                        val prev = existing?.moveComment?.takeIf { it.isNotBlank() }
                        if (prev != null) { appendLine(prev); appendLine() }
                        append(header)
                        appendLine(thoughts)
                    }.trim()
                    val upd = (existing ?: PositionAnnotation(fen = fen))
                        .copy(moveComment = newComment)
                    annotationDao.upsert(upd)
                    annotationCache[fen] = upd
                    _uiState.update { it.copy(currentComment = newComment) }
                    refreshTreeItems()
                }
            }

            val moment = _uiState.value.guidedDiscoveryCriticalMoment
            if (moment != null) {
                launch(Dispatchers.IO) {
                    criticalMomentDao.update(
                        moment.copy(explanationState = CriticalMoment.ExplanationState.REVEALED.name)
                    )
                }
            }

            _uiState.update { state ->
                state.copy(
                    guidedDiscoveryEngineThinking = false,
                    guidedDiscoveryAnswerRevealed = true,
                    guidedDiscoveryRevealedEvalCp = evalCp,
                    boardState = state.boardState.copy(
                        arrows = if (engineArrow != null) listOf(engineArrow) else emptyList(),
                    ),
                )
            }
        }
    }

    /**
     * Saves the user's thoughts and exits guided discovery mode.
     * The position is now marked as reviewed.
     */
    fun submitGuidedThoughts() {
        val thoughts = _uiState.value.guidedDiscoveryThoughts
        val fen      = _uiState.value.boardState.fen

        if (thoughts.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                val header  = "### Guided Discovery Notes\n"
                val existing = getCachedAnnotation(fen)
                val prev    = existing?.moveComment?.takeIf { it.isNotBlank() }
                val newComment = buildString {
                    if (prev != null) { appendLine(prev); appendLine() }
                    append(header)
                    appendLine(thoughts)
                }.trim()
                val upd = (existing ?: PositionAnnotation(fen = fen))
                    .copy(moveComment = newComment)
                annotationDao.upsert(upd)
                annotationCache[fen] = upd
                _uiState.update { it.copy(currentComment = newComment) }
                refreshTreeItems()
            }
        }
        exitMentorMode()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC — Sandbox (Milestone 2 + enhanced Task 3.1)
    // ═══════════════════════════════════════════════════════════════════════════

    fun enterSandboxMode() {
        val fen = _uiState.value.boardState.fen
        sandboxEvalCp = truthMap.find { it.fen == fen }?.evalCp
        _uiState.update {
            it.copy(
                sandboxMode    = true,
                blunderReflectionMode = false,
                reviewMode     = ReviewMode.ANALYSE,
                analyseSubMode = AnalyseSubMode.EXPLORE,
                boardState     = it.boardState.copy(isEditorMode = true),
            )
        }
    }

    fun exitSandboxMode() {
        val idx = _uiState.value.moveIndex
        _uiState.update {
            it.copy(
                sandboxMode           = false,
                blunderGuardActive    = false,
                blunderReflectionMode = false,
                sandboxEngineThinking = false,
                analyseSubMode        = AnalyseSubMode.VIEW,
            )
        }
        viewModelScope.launch(Dispatchers.Default) { applyMoveIndex(idx) }
    }

    fun onSandboxSquareTap(square: Square) {
        if (_uiState.value.sandboxEngineThinking) return
        if (_uiState.value.blunderReflectionMode)  return  // locked until user acknowledges
        val cur      = _uiState.value.boardState
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
            if (board.legalMoves().contains(move)) {
                attemptSandboxMove(preFen = cur.fen, move = move)
            } else {
                _uiState.update { it.copy(boardState = cur.copy(selectedSquare = null, legalMoves = emptyList())) }
            }
        }
    }

    /** User wants to try a different move after a blunder was detected (restores pre-blunder FEN). */
    fun retryAfterBlunder() {
        val preFen = _uiState.value.blunderPreMoveFen
        if (preFen.isBlank()) { exitSandboxMode(); return }
        _uiState.update {
            it.copy(
                blunderGuardActive    = false,
                blunderReflectionMode = false,
                blunderReflectionInsight = null,
                boardState = it.boardState.copy(
                    fen          = preFen,
                    lastMove     = null,
                    selectedSquare = null,
                    legalMoves   = emptyList(),
                    showingFlash = false,
                ),
            )
        }
    }

    /** User acknowledges the blunder but keeps the position (continues from blunder state). */
    fun continueAfterBlunder() {
        _uiState.update {
            it.copy(
                blunderGuardActive    = false,
                blunderReflectionMode = false,
                blunderReflectionInsight = null,
                boardState = it.boardState.copy(showingFlash = false),
            )
        }
        // Trigger engine reply from the current (blunder) position
        triggerEngineReply(_uiState.value.boardState.fen)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE — Core
    // ═══════════════════════════════════════════════════════════════════════════

    private fun loadGame() {
        viewModelScope.launch(Dispatchers.IO) {
            val game = repo.findById(gameId) ?: run {
                Log.e(TAG, "loadGame: game $gameId NOT FOUND in DB"); return@launch
            }
            Log.d(TAG, "loadGame: found game '${game.whitePlayer} vs ${game.blackPlayer}' movesUci.length=${game.movesUci.length} pgn.length=${game.pgn.length}")

            // If movesUci is blank the game was imported before the PGN-comment-stripping
            // fix landed.  Re-parse on the fly from the stored raw PGN so the user doesn't
            // need to re-import.
            val rawUci = if (game.movesUci.isNotBlank()) {
                Log.d(TAG, "loadGame: using stored movesUci")
                game.movesUci
            } else {
                Log.w(TAG, "loadGame: movesUci is blank — falling back to raw PGN parse")
                extractUciMovesFromFullPgn(game.pgn)
            }
            uciMoves = rawUci.split(' ').filter { it.isNotBlank() }
            Log.d(TAG, "loadGame: uciMoves.size=${uciMoves.size}  first3=${uciMoves.take(3)}")
            buildFenAndSanSequence()
            Log.d(TAG, "loadGame: fenSequence.size=${fenSequence.size}  sanMoves.size=${sanMoves.size}")

            // Pre-warm annotation cache so navigation never needs runBlocking on the main thread.
            for (fen in fenSequence) {
                if (!annotationCache.containsKey(fen)) {
                    annotationCache[fen] = annotationDao.getByFen(fen)
                }
            }

            val storedMoments = criticalMomentDao.getByGameId(gameId)
            val openingEntry  = runCatching { opening.classifyByMoves(uciMoves.take(20)) }.getOrNull()

            _uiState.update {
                it.copy(
                    game            = game,
                    totalMoves      = uciMoves.size,
                    openingSummary  = openingEntry?.let { e -> "${e.eco} · ${e.name}" } ?: "",
                    phaseSummary    = buildPhaseSummary(),
                    criticalMoments = storedMoments,
                )
            }
            applyMoveIndex(0)
            launchBackgroundAnalysis(storedMoments)
        }
    }

    private fun buildFenAndSanSequence() {
        val fens  = mutableListOf(START_FEN)
        val sans  = mutableListOf<String>()
        val board = Board().apply { loadFromFen(START_FEN) }
        for ((idx, uci) in uciMoves.withIndex()) {
            sans.add(runCatching { ChessUtils.uciToSan(board, uci) }.getOrDefault(uci))
            val move = uciToMove(board, uci)
            if (move == null) {
                Log.e(TAG, "buildFenAndSanSequence: uciToMove returned null for '$uci' at index $idx — stopping")
                break
            }
            val applied = board.doMove(move)
            if (!applied) {
                Log.e(TAG, "buildFenAndSanSequence: board.doMove($uci) returned FALSE at index $idx — stopping")
                break
            }
            fens.add(board.fen)
        }
        Log.d(TAG, "buildFenAndSanSequence: built ${fens.size} FENs for ${uciMoves.size} moves")
        fenSequence = fens
        sanMoves    = sans
    }

    private fun applyMoveIndex(index: Int) {
        val fen = fenSequence.getOrElse(index) { START_FEN }
        Log.d(TAG, "applyMoveIndex($index): fenSequence.size=${fenSequence.size}  fen=${fen.take(40)}")
        val lastMove   = if (index > 0) uciToMoveFromFens(index) else null
        val annot      = getCachedAnnotation(fen)
        val arrows     = annot?.arrowsJson?.let { parseArrows(it) }  ?: emptyList()
        val marks      = annot?.markedSquaresJson?.let { parseMarks(it) } ?: emptyList()
        val comment    = annot?.moveComment ?: ""
        // Editor mode active only in Analyse > EDIT sub-mode (read live state to avoid races)
        val live       = _uiState.value
        val inEditMode = live.reviewMode == ReviewMode.ANALYSE && live.analyseSubMode == AnalyseSubMode.EDIT
        // Truth-map eval for this position (White-perspective centipawns)
        val evalCp     = truthMap.find { it.moveIndex == index }?.evalCp

        _uiState.update { s ->
            s.copy(
                moveIndex  = index,
                boardState = s.boardState.copy(
                    fen            = fen,
                    lastMove       = lastMove,
                    selectedSquare = null,
                    legalMoves     = emptyList(),
                    userArrows     = arrows,
                    markedSquares  = marks,
                    arrows         = emptyList(),
                    isEditorMode   = inEditMode,
                    showingFlash   = false,
                ),
                currentComment         = comment,
                hasAnnotationAtCurrent = comment.isNotBlank() || arrows.isNotEmpty() || marks.isNotEmpty(),
                treeItems              = buildTreeItems(index),
                blunderGuardActive     = false,
                mentorAvailable        = computeMentorAvailable(index),
                currentEvalCp          = evalCp,
            )
        }
        Log.d(TAG, "applyMoveIndex($index): state updated — moveIndex=${_uiState.value.moveIndex} fen=${_uiState.value.boardState.fen.take(40)}")
    }

    /** True when there is an engine-marked critical moment at this move index. */
    private fun computeMentorAvailable(index: Int): Boolean {
        return _uiState.value.criticalMoments.any {
            it.moveIndex == index && it.type == CriticalMoment.Type.ENGINE_MARKED.name
        }
    }

    private fun buildTreeItems(currentIndex: Int): List<TreeDisplayItem> {
        val items  = mutableListOf<TreeDisplayItem>()
        var moveNo = 1
        sanMoves.forEachIndexed { idx, san ->
            val mIdx    = idx + 1
            val isWhite = idx % 2 == 0
            val fen     = fenSequence.getOrElse(mIdx) { START_FEN }
            val annot   = getCachedAnnotation(fen)
            val comment = annot?.moveComment ?: ""
            val hasAnnot = comment.isNotBlank() ||
                (annot?.arrowsJson?.length ?: 0) > 2 ||
                (annot?.markedSquaresJson?.length ?: 0) > 2

            items.add(
                TreeDisplayItem.MoveItem(
                    nodeId         = mIdx.toLong(),
                    san            = san,
                    fen            = fen,
                    comment        = comment,
                    hasAnnotations = hasAnnot,
                    isCurrentMove  = mIdx == currentIndex,
                    depth          = 0,
                    moveNumber     = moveNo,
                    isWhiteMove    = isWhite,
                    showMoveNumber = isWhite,
                )
            )
            if (!isWhite) moveNo++
        }
        return items
    }

    private fun refreshTreeItems() {
        _uiState.update { it.copy(treeItems = buildTreeItems(it.moveIndex)) }
    }

    // ─── Background Truth Map ─────────────────────────────────────────────────

    private fun launchBackgroundAnalysis(stored: List<CriticalMoment>) {
        // If we already have ENGINE_MARKED moments, try to restore the truth map from DB
        // so the Blunder Guard and Missed Moment detector still work in subsequent sessions.
        if (stored.any { it.type == CriticalMoment.Type.ENGINE_MARKED.name }) {
            viewModelScope.launch(Dispatchers.IO) {
                val dbEvals = gameEvaluationDao.getByGameId(gameId)
                if (dbEvals.isNotEmpty()) {
                    // Reconstruct in-memory truth map from persisted evaluations
                    truthMap = dbEvals.map { ev ->
                        TruthMapEntry(
                            moveIndex = ev.moveIndex,
                            fen       = fenSequence.getOrElse(ev.moveIndex) { "" },
                            evalCp    = ev.evalCp,
                            evalDelta = ev.evalDelta,
                            motif     = ev.motif,
                        )
                    }
                }
                _uiState.update { it.copy(isBackgroundAnalysisDone = true, backgroundAnalysisProgress = 1f) }
            }
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            val map = truthMapBuilder.build(uciMoves) { processed, total ->
                _uiState.update { it.copy(backgroundAnalysisProgress = processed.toFloat() / total) }
            }
            truthMap = map

            // Persist evaluations for future sessions (Game Report screen)
            val evaluations = map.map { entry ->
                GameEvaluation(
                    gameId    = gameId,
                    moveIndex = entry.moveIndex,
                    evalCp    = entry.evalCp,
                    evalDelta = entry.evalDelta,
                    motif     = entry.motif,
                )
            }
            if (evaluations.isNotEmpty()) {
                launch(Dispatchers.IO) { gameEvaluationDao.insertAll(evaluations) }
            }

            val moments = map.filter { it.isCritical || it.hasTacticalMotif }.map { entry ->
                CriticalMoment(
                    gameId           = gameId,
                    moveIndex        = entry.moveIndex,
                    type             = CriticalMoment.Type.ENGINE_MARKED.name,
                    severity         = kotlin.math.abs(entry.playerEvalDelta),
                    reasonCategory   = motifToReason(entry.motif),
                    explanationState = CriticalMoment.ExplanationState.HIDDEN.name,
                    fen              = entry.fen,
                )
            }
            if (moments.isNotEmpty()) {
                launch(Dispatchers.IO) { criticalMomentDao.insertAll(moments) }
            }

            _uiState.update {
                it.copy(
                    isBackgroundAnalysisDone   = true,
                    backgroundAnalysisProgress = 1f,
                    criticalMoments            = criticalMomentDao.getByGameId(gameId),
                )
            }
        }
    }

    // ─── Missed Moment detection ──────────────────────────────────────────────

    private fun checkMissedMoments(fromIndex: Int, toIndex: Int) {
        if (truthMap.isEmpty() && _uiState.value.criticalMoments.isEmpty()) return

        val criticalIndices = if (truthMap.isNotEmpty()) {
            truthMap.filter { it.isCritical || it.hasTacticalMotif }.map { it.moveIndex }.toSet()
        } else {
            _uiState.value.criticalMoments
                .filter { it.type == CriticalMoment.Type.ENGINE_MARKED.name }
                .map { it.moveIndex }.toSet()
        }

        for (idx in (fromIndex + 1)..toIndex) {
            if (idx !in criticalIndices) continue
            val fen     = fenSequence.getOrElse(idx) { continue }
            val annot   = getCachedAnnotation(fen)
            val reviewed = (annot?.moveComment?.isNotBlank() == true) ||
                (annot?.arrowsJson?.length ?: 0) > 2
            if (!reviewed) {
                _uiState.update {
                    it.copy(showMissedMomentBanner = true, missedMomentMoveIndex = idx)
                }
                return
            }
        }
    }

    // ─── Sandbox: attempt move + async blunder check ──────────────────────────

    /**
     * Applies the move immediately for visual feedback, then runs a quick engine analysis
     * (depth 10) to detect blunders. If the move is a blunder:
     * - The board is left showing the move (so the user sees what they played)
     * - The border flashes red
     * - The blunder reflection panel opens, blocking further play
     * If it is not a blunder, the engine plays a reply after [ChessConstants.OPPONENT_REPLY_DELAY_MS].
     */
    private fun attemptSandboxMove(preFen: String, move: Move) {
        val preBoardForSan = Board().apply { loadFromFen(preFen) }
        val postBoard      = Board().apply { loadFromFen(preFen) }
        postBoard.doMove(move)
        val postFen      = postBoard.fen
        val playerWasWhite = postBoard.sideToMove == Side.BLACK  // after White's move it's Black's turn

        // Optimistically show the move
        _uiState.update {
            it.copy(
                boardState = it.boardState.copy(
                    fen            = postFen,
                    lastMove       = move,
                    selectedSquare = null,
                    legalMoves     = emptyList(),
                    showingFlash   = false,
                ),
                sandboxEngineThinking = true,
                blunderGuardActive    = false,
            )
        }

        viewModelScope.launch(Dispatchers.Default) {
            // ── Baseline eval (White-perspective) ────────────────────────────
            val preEvalWhite = sandboxEvalCp
                ?: run {
                    // Position not in truth map → quick analysis of pre-move position
                    runCatching {
                        engine.analyzePosition(preFen, depth = 10)
                    }.getOrNull()?.toWhitePerspective(Board().apply { loadFromFen(preFen) })
                }

            // ── Post-move eval ───────────────────────────────────────────────
            val postResult    = runCatching { engine.analyzePosition(postFen, depth = 10) }.getOrNull()
            val postEvalWhite = postResult?.toWhitePerspective(postBoard)

            // ── Centipawn loss from the player's perspective ─────────────────
            val cpLoss: Int = if (preEvalWhite != null && postEvalWhite != null) {
                val loss = if (playerWasWhite) preEvalWhite - postEvalWhite
                           else               postEvalWhite - preEvalWhite
                maxOf(0, loss)
            } else 0

            val isBlunder = cpLoss >= ChessConstants.BLUNDER_THRESHOLD_CP
            val motif     = postResult?.let { com.acepero13.chess.core.engine.MotifClassifier.classify(it, postFen) } ?: "mixed"

            if (isBlunder) {
                val insight = InsightReconciler.forBlunder(motif, cpLoss)
                _uiState.update {
                    it.copy(
                        sandboxEngineThinking = false,
                        blunderGuardActive    = true,
                        blunderReflectionMode = true,
                        blunderReflectionInsight = insight,
                        blunderPreMoveFen     = preFen,
                        blunderCpLoss         = cpLoss,
                        boardState = it.boardState.copy(showingFlash = true),
                    )
                }
                delay(ChessConstants.BLIND_INTER_MOVE_FLASH_MS)
                _uiState.update { it.copy(boardState = it.boardState.copy(showingFlash = false)) }
            } else {
                // Update sandbox baseline eval for next blunder check
                sandboxEvalCp = postEvalWhite
                _uiState.update { it.copy(sandboxEngineThinking = false, blunderGuardActive = false) }
                triggerEngineReply(postFen)
            }
        }
    }

    private fun triggerEngineReply(fen: String) {
        _uiState.update { it.copy(sandboxEngineThinking = true) }
        viewModelScope.launch(Dispatchers.Default) {
            delay(ChessConstants.OPPONENT_REPLY_DELAY_MS)
            val result  = runCatching { engine.analyzePosition(fen, depth = 15) }.getOrNull()
            val bestUci = result?.bestMoveUci
            if (bestUci != null) {
                val board = Board().apply { loadFromFen(fen) }
                val move  = uciToMove(board, bestUci)
                if (move != null) {
                    board.doMove(move)
                    val replyFen = board.fen
                    // Update sandbox baseline for next move's blunder check
                    val replyResult = runCatching { engine.analyzePosition(replyFen, depth = 10) }.getOrNull()
                    sandboxEvalCp   = replyResult?.toWhitePerspective(board)
                    _uiState.update {
                        it.copy(
                            boardState = it.boardState.copy(fen = replyFen, lastMove = move),
                            sandboxEngineThinking = false,
                        )
                    }
                    return@launch
                }
            }
            _uiState.update { it.copy(sandboxEngineThinking = false) }
        }
    }

    // ─── Annotation persistence ───────────────────────────────────────────────

    private fun persistAnnotation(boardState: BoardState) {
        viewModelScope.launch(Dispatchers.IO) {
            val fen     = boardState.fen
            val existing = getCachedAnnotation(fen)
            val upd     = (existing ?: PositionAnnotation(fen = fen)).copy(
                arrowsJson        = gson.toJson(boardState.userArrows),
                markedSquaresJson = gson.toJson(boardState.markedSquares),
            )
            annotationDao.upsert(upd)
            annotationCache[fen] = upd
            val hasAnnot = upd.moveComment.isNotBlank() || upd.arrowsJson.length > 2 || upd.markedSquaresJson.length > 2
            _uiState.update { it.copy(hasAnnotationAtCurrent = hasAnnot) }
            refreshTreeItems()
        }
    }

    /**
     * Returns the cached annotation for [fen].
     * All game positions are pre-warmed during [loadGame]; new annotations are inserted
     * into the cache immediately when saved, so this is always an O(1) map lookup —
     * no DB access, no runBlocking, safe to call from the main thread.
     */
    private fun getCachedAnnotation(fen: String): PositionAnnotation? = annotationCache[fen]

    // ─── Parsing ─────────────────────────────────────────────────────────────

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

    private fun uciToMoveFromFens(index: Int): Move? {
        if (index < 1 || index > uciMoves.size) return null
        val uci   = uciMoves.getOrNull(index - 1) ?: return null
        val board = Board().apply { loadFromFen(fenSequence.getOrElse(index - 1) { START_FEN }) }
        return uciToMove(board, uci)
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun buildPhaseSummary(): String {
        val total = uciMoves.size
        return when {
            total <= 10  -> "Short game · ${total / 2} moves each side"
            total <= 30  -> "Middlegame battle · ${total} half-moves"
            else         -> "Full game · ${total / 2} moves"
        }
    }

    private fun motifToReason(motif: String) = when (motif) {
        "checkmate" -> CriticalMoment.ReasonCategory.MISSED_WIN.name
        "hanging"   -> CriticalMoment.ReasonCategory.HANGING_PIECE.name
        "fork"      -> CriticalMoment.ReasonCategory.MISSED_TACTIC.name
        else        -> CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE.name
    }
}

// ─── Extension helpers ────────────────────────────────────────────────────────

private fun EngineResult.toWhitePerspective(board: Board): Int =
    if (board.sideToMove == Side.WHITE) score else -score

private fun EngineResult.toArrow(): Arrow? {
    val uci = bestMoveUci
    if (uci.length < 4) return null
    return runCatching {
        Arrow(
            from  = Square.valueOf(uci.substring(0, 2).uppercase()),
            to    = Square.valueOf(uci.substring(2, 4).uppercase()),
            color = Color(0xCC4CAF50.toInt()),   // green engine arrow
        )
    }.getOrNull()
}
