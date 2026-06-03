package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.db.CriticalMomentDao
import com.acepero13.android.gamereviewer.data.db.GameEvaluationDao
import com.acepero13.android.gamereviewer.data.db.MoveTimeDao
import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.data.repository.SettingsRepository
import com.acepero13.android.gamereviewer.data.repository.TriggerMasteryRepository
import kotlinx.coroutines.flow.first
import com.acepero13.android.gamereviewer.domain.CoachingTrigger
import com.acepero13.android.gamereviewer.domain.CoachingTriggerEvaluator
import com.acepero13.android.gamereviewer.domain.InsightReconciler
import com.acepero13.android.gamereviewer.domain.OpeningDeviation
import com.acepero13.android.gamereviewer.domain.OpeningDeviationAnalyzer
import com.acepero13.android.gamereviewer.domain.extractUciMovesFromFullPgn
import com.acepero13.android.gamereviewer.domain.TruthMapBuilder
import com.acepero13.android.gamereviewer.domain.TruthMapEntry
import com.acepero13.android.gamereviewer.engine.highlights.BoardAttackHelper
import com.acepero13.android.gamereviewer.engine.highlights.GameHighlight
import com.acepero13.android.gamereviewer.engine.highlights.GameHighlightEngine
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
import com.acepero13.chess.core.ui.theme.AnalyzeBlue
import com.acepero13.chess.core.util.ChessUtils
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveGenerator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.util.Log
import com.acepero13.android.gamereviewer.domain.BehavioralDiagnostic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
enum class CoordinationQuizPhase { ASKING, REVEALING }

/** Result of the user's attempted move in Mentor move-input mode. */
enum class MentorMoveResult { CORRECT, CLOSE, INCORRECT }

/**
 * Cross-game coaching context computed when a Mentor Session starts.
 * Null when not enough data exists (fewer than 2 games analyzed).
 */
data class WeaknessContext(
    val trendTitle: String,
    val trendEmoji: String,
    val trendDescription: String,
    val gamesAffected: Int,
    val totalGamesAnalyzed: Int,
    val matchingMoveIndices: List<Int>,
)

/** A single unanswered position shown in end-of-session Board Scan Reflection mode. */
data class ReflectionItem(
    val moveIndex: Int,
    val fen: String,
    /** The trigger type name that fired here — used for keying. */
    val triggerTypeName: String,
    /** Human-readable label for the correct answer (e.g. "Safety Issue"). */
    val correctLabel: String,
    /** User's selection from [CoachingTrigger.ALL_LABELS]. Null until answered. */
    val userAnswer: String? = null,
)

/** A single option in the post-move "Why was this critical?" classification quiz. */
data class ClassificationOption(
    val label:       String,
    val description: String,
    val category:    com.acepero13.android.gamereviewer.data.model.CriticalMoment.ReasonCategory,
)

/** Player's pre-game prediction about what went wrong. */
enum class GamePrediction(
    val emoji:       String,
    val label:       String,
    val description: String,
) {
    SPECIFIC_BLUNDER(
        emoji       = "🎯",
        label       = "I blundered somewhere specific",
        description = "A tactical oversight or material loss",
    ),
    TIME_PRESSURE(
        emoji       = "⏱️",
        label       = "I lost on time / rushed",
        description = "Clock pressure forced inaccurate moves",
    ),
    OUTPLAYED_POSITIONALLY(
        emoji       = "🧭",
        label       = "I got outplayed positionally",
        description = "Gradual squeeze or strategic errors",
    ),
    NOT_SURE(
        emoji       = "🔍",
        label       = "Not sure — let the engine decide",
        description = "Let the analysis reveal what went wrong",
    ),
}

/** Outcome of comparing the player's pre-game prediction to engine findings. */
data class PredictionMatchResult(
    val isAccurate: Boolean,
    val headline:   String,
    val detail:     String? = null,
)

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
    /** Tracks the last moveIndex at which each reasonCategory banner was shown, used to
     *  suppress repeat interventions for the same issue on consecutive moves. */
    val shownCategoryAtMove: Map<String, Int> = emptyMap(),

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

    // ── Forcing sequence explorer ────────────────────────────────────────────
    /** True when the user is in "try the forcing sequence" challenge mode. */
    val forcingSequenceMode: Boolean = false,
    /** True while the engine PV is being animated on the board. */
    val forcingSequenceAnimating: Boolean = false,
    /** True once the animation has finished (user can replay or explore freely). */
    val forcingSequenceComplete: Boolean = false,
    /** Parsed UCI moves of the forcing sequence to animate. */
    val forcingSequencePvMoves: List<String> = emptyList(),
    /** FEN at the start of the forcing sequence (used for replay). */
    val forcingSequenceStartFen: String = "",
    /** Which step of the animation is currently visible (0 = not started). */
    val forcingSequenceCurrentStep: Int = 0,

    // ── Critical moments for this game ──────────────────────────────────────
    val criticalMoments: List<CriticalMoment> = emptyList(),

    // ── Mode system ──────────────────────────────────────────────────────────
    val reviewMode: ReviewMode = ReviewMode.NAVIGATE,
    val analyseSubMode: AnalyseSubMode = AnalyseSubMode.VIEW,
    /** Stored before entering MENTOR so exitMentorMode can return to the right place. */
    val previousReviewMode: ReviewMode = ReviewMode.NAVIGATE,
    /** True when the Mentor button should be enabled (ENGINE_MARKED moment at current index). */
    val mentorAvailable: Boolean = false,

    // ── Mentor move-input mode ────────────────────────────────────────────────
    /** True when the user has activated "Play your answer" in the mentor panel. */
    val mentorMoveInputActive: Boolean = false,
    /** True while the engine is checking the user's move against the best move. */
    val mentorMoveChecking: Boolean = false,
    /** Result of the last move the user played in mentor mode. Null = no attempt yet. */
    val mentorMoveResult: MentorMoveResult? = null,
    /** Human-readable feedback shown after a move check. */
    val mentorMoveFeedback: String = "",

    // ── Analyse mode engine toggles ──────────────────────────────────────────
    val evalBarVisible: Boolean = false,
    val bestMoveVisible: Boolean = false,
    /** Current centipawn eval (White-perspective) sourced from truth map; feeds EvalBar. */
    val currentEvalCp: Int? = null,

    // ── Edit sub-mode annotation color (shared by arrows and square marks) ──
    val currentArrowColor: Color = Color(0xCCF0A500.toInt()),   // ChessGold default

    // ── Game highlights (produced by rule engine after background analysis) ──
    val gameHighlights: List<GameHighlight> = emptyList(),

    // ── Stats sheet ──────────────────────────────────────────────────────────
    /** True when the stats bottom-sheet is open. */
    val showStatsSheet: Boolean = false,
    /** Per-side accuracy/quality stats, null until first sheet open (lazy). */
    val playerStats: Pair<com.acepero13.android.gamereviewer.data.model.PlayerStats, com.acepero13.android.gamereviewer.data.model.PlayerStats>? = null,

    // ── Mentor context label (shown as banner when Mentor navigates the board) ──
    /** Human-readable label shown when Mentor auto-navigates to a decision point.
     *  E.g. "Move 14. — Find the best move for White". Cleared on exitMentorMode. */
    val mentorContextLabel: String = "",

    // ── Mentor session (top-N mistakes review) ───────────────────────────────
    /** Ordered list of moveIndices to review this session (sorted by severity desc). */
    val mentorSessionQueue: List<Int> = emptyList(),
    /** Current position (0-based) inside [mentorSessionQueue]. */
    val mentorSessionIdx: Int = 0,

    // ── Post-move classification quiz ────────────────────────────────────────
    /** True when the "Why was this critical?" 4-option quiz should be shown. */
    val showClassificationQuiz: Boolean = false,
    /** The 4 shuffled options for the quiz (one is correct). */
    val classificationOptions: List<ClassificationOption> = emptyList(),
    /** Index into [classificationOptions] that is the correct answer. */
    val classificationCorrectIndex: Int = -1,
    /** Index selected by the user. -1 = not yet selected. */
    val classificationSelectedIndex: Int = -1,

    // ── Mentor insight reveal gate ────────────────────────────────────────────
    /** False until the user has attempted a move, asked for a hint, or revealed the answer.
     *  While false the panel hides the specific motif title so the user must think first. */
    val guidedDiscoveryInsightRevealed: Boolean = false,

    // ── Weakness-aware coaching context ──────────────────────────────────────
    /** Cross-game weakness summary shown at the start of a Mentor session. Null until computed. */
    val weaknessContext: WeaknessContext? = null,
    /** True while the Coach's Briefing banner should be visible at session start. */
    val showCoachsBriefing: Boolean = false,

    // ── Proactive coaching triggers (Board Scan) ──────────────────────────────
    /** All coaching triggers detected for this game, keyed by moveIndex. */
    val triggersByMove: Map<Int, List<CoachingTrigger>> = emptyMap(),
    /** The trigger currently being shown in the proactive coaching panel. Null when panel hidden. */
    val activeProactiveTrigger: CoachingTrigger? = null,
    /** True when the ProactiveCoachingPanel is visible. */
    val showProactiveCoaching: Boolean = false,
    /** Move indices where the user tapped the Coach Lamp — used to build reflection items. */
    val triggersEngaged: Set<Int> = emptySet(),

    // ── Coordination visual quiz ──────────────────────────────────────────────
    /** Phase of the coordination quiz: ASKING (question shown) → REVEALING (arrows on board). */
    val coordinationQuizPhase: CoordinationQuizPhase = CoordinationQuizPhase.ASKING,

    // ── Interactive coach answer mode ─────────────────────────────────────────
    /** True while the user is being prompted to tap a square on the board to answer. */
    val proactiveInteractiveMode: Boolean = false,
    /** Feedback text shown after the user selects a square. Null = no answer yet. */
    val proactiveAnswerFeedback: String? = null,
    /** True = user's tapped square was correct, False = wrong, null = no answer yet. */
    val proactiveAnswerIsCorrect: Boolean? = null,
    /** Temporary square highlights added by the coach (not saved as annotations). */
    val coachHighlightSquares: List<MarkedSquare> = emptyList(),
    /** All hanging squares detected at the current position for PreMoveChecklist. */
    val proactiveHangingSquares: List<String> = emptyList(),
    /** Subset of [proactiveHangingSquares] belonging to the user's own pieces. */
    val proactiveHangingOwnSquares: Set<String> = emptySet(),
    /** Subset of [proactiveHangingSquares] the user has already tapped correctly. */
    val proactiveFoundSquares: Set<String> = emptySet(),

    // ── Board Scan Reflection Mode (end of Mentor session) ───────────────────
    /** True when Reflection Mode is active (shown after Mentor session completes). */
    val showReflectionMode: Boolean = false,
    /** Positions with un-engaged triggers the user can now self-categorize. */
    val reflectionItems: List<ReflectionItem> = emptyList(),

    // ── Structured Analysis Prompts (position coach) ──────────────────────────
    /** Mirrors the setting — false = card never shown regardless of highlights. */
    val positionCoachEnabled: Boolean = false,

    // ── Developer Options ─────────────────────────────────────────────────────
    /** When true, a "Copy LLM Prompt" button appears next to active coaching panels. */
    val developerModeEnabled: Boolean = false,
    /** Move indices where the user has already dismissed the coach card this session. */
    val positionCoachDismissedMoves: Set<Int> = emptySet(),

    // ── Opening Theory Coach ──────────────────────────────────────────────────
    /** First deviation from opening book theory, null if game stayed in book or no data. */
    val openingDeviation: OpeningDeviation? = null,
    /** True while the OpeningDeviationPanel is shown (user navigated to the deviation move). */
    val showOpeningDeviationPanel: Boolean = false,
    /** True once the user has dismissed the panel — prevents it re-appearing on re-navigation. */
    val openingDeviationDismissed: Boolean = false,

    // ── Endgame Recognition Coach ─────────────────────────────────────────────
    /** First recognised endgame type in this game, null if no known endgame was reached. */
    val endgameClassification: com.acepero13.android.gamereviewer.domain.EndgameClassification? = null,
    /** True while the EndgameRecognitionPanel is shown (user navigated to the endgame start move). */
    val showEndgameRecognitionPanel: Boolean = false,
    /** True once the user has dismissed the panel — prevents it re-appearing on re-navigation. */
    val endgamePanelDismissed: Boolean = false,

    // ── Middlegame Plan Coach ─────────────────────────────────────────────────
    /** Detected pawn structure plans at the start of the middlegame, null if none found. */
    val middlegamePlanClassification: com.acepero13.android.gamereviewer.domain.MiddlegamePlanClassification? = null,
    /** True while the MiddlegamePlanPanel is shown (user navigated to the detected middlegame move). */
    val showMiddlegamePlanPanel: Boolean = false,
    /** True once the user has dismissed the panel — prevents it re-appearing on re-navigation. */
    val middlegamePlanPanelDismissed: Boolean = false,

    // ── Pre-game prediction gate ──────────────────────────────────────────────
    /** True while the prediction overlay is shown (before the user starts navigating). */
    val showPredictionGate: Boolean = false,
    /** The prediction the user selected before starting the review. Null if skipped. */
    val gamePrediction: GamePrediction? = null,

    // ── Game story headline ───────────────────────────────────────────────────
    /** One-sentence narrative computed after background analysis completes. */
    val gameStory: String = "",
    /** True once the user has dismissed the story card. */
    val gameStoryDismissed: Boolean = false,
    /** Sticky flag — true once the session ends (user reaches the last move) or
     *  when structured analysis prompts are enabled.  Never resets to false. */
    val gameStoryUnlocked: Boolean = false,

    // ── Post-game debrief ─────────────────────────────────────────────────────
    /** True when the user has navigated to the end of the game and analysis is done. */
    val showPostGameDebrief: Boolean = false,
    /** Comparison of the player's prediction vs engine findings. */
    val predictionMatchResult: PredictionMatchResult? = null,

    // ── Recency Bias — player's historically weak trigger types ──────────────
    /** Trigger types the player has consistently missed across previous games.
     *  Populated during background analysis from BehavioralDiagnostic. */
    val weakTriggerTypes: Set<String> = emptySet(),

    // ── Mentor pivotal moments (Big Three) ───────────────────────────────────
    /** The three key moments for this game's mentor session. Null until computed. */
    val pivotalMoments: com.acepero13.android.gamereviewer.domain.PivotalMoments? = null,
    /** True while the Pivotal Moments overview panel is shown before the session begins. */
    val showPivotalMomentsPanel: Boolean = false,

    // ── Calibration quiz ─────────────────────────────────────────────────────
    /** True while the CalibrationPanel is visible to the user. */
    val showCalibrationPanel: Boolean = false,
    /** The EvalCalibration trigger driving the current quiz. Null when panel is hidden. */
    val calibrationTrigger: com.acepero13.android.gamereviewer.domain.CoachingTrigger.EvalCalibration? = null,
    /** User's current slider selection: -2 (Strong Black) … +2 (Strong White). Starts at 0 (Equal). */
    val calibrationUserValue: Int = 0,
    /** True once the user has pressed "Lock In My Assessment". */
    val calibrationLocked: Boolean = false,
    /** Feedback text shown after locking in (compares user assessment to engine). */
    val calibrationFeedback: String = "",
    /** True when the feedback is positive (user was close to engine eval). */
    val calibrationFeedbackPositive: Boolean = false,
)

private const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
private const val CATEGORY_COOLDOWN_MOVES = 5

// ── ViewModel ─────────────────────────────────────────────────────────────────

class AnalysisViewModel(
    private val gameId: Long,
    private val repo: GameRepository,
    private val annotationDao: PositionAnnotationDao,
    private val criticalMomentDao: CriticalMomentDao,
    private val gameEvaluationDao: GameEvaluationDao,
    private val moveTimeDao: MoveTimeDao,
    private val endgameEncounterDao: com.acepero13.android.gamereviewer.data.db.EndgameEncounterDao,
    private val engine: StockfishEngine,
    private val opening: OpeningClassifier,
    private val truthMapBuilder: TruthMapBuilder,
    private val settingsRepo: SettingsRepository,
    private val masteryRepo: TriggerMasteryRepository,
    private val deviationAnalyzer: OpeningDeviationAnalyzer,
    private val endgameRecognizer: com.acepero13.android.gamereviewer.domain.EndgameRecognizer,
    private val middlegamePlanDetector: com.acepero13.android.gamereviewer.domain.MiddlegamePlanDetector,
) : ViewModel() {

    private val tag = "$TAG[game=$gameId]"

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
        Log.d(tag, "goToMove($index) — reviewMode=${_uiState.value.reviewMode} uciMoves.size=${uciMoves.size} fenSequence.size=${fenSequence.size}")
        if (_uiState.value.reviewMode == ReviewMode.MENTOR) {
            Log.d(tag, "goToMove: BLOCKED (mentor/guided discovery active)")
            return
        }
        // Diagnostic: if uciMoves is still empty, log the raw DB fields so we can see why
        if (uciMoves.isEmpty()) {
            val game = _uiState.value.game
            Log.e(tag, "goToMove: uciMoves empty! game=${game?.let { "'${it.whitePlayer} vs ${it.blackPlayer}' movesUci.length=${it.movesUci.length} movesUci_preview='${it.movesUci.take(60)}' pgn.length=${it.pgn.length} pgn_preview='${it.pgn.take(120)}'" } ?: "null"}")
        }
        val clamped = index.coerceIn(0, uciMoves.size)
        Log.d(tag, "goToMove: clamped=$clamped  currentMoveIndex=${_uiState.value.moveIndex}")
        val prev    = _uiState.value.moveIndex
        if (clamped > prev) checkMissedMoments(fromIndex = prev, toIndex = clamped)
        viewModelScope.launch(Dispatchers.Default) { applyMoveIndex(clamped) }
    }

    fun stepForward()  { Log.d(tag, "stepForward  moveIndex=${_uiState.value.moveIndex}"); goToMove(_uiState.value.moveIndex + 1) }
    fun stepBackward() { Log.d(tag, "stepBackward moveIndex=${_uiState.value.moveIndex}"); goToMove(_uiState.value.moveIndex - 1) }
    fun goToStart()    { Log.d(tag, "goToStart");  goToMove(0) }
    fun goToEnd()      { Log.d(tag, "goToEnd");    goToMove(uciMoves.size) }

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

    fun undoLastArrow() {
        val cur = _uiState.value.boardState
        if (cur.userArrows.isEmpty()) return
        val upd = cur.copy(userArrows = cur.userArrows.dropLast(1))
        _uiState.update { it.copy(boardState = upd) }
        persistAnnotation(upd)
    }

    fun clearArrows() {
        val cur = _uiState.value.boardState
        if (cur.userArrows.isEmpty()) return
        val upd = cur.copy(userArrows = emptyList())
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

            // Look up the engine's verdict for this position; fall back to STRATEGIC_MISTAKE
            // when background analysis hasn't run yet or this move wasn't flagged.
            val truthEntry     = truthMap.find { it.moveIndex == idx }
            val reasonCategory = truthEntry?.let { motifToReason(it) }
                ?: CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE.name
            val severity       = truthEntry?.let { kotlin.math.abs(it.playerEvalDelta) } ?: 0

            criticalMomentDao.insert(
                CriticalMoment(
                    gameId           = gameId,
                    moveIndex        = idx,
                    type             = CriticalMoment.Type.USER_MARKED.name,
                    severity         = severity,
                    reasonCategory   = reasonCategory,
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

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC — Stats sheet
    // ═══════════════════════════════════════════════════════════════════════════

    fun toggleStatsSheet() {
        val nowOpen = !_uiState.value.showStatsSheet
        _uiState.update { it.copy(showStatsSheet = nowOpen) }
        if (nowOpen && _uiState.value.playerStats == null) {
            viewModelScope.launch(Dispatchers.IO) {
                val game = _uiState.value.game ?: return@launch
                val evals = gameEvaluationDao.getByGameId(gameId)
                val times = moveTimeDao.getByGameId(gameId)
                val stats = com.acepero13.android.gamereviewer.domain.PlayerStatsCalculator
                    .compute(game, evals, times)
                _uiState.update { it.copy(playerStats = stats) }
            }
        }
    }

    fun dismissStatsSheet() {
        _uiState.update { it.copy(showStatsSheet = false) }
    }

    fun dismissMissedMomentBanner() {
        _uiState.update { it.copy(showMissedMomentBanner = false, missedMomentMoveIndex = null) }
    }

    fun dismissOpeningDeviationPanel() {
        _uiState.update { it.copy(showOpeningDeviationPanel = false, openingDeviationDismissed = true) }
    }

    fun dismissEndgameRecognitionPanel() {
        _uiState.update { it.copy(showEndgameRecognitionPanel = false, endgamePanelDismissed = true) }
    }

    fun dismissMiddlegamePlanPanel() {
        _uiState.update { it.copy(showMiddlegamePlanPanel = false, middlegamePlanPanelDismissed = true) }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC — Pre-game prediction & post-game debrief
    // ═══════════════════════════════════════════════════════════════════════════

    fun submitPrediction(prediction: GamePrediction) {
        _uiState.update { it.copy(showPredictionGate = false, gamePrediction = prediction) }
    }

    fun skipPrediction() {
        _uiState.update { it.copy(showPredictionGate = false) }
    }

    fun dismissGameStory() {
        _uiState.update { it.copy(gameStoryDismissed = true) }
    }

    fun dismissPostGameDebrief() {
        _uiState.update { it.copy(showPostGameDebrief = false) }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC — Developer / Coach Accuracy Debug
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Builds a filled-in LLM evaluation prompt for the currently active coaching panel.
     * Returns null when developer mode is off or no coaching panel is active.
     *
     * The returned string can be copied to the clipboard and pasted into any LLM
     * to verify whether the coaching comment is accurate and pedagogically sound.
     */
    fun buildCoachEvalPrompt(): String? {
        val s   = _uiState.value
        val idx = s.moveIndex
        if (idx <= 0 || fenSequence.size < idx) return null

        val fenBefore  = fenSequence[idx - 1]
        val san        = if (sanMoves.size >= idx) sanMoves[idx - 1] else "?"
        val isWhite    = idx % 2 == 1
        val colorLabel = if (isWhite) "White" else "Black"
        val moveNumber = (idx + 1) / 2

        val evalBefore = truthMap.getOrNull(idx - 1)?.evalCp
        val evalAfter  = truthMap.getOrNull(idx)?.evalCp
        val cpLoss     = if (evalBefore != null && evalAfter != null) {
            val delta = evalAfter - evalBefore
            if (isWhite) -delta else delta
        } else null

        fun fmt(cp: Int?) = when {
            cp == null   -> "N/A"
            cp > 9000    -> "Forced mate (White)"
            cp < -9000   -> "Forced mate (Black)"
            else         -> "%+.2f".format(cp / 100.0)
        }

        // GameHighlights and coaching triggers at this move — used for both
        // the phase field (more accurate than heuristic) and the debug section.
        val highlightsHere   = s.gameHighlights.filter { it.moveIndex == idx }
        val triggersHere     = s.triggersByMove[idx] ?: emptyList()

        val phase = highlightsHere.firstOrNull()?.phase?.name?.lowercase()
            ?.replaceFirstChar { it.uppercase() }
            ?: when {
                idx <= 15 -> "Opening"
                idx <= 35 -> "Middlegame"
                else      -> "Endgame"
            }

        val triggerType: String
        val insight: InsightReconciler.Insight
        val criticalMoment = s.guidedDiscoveryCriticalMoment

        when {
            s.guidedDiscoveryMode && s.guidedDiscoveryInsight != null -> {
                triggerType = criticalMoment?.reasonCategory ?: "UNKNOWN"
                insight     = s.guidedDiscoveryInsight
            }
            s.showProactiveCoaching && s.activeProactiveTrigger != null -> {
                triggerType = s.activeProactiveTrigger.typeName()
                insight     = InsightReconciler.forTrigger(s.activeProactiveTrigger)
            }
            s.showMiddlegamePlanPanel && s.middlegamePlanClassification != null -> {
                return buildMiddlegamePlanEvalPrompt(s.middlegamePlanClassification)
            }
            s.showEndgameRecognitionPanel && s.endgameClassification != null -> {
                return buildEndgameEvalPrompt(s.endgameClassification)
            }
            else -> return null
        }

        val questions = insight.questions
            .mapIndexed { i, q -> "${i + 1}. $q" }
            .joinToString("\n")

        return buildString {
            appendLine("## Position")
            appendLine()
            appendLine("FEN: $fenBefore")
            appendLine()
            appendLine("Move played: $san")
            appendLine("Color that played the move: $colorLabel")
            appendLine("Move number: $moveNumber")
            appendLine()
            appendLine("Game phase: $phase")
            appendLine()
            appendLine("Engine evaluation BEFORE move: ${fmt(evalBefore)}")
            appendLine("Engine evaluation AFTER move:  ${fmt(evalAfter)}")
            appendLine("Centipawn loss: ${if (cpLoss != null) "$cpLoss cp" else "N/A"}")
            appendLine()
            appendLine("Engine best move: N/A")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## Coaching trigger type")
            appendLine()
            appendLine(triggerType)
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## App's coaching output")
            appendLine()
            appendLine("**Title:** ${insight.title}")
            appendLine()
            appendLine("**Description:** ${insight.description}")
            appendLine()
            appendLine("**Coaching questions shown to the user:**")
            appendLine(questions)
            appendLine()
            appendLine("**Conceptual hint (shown on request):** ${insight.conceptualHint}")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## Rule Debug Info")
            appendLine()

            // ── Active coaching panel source ──────────────────────────────────
            if (criticalMoment != null && s.guidedDiscoveryMode) {
                appendLine("**Panel source:** Guided Discovery (CriticalMoment)")
                appendLine("**CriticalMoment.type:** ${criticalMoment.type}")
                appendLine("**CriticalMoment.severity:** ${criticalMoment.severity} cp")
                appendLine("**CriticalMoment.explanationState:** ${criticalMoment.explanationState}")
            } else if (s.activeProactiveTrigger != null) {
                val t = s.activeProactiveTrigger
                appendLine("**Panel source:** Proactive Coaching (CoachingTriggerEvaluator.kt)")
                appendLine("**Trigger class:** CoachingTrigger.${t::class.simpleName}")
                val props = when (t) {
                    is CoachingTrigger.Safety              -> "kingSquare=${t.kingSquare}"
                    is CoachingTrigger.CandidateMoves      -> "evalCp=${t.evalCp}"
                    is CoachingTrigger.WorstPiece          -> "pieceSquare=${t.pieceSquare}, mobility=${t.mobility}"
                    is CoachingTrigger.ForcingMove         -> "motif=${t.motif}"
                    is CoachingTrigger.OpponentPlan        -> "evalGain=${t.evalGain}"
                    is CoachingTrigger.PreMoveChecklist    -> "hangingSquare=${t.hangingSquare}"
                    is CoachingTrigger.RookActivation      -> "rookSquare=${t.rookSquare}, openFileIndex=${t.openFileIndex}"
                    is CoachingTrigger.ImpulseControl      -> "timeSpentSeconds=${t.timeSpentSeconds}, cpLoss=${t.cpLoss}"
                    is CoachingTrigger.CalculationBlunder  -> "timeSpentSeconds=${t.timeSpentSeconds}, cpLoss=${t.cpLoss}"
                    is CoachingTrigger.TacticalOversight   -> "timeSpentSeconds=${t.timeSpentSeconds}, cpLoss=${t.cpLoss}"
                    is CoachingTrigger.CandidateSearch     -> "evalCp=${t.evalCp}"
                    is CoachingTrigger.CctCheck            -> "evalDelta=${t.evalDelta}"
                    is CoachingTrigger.ConversionStrategy  -> "evaluationCp=${t.evaluationCp}"
                    is CoachingTrigger.CoordinatedAttack   -> "isPlayerSide=${t.isPlayerSide}, isLoss=${t.isLoss}, pieceCount=${t.pieceCount}"
                    is CoachingTrigger.PieceHarmony        -> "isPlayerSide=${t.isPlayerSide}, isLoss=${t.isLoss}, score=${t.score}"
                    is CoachingTrigger.PunishBlunder       -> "opponentLoss=${t.opponentLoss}"
                    is CoachingTrigger.EvalCalibration     -> "engineEvalCp=${t.engineEvalCp}, context=${t.context}"
                }
                appendLine("**Trigger properties:** $props")
            }

            // ── All triggers detected at this move ────────────────────────────
            if (triggersHere.isNotEmpty()) {
                appendLine()
                appendLine("**All coaching triggers at move $idx:**")
                triggersHere.forEach { t ->
                    appendLine("  • ${t.typeName()} (CoachingTrigger.${t::class.simpleName})")
                }
            }

            // ── GameHighlight rules that fired at this move ───────────────────
            appendLine()
            if (highlightsHere.isEmpty()) {
                appendLine("**GameHighlight rules at move $idx:** none")
            } else {
                appendLine("**GameHighlight rules at move $idx:**")
                highlightsHere.forEach { h ->
                    val fileName = h.ruleType
                        .split("_")
                        .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
                        .let { "${it}Rule.kt" }
                    appendLine()
                    appendLine("  Rule file : $fileName")
                    appendLine("  ruleType  : ${h.ruleType}")
                    appendLine("  severity  : ${h.severity}")
                    appendLine("  title     : ${h.title}")
                    appendLine("  description: ${h.description}")
                    appendLine("  tip       : ${h.improvementTip}")
                }
            }
        }
    }

    private fun buildMiddlegamePlanEvalPrompt(c: com.acepero13.android.gamereviewer.domain.MiddlegamePlanClassification): String {
        val insights = c.plans.map { InsightReconciler.forMiddlegamePlan(it) }
        return buildString {
            appendLine("## Position")
            appendLine()
            appendLine("FEN: ${c.fen}")
            appendLine("Move index: ${c.moveIndex}")
            appendLine("Game phase: Middlegame")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## Detected Plans (${c.plans.size})")
            appendLine()
            c.plans.forEachIndexed { i, plan ->
                val ins = insights.getOrNull(i)
                appendLine("### Plan ${i + 1}: ${plan.title}")
                appendLine("**Type:** ${plan.type}")
                appendLine("**Priority:** ${plan.priority}")
                plan.affectedFile?.let { appendLine("**Affected file:** $it") }
                appendLine("**Advice:** ${plan.planAdvice}")
                if (ins != null) {
                    appendLine()
                    appendLine("**Coaching output for this plan:**")
                    appendLine("Title: ${ins.title}")
                    appendLine("Description: ${ins.description}")
                    appendLine("Questions shown to user:")
                    ins.questions.forEachIndexed { qi, q -> appendLine("  ${qi + 1}. $q") }
                    appendLine("Conceptual hint: ${ins.conceptualHint}")
                }
                appendLine()
            }
        }
    }

    private fun buildEndgameEvalPrompt(c: com.acepero13.android.gamereviewer.domain.EndgameClassification): String {
        val insight = InsightReconciler.forEndgame(chapter = c.entry.chapter, name = c.entry.name)
        return buildString {
            appendLine("## Position")
            appendLine()
            appendLine("FEN: ${c.fen}")
            appendLine("First endgame move index: ${c.firstEndgameMoveIndex}")
            appendLine("Game phase: Endgame")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## Endgame Classification")
            appendLine()
            appendLine("**Name:** ${c.entry.name}")
            appendLine("**Chapter:** ${c.entry.chapter} of *100 Endgames You Should Know*")
            appendLine("**Category:** ${c.entry.category}")
            appendLine("**Material signature:** ${c.entry.materialSignature}")
            appendLine("**Study advice:** ${c.entry.studyAdvice}")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## App's coaching output")
            appendLine()
            appendLine("**Title:** ${insight.title}")
            appendLine("**Description:** ${insight.description}")
            appendLine()
            appendLine("**Questions shown to user:**")
            insight.questions.forEachIndexed { i, q -> appendLine("${i + 1}. $q") }
            appendLine()
            appendLine("**Conceptual hint:** ${insight.conceptualHint}")
        }
    }

    /**
     * Navigates to the missed critical position and activates Mentor mode
     * (Task 3.3) so the user cannot simply scroll past it again.
     */
    fun reviewMissedMoment() {
        val idx = _uiState.value.missedMomentMoveIndex ?: return
        dismissMissedMomentBanner()          // state update #1 — banner begins slideOut animation
        viewModelScope.launch {
            delay(200)                       // let the slideOutVertically animation complete
            enterMentorMode(targetMoveIndex = idx)
        }
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
    // PUBLIC — Proactive coaching (Board Scan triggers)
    // ═══════════════════════════════════════════════════════════════════════════

    /** Opens the ProactiveCoachingPanel (or CalibrationPanel) for the primary trigger at the current position. */
    fun enterProactiveCoaching() {
        val idx     = _uiState.value.moveIndex
        val trigger = _uiState.value.triggersByMove[idx]?.firstOrNull() ?: return
        if (trigger is CoachingTrigger.EvalCalibration) {
            _uiState.update {
                it.copy(
                    showCalibrationPanel   = true,
                    calibrationTrigger     = trigger,
                    calibrationUserValue   = 0,
                    calibrationLocked      = false,
                    calibrationFeedback    = "",
                    calibrationFeedbackPositive = false,
                    triggersEngaged        = it.triggersEngaged + idx,
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    showProactiveCoaching  = true,
                    activeProactiveTrigger = trigger,
                    triggersEngaged        = it.triggersEngaged + idx,
                )
            }
        }
    }

    /** Updates the slider position in the calibration quiz without locking in. */
    fun onCalibrationValueChange(value: Int) {
        _uiState.update { it.copy(calibrationUserValue = value.coerceIn(-2, 2)) }
    }

    /**
     * Locks in the user's assessment and reveals feedback comparing it to the engine eval.
     * The engine eval stored in the trigger is never shown raw — only mapped to the same
     * 5-point scale so the feedback stays in the user's own language.
     */
    fun lockInCalibration() {
        val trigger = _uiState.value.calibrationTrigger ?: return
        val userValue    = _uiState.value.calibrationUserValue
        val engineValue  = evalCpToCalibrationValue(trigger.engineEvalCp)
        val diff         = kotlin.math.abs(userValue - engineValue)
        val userLabel    = calibrationValueLabel(userValue)
        val engineLabel  = calibrationValueLabel(engineValue)

        val (feedback, positive) = when (diff) {
            0 -> Pair(
                "Spot on. You read the board exactly as the engine does — that kind of positional clarity is a real asset.",
                true,
            )
            1 -> Pair(
                "Close. You said $userLabel and the engine agrees it's $engineLabel — you're in the right neighbourhood. The difference is a matter of degree, not direction.",
                true,
            )
            2 -> Pair(
                "You felt it was $userLabel, but the engine evaluates it as $engineLabel. Try to identify what piece or pawn structure difference you might have missed.",
                false,
            )
            else -> Pair(
                "You felt it was $userLabel, but the engine sees $engineLabel here. This is a gap worth studying — look at the pawn structure and piece activity to see what you underestimated.",
                false,
            )
        }
        _uiState.update {
            it.copy(
                calibrationLocked           = true,
                calibrationFeedback         = feedback,
                calibrationFeedbackPositive = positive,
            )
        }
    }

    /** Closes the calibration panel and resets its state. */
    fun dismissCalibration() {
        _uiState.update {
            it.copy(
                showCalibrationPanel        = false,
                calibrationTrigger          = null,
                calibrationUserValue        = 0,
                calibrationLocked           = false,
                calibrationFeedback         = "",
                calibrationFeedbackPositive = false,
            )
        }
    }

    private fun evalCpToCalibrationValue(evalCp: Int): Int = when {
        evalCp <= -150 -> -2
        evalCp <= -50  -> -1
        evalCp < 50    ->  0
        evalCp < 150   ->  1
        else           ->  2
    }

    private fun calibrationValueLabel(value: Int): String = when (value) {
        -2 -> "a strong Black advantage"
        -1 -> "a slight Black advantage"
         0 -> "equal"
         1 -> "a slight White advantage"
         2 -> "a strong White advantage"
        else -> "unknown"
    }

    fun dismissProactiveCoaching() {
        // Keep activeProactiveTrigger non-null so the Panel's exit animation
        // can play while still in composition. It is cleared in applyMoveIndex
        // when the user navigates to the next position.
        _uiState.update { it.copy(
            showProactiveCoaching       = false,
            proactiveInteractiveMode    = false,
            proactiveAnswerFeedback     = null,
            proactiveAnswerIsCorrect    = null,
            coachHighlightSquares       = emptyList(),
            proactiveHangingSquares     = emptyList(),
            proactiveHangingOwnSquares  = emptySet(),
            proactiveFoundSquares       = emptySet(),
            coordinationQuizPhase       = CoordinationQuizPhase.ASKING,
            boardState                  = it.boardState.copy(arrows = emptyList()),
        )}
    }

    /** Reveals the coordination arrows on the board for CoordinatedAttack / PieceHarmony triggers. */
    fun onCoordinationQuizReveal() {
        val trigger = _uiState.value.activeProactiveTrigger ?: return
        val arrows = buildCoordinationArrows(trigger)
        _uiState.update { it.copy(
            coordinationQuizPhase = CoordinationQuizPhase.REVEALING,
            boardState = it.boardState.copy(arrows = arrows),
        )}
    }

    private fun buildCoordinationArrows(trigger: CoachingTrigger): List<Arrow> = when (trigger) {
        is CoachingTrigger.CoordinatedAttack -> {
            val target = trigger.targetSquare ?: return emptyList()
            trigger.attackerSquares.map { attacker -> Arrow(attacker, target, AnalyzeBlue) }
        }
        is CoachingTrigger.PieceHarmony -> {
            if (trigger.targetSquares.isEmpty() || trigger.attackerSquares.isEmpty()) return emptyList()
            trigger.attackerSquares.mapNotNull { attacker ->
                val target = trigger.targetSquares.minByOrNull { t ->
                    val df = (squareFile(attacker) - squareFile(t)).toLong()
                    val dr = (squareRank(attacker) - squareRank(t)).toLong()
                    df * df + dr * dr
                } ?: return@mapNotNull null
                Arrow(attacker, target, AnalyzeBlue)
            }
        }
        else -> emptyList()
    }

    private fun squareFile(sq: Square): Int = BoardAttackHelper.fileOf(sq)
    private fun squareRank(sq: Square): Int = BoardAttackHelper.rankOf(sq)

    /** Marks the current position coach card as dismissed for this session. */
    fun dismissPositionCoach() {
        val idx = _uiState.value.moveIndex
        _uiState.update { it.copy(
            positionCoachDismissedMoves = it.positionCoachDismissedMoves + idx,
        )}
    }

    /** Activates board-tap routing so the user can select a square to answer the coach's question. */
    fun startProactiveInteraction() {
        val trigger = _uiState.value.activeProactiveTrigger
        // For PreMoveChecklist, detect ALL hanging squares from the live FEN.
        // For CctCheck, detect all squares targeted by the opponent's CCT moves.
        val (targetSquares, ownSquares) = when (trigger) {
            is CoachingTrigger.PreMoveChecklist -> detectAllHangingSquares()
            is CoachingTrigger.CctCheck         -> Pair(detectOpponentCctSquares(), emptySet())
            else                                -> Pair(emptyList(), emptySet())
        }

        _uiState.update { it.copy(
            proactiveInteractiveMode    = true,
            proactiveAnswerFeedback     = null,
            proactiveAnswerIsCorrect    = null,
            coachHighlightSquares       = emptyList(),
            proactiveHangingSquares     = targetSquares,
            proactiveHangingOwnSquares  = ownSquares,
            proactiveFoundSquares       = emptySet(),
        )}
    }

    /** Scans the current board position for pieces with more attackers than defenders. */
    /** Returns (allHangingSquareNames, ownHangingSquareNames) split by player side. */
    private fun detectAllHangingSquares(): Pair<List<String>, Set<String>> {
        val fen = _uiState.value.boardState.fen
        if (fen.isBlank()) return Pair(emptyList(), emptySet())
        val board = runCatching { Board().apply { loadFromFen(fen) } }.getOrNull()
            ?: return Pair(emptyList(), emptySet())
        val userSide = if (boardFlippedForBlack) Side.BLACK else Side.WHITE
        val hanging = BoardAttackHelper.allPieces(board)
            .filter { (_, piece) -> piece.pieceType != PieceType.KING }
            .filter { (sq, piece) -> BoardAttackHelper.isGenuinelyHanging(board, sq, piece) }
        val all = hanging.map { (sq, _) -> sq.name }
        val own = hanging.filter { (_, piece) -> piece.pieceSide == userSide }.map { (sq, _) -> sq.name }.toSet()
        return Pair(all, own)
    }

    /**
     * Computes all squares targeted by the opponent's Check, Capture, or Threat moves
     * from the current board position. The FEN after the user's move has the opponent to move,
     * so we enumerate their legal moves and collect any that are checks or captures.
     */
    private fun detectOpponentCctSquares(): List<String> {
        val fen = _uiState.value.boardState.fen
        if (fen.isBlank()) return emptyList()
        val board = runCatching { Board().apply { loadFromFen(fen) } }.getOrNull() ?: return emptyList()
        val sideToMove = board.sideToMove
        return MoveGenerator.generateLegalMoves(board).mapNotNull { move ->
            val targetPiece = board.getPiece(move.to)
            val isCapture   = targetPiece != Piece.NONE && targetPiece.pieceSide != sideToMove
            val givesCheck  = runCatching {
                val clone = Board().apply { loadFromFen(fen) }
                clone.doMove(move)
                clone.isKingAttacked
            }.getOrDefault(false)
            when {
                givesCheck -> move.to.name
                isCapture && isMateriallyFavorableCapture(board, move, fen, sideToMove) -> move.to.name
                else -> null
            }
        }.distinct()
    }

    /**
     * Returns true if the opponent's capture is a real threat — i.e., they gain material or at
     * least break even after any recapture. Filters out losing captures like Qxpawn where the
     * queen is immediately recaptured.
     */
    private fun isMateriallyFavorableCapture(
        board: Board,
        move: Move,
        fen: String,
        sideToMove: com.github.bhlangonijr.chesslib.Side,
    ): Boolean {
        val capturingPiece = board.getPiece(move.from)
        val capturedPiece  = board.getPiece(move.to)
        val capturingValue = cctPieceValue(capturingPiece)
        val capturedValue  = cctPieceValue(capturedPiece)
        // Always a real threat if the opponent captures a more valuable piece.
        if (capturedValue >= capturingValue) return true
        // Captured piece is less valuable — only a real threat if the landing square is
        // undefended (i.e., the capturing piece cannot be recaptured).
        val afterCapture = runCatching {
            val clone = Board().apply { loadFromFen(fen) }
            clone.doMove(move)
            clone
        }.getOrNull() ?: return true
        val canRecapture = MoveGenerator.generateLegalMoves(afterCapture).any { it.to == move.to }
        return !canRecapture
    }

    private fun cctPieceValue(piece: Piece): Int = when (piece.pieceType) {
        PieceType.PAWN   -> 100
        PieceType.KNIGHT -> 320
        PieceType.BISHOP -> 330
        PieceType.ROOK   -> 500
        PieceType.QUEEN  -> 900
        PieceType.KING   -> 20000
        else             -> 0
    }

    /** Validates the square the user tapped against the active trigger's expected answer. */
    fun answerProactiveQuestion(square: Square) {
        val trigger = _uiState.value.activeProactiveTrigger ?: return
        val state   = _uiState.value

        // ── Multi-select mode: PreMoveChecklist and CctCheck ─────────────────
        val isMultiSelect = (trigger is CoachingTrigger.PreMoveChecklist || trigger is CoachingTrigger.CctCheck)
                && state.proactiveHangingSquares.isNotEmpty()
        if (isMultiSelect) {
            val green  = Color(0xFF22C55E)
            val blue   = Color(0xFF3B82F6)
            val red    = Color(0xFFEF4444)
            val sqName = square.name

            if (sqName in state.proactiveHangingSquares) {
                val newFound = state.proactiveFoundSquares + sqName
                val total    = state.proactiveHangingSquares.size
                val allFound = newFound.size == total
                val feedback = when {
                    trigger is CoachingTrigger.CctCheck && allFound ->
                        "All $total opponent CCT target${if (total == 1) "" else "s"} found! Good threat awareness."
                    trigger is CoachingTrigger.CctCheck ->
                        "Correct! ${newFound.size}/$total found — keep scanning for checks and captures."
                    allFound ->
                        "All $total hanging piece${if (total == 1) "" else "s"} found! Great board scan."
                    else ->
                        "Correct! ${newFound.size}/$total found — keep looking for more loose pieces."
                }
                val isOwn     = sqName in state.proactiveHangingOwnSquares
                val hitColor  = if (isOwn) green else blue
                _uiState.update { it.copy(
                    proactiveAnswerFeedback  = feedback,
                    proactiveAnswerIsCorrect = true,
                    proactiveFoundSquares    = newFound,
                    proactiveInteractiveMode = !allFound,
                    coachHighlightSquares    = it.coachHighlightSquares +
                        MarkedSquare(square, hitColor.copy(alpha = 0.6f)),
                )}
            } else {
                val missMsg = if (trigger is CoachingTrigger.CctCheck)
                    "That square isn't targeted by a check or capture. Look for opponent moves that win material or give check."
                else
                    "That piece is adequately defended. Count attackers vs. defenders — find one where attackers win."
                _uiState.update { it.copy(
                    proactiveAnswerFeedback  = missMsg,
                    proactiveAnswerIsCorrect = false,
                    coachHighlightSquares    = it.coachHighlightSquares +
                        MarkedSquare(square, red.copy(alpha = 0.45f)),
                )}
            }
            return
        }

        // ── Other triggers: single-square mode ────────────────────────────────
        val (feedback, correct, coachMarks) = evaluateProactiveAnswer(trigger, square)
        _uiState.update { it.copy(
            proactiveInteractiveMode = false,
            proactiveAnswerFeedback  = feedback,
            proactiveAnswerIsCorrect = correct,
            coachHighlightSquares    = coachMarks,
        )}
    }

    private fun evaluateProactiveAnswer(
        trigger: CoachingTrigger,
        tapped:  Square,
    ): Triple<String, Boolean?, List<MarkedSquare>> {
        val green = Color(0xFF22C55E)
        val red   = Color(0xFFEF4444)

        fun squareFromName(name: String?) =
            if (name.isNullOrBlank()) null
            else runCatching { Square.valueOf(name) }.getOrNull()

        return when (trigger) {
            is CoachingTrigger.WorstPiece -> {
                val answer = squareFromName(trigger.pieceSquare)
                if (answer != null && tapped == answer) {
                    Triple(
                        "Correct! That piece has only ${trigger.mobility} move${if (trigger.mobility == 1) "" else "s"} — it needs rerouting.",
                        true,
                        listOf(MarkedSquare(tapped, green.copy(alpha = 0.6f))),
                    )
                } else {
                    val marks = buildList {
                        add(MarkedSquare(tapped, red.copy(alpha = 0.5f)))
                        if (answer != null) add(MarkedSquare(answer, green.copy(alpha = 0.6f)))
                    }
                    Triple(
                        "Not quite — find the piece with the fewest legal moves.",
                        false,
                        marks,
                    )
                }
            }

            is CoachingTrigger.Safety -> {
                val kingSquare = squareFromName(trigger.kingSquare)
                val isKing = kingSquare != null && tapped == kingSquare
                val marks = if (isKing) listOf(MarkedSquare(tapped, green.copy(alpha = 0.6f)))
                            else buildList {
                                add(MarkedSquare(tapped, red.copy(alpha = 0.5f)))
                                if (kingSquare != null) add(MarkedSquare(kingSquare, green.copy(alpha = 0.6f)))
                            }
                Triple(
                    if (isKing) "That's your King — now count the friendly pieces guarding the adjacent squares."
                    else        "That's not your King. Tap the King to assess its exposure.",
                    if (isKing) true else false,
                    marks,
                )
            }

            else -> Triple("Good thinking! Continue reviewing this position.", true, emptyList())
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC — Board Scan Reflection Mode
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Builds the reflection list from all trigger positions the user navigated
     * past without engaging the Coach Lamp, then activates Reflection Mode.
     */
    fun enterReflectionMode() {
        val engaged     = _uiState.value.triggersEngaged
        val triggerMap  = _uiState.value.triggersByMove
        val items       = triggerMap.entries
            .filter { (idx, _) -> idx !in engaged }
            .sortedBy { (idx, _) -> idx }
            .mapNotNull { (idx, triggers) ->
                val trigger = triggers.firstOrNull() ?: return@mapNotNull null
                val fen     = fenSequence.getOrElse(idx) { "" }
                ReflectionItem(
                    moveIndex       = idx,
                    fen             = fen,
                    triggerTypeName = trigger.typeName(),
                    correctLabel    = trigger.displayLabel(),
                )
            }
        if (items.isEmpty()) {
            exitMentorMode()
            return
        }
        _uiState.update {
            it.copy(
                showReflectionMode = true,
                reflectionItems    = items,
            )
        }
    }

    /** Records the user's self-categorization for one reflection item and updates mastery streak. */
    fun answerReflection(moveIndex: Int, answer: String) {
        val updated = _uiState.value.reflectionItems.map { item ->
            if (item.moveIndex == moveIndex) item.copy(userAnswer = answer) else item
        }
        _uiState.update { it.copy(reflectionItems = updated) }

        // Update mastery streak for the trigger type at this position
        val item = _uiState.value.reflectionItems.firstOrNull { it.moveIndex == moveIndex } ?: return
        val typeName  = item.triggerTypeName
        val isCorrect = answer == item.correctLabel
        viewModelScope.launch(Dispatchers.IO) {
            if (isCorrect) masteryRepo.recordCorrect(typeName)
            else           masteryRepo.recordIncorrect(typeName)
        }
    }

    /** Closes Reflection Mode and returns to Navigate. */
    fun exitReflectionMode() {
        _uiState.update {
            it.copy(
                showReflectionMode = false,
                reflectionItems    = emptyList(),
            )
        }
        exitMentorMode()
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
        // Look up the critical moment using the target index (position after the move).
        val moment  = _uiState.value.criticalMoments
            .firstOrNull { it.moveIndex == targetMoveIndex && it.type == CriticalMoment.Type.ENGINE_MARKED.name }
        val reason  = moment?.toReason() ?: CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE
        val insight = InsightReconciler.forReason(reason)
        val clamped = targetMoveIndex.coerceIn(0, uciMoves.size)

        // Navigate to the decision point — the position BEFORE the critical move.
        // At fenSequence[clamped] the opponent is to move (the mistake has already been played),
        // so the user cannot tap their own pieces. fenSequence[clamped - 1] is where the player
        // still needs to choose and move input works correctly.
        val displayIndex = (clamped - 1).coerceAtLeast(0)

        // Build a context label that tells the user *which* move to find
        // WITHOUT revealing the actual move played (which would leak the answer).
        val fullMove = clamped / 2 + 1
        val displayFen = fenSequence.getOrElse(displayIndex) { START_FEN }
        val sideToMove = runCatching {
            Board().apply { loadFromFen(displayFen) }.sideToMove
        }.getOrNull()
        val who = when (sideToMove) {
            Side.WHITE -> "White"
            Side.BLACK -> "Black"
            else       -> "you"
        }
        val contextLabel = "Move $fullMove. — Find the best move for $who"

        viewModelScope.launch(Dispatchers.Default) {
            Log.d("MentorTap", "enterMentorMode coroutine start — current reviewMode=${_uiState.value.reviewMode}")
            applyMoveIndex(displayIndex)
            Log.d("MentorTap", "enterMentorMode after applyMoveIndex — reviewMode=${_uiState.value.reviewMode}")
            _uiState.update {
                it.copy(
                    reviewMode                    = ReviewMode.MENTOR,
                    previousReviewMode            = if (from == ReviewMode.MENTOR) it.previousReviewMode else from,
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
                    showMissedMomentBanner        = false,
                    mentorMoveInputActive         = false,
                    mentorMoveChecking            = false,
                    mentorMoveResult              = null,
                    mentorMoveFeedback            = "",
                    mentorContextLabel            = contextLabel,
                    showClassificationQuiz        = false,
                    classificationOptions         = emptyList(),
                    classificationCorrectIndex    = -1,
                    classificationSelectedIndex   = -1,
                    guidedDiscoveryInsightRevealed = false,
                )
            }
            Log.d("MentorTap", "enterMentorMode complete — reviewMode=${_uiState.value.reviewMode} mentorMoveInputActive=${_uiState.value.mentorMoveInputActive}")
        }
    }

    /** Exits Mentor mode and returns to [previousReviewMode]. All state reset atomically. */
    fun exitMentorMode() {
        Log.d("MentorTap", "exitMentorMode called — current reviewMode=${_uiState.value.reviewMode} mentorMoveInputActive=${_uiState.value.mentorMoveInputActive}", Exception("exitMentorMode stacktrace"))
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
                mentorMoveInputActive         = false,
                mentorMoveChecking            = false,
                mentorMoveResult              = null,
                mentorMoveFeedback            = "",
                mentorContextLabel            = "",
                // Clear session
                mentorSessionQueue            = emptyList(),
                mentorSessionIdx              = 0,
                // Clear classification quiz
                showClassificationQuiz        = false,
                classificationOptions         = emptyList(),
                classificationCorrectIndex    = -1,
                classificationSelectedIndex   = -1,
                guidedDiscoveryInsightRevealed = false,
                // Clear weakness context + pivotal moments
                weaknessContext               = null,
                showCoachsBriefing            = false,
                pivotalMoments                = null,
                showPivotalMomentsPanel       = false,
                // Clear proactive coaching + reflection state
                showProactiveCoaching         = false,
                activeProactiveTrigger        = null,
                triggersEngaged               = emptySet(),
                showReflectionMode            = false,
                reflectionItems               = emptyList(),
                // Clear calibration panel
                showCalibrationPanel          = false,
                calibrationTrigger            = null,
                calibrationUserValue          = 0,
                calibrationLocked             = false,
                calibrationFeedback           = "",
                calibrationFeedbackPositive   = false,
            )
        }
        viewModelScope.launch(Dispatchers.Default) { applyMoveIndex(idx) }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC — Mentor move-input (play your answer on the board)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Toggles "Play your answer" mode in the mentor panel.
     * When active the board accepts piece selection and move attempts.
     * Resets any previous attempt result so the user can try again.
     */
    fun toggleMentorMoveInput() {
        val nowActive = !_uiState.value.mentorMoveInputActive
        Log.d("MentorTap", "toggleMentorMoveInput: nowActive=$nowActive | current reviewMode=${_uiState.value.reviewMode}")
        val idx       = _uiState.value.moveIndex
        val fen       = fenSequence.getOrElse(idx) { START_FEN }
        _uiState.update {
            it.copy(
                mentorMoveInputActive = nowActive,
                mentorMoveChecking    = false,
                mentorMoveResult      = null,
                mentorMoveFeedback    = "",
                boardState            = it.boardState.copy(
                    fen            = fen,      // restore original position if a move was shown
                    lastMove       = null,
                    selectedSquare = null,
                    legalMoves     = emptyList(),
                    // KEY: ChessBoard routes taps through detectTapGestures → onSquareTap only
                    // when isEditorMode = FALSE. When true, the board uses the arrow/mark gesture
                    // handler and onSquareTap is never called. Mentor move-input needs onSquareTap,
                    // so we must keep isEditorMode = false here.
                    isEditorMode   = false,
                ),
            )
        }
        Log.d("MentorTap", "toggleMentorMoveInput done — reviewMode=${_uiState.value.reviewMode} mentorMoveInputActive=${_uiState.value.mentorMoveInputActive}")
    }

    /**
     * Handles a square tap on the board while mentor move-input is active.
     *
     * First tap → selects the piece and shows legal-move highlights.
     * Second tap on a legal target → plays the move and starts engine check.
     * Second tap on an illegal square → deselects.
     */
    fun onMentorSquareTap(square: Square) {
        val st = _uiState.value
        Log.d("MentorTap", "onMentorSquareTap: square=$square | mentorMoveInputActive=${st.mentorMoveInputActive} | mentorMoveChecking=${st.mentorMoveChecking} | reviewMode=${st.reviewMode} | isEditorMode=${st.boardState.isEditorMode}")
        if (!st.mentorMoveInputActive) {
            Log.d("MentorTap", "  → EARLY RETURN: mentorMoveInputActive is false")
            return
        }
        if (st.mentorMoveChecking) {
            Log.d("MentorTap", "  → EARLY RETURN: mentorMoveChecking is true")
            return
        }

        val cur      = st.boardState
        val selected = cur.selectedSquare
        Log.d("MentorTap", "  selectedSquare=$selected  fen=${cur.fen.take(30)}…")

        if (selected == null) {
            val board = Board().apply { loadFromFen(cur.fen) }
            val piece = board.getPiece(square)
            Log.d("MentorTap", "  first tap: piece=$piece sideToMove=${board.sideToMove}")
            if (piece != Piece.NONE && piece.pieceSide == board.sideToMove) {
                val legal = board.legalMoves().filter { it.from == square }
                Log.d("MentorTap", "  selecting piece — ${legal.size} legal moves")
                _uiState.update { it.copy(boardState = cur.copy(selectedSquare = square, legalMoves = legal)) }
            } else {
                Log.d("MentorTap", "  piece is NONE or wrong side — ignoring")
            }
        } else {
            val board = Board().apply { loadFromFen(cur.fen) }
            val move  = ChessUtils.buildMove(board, selected, square, solutionUci = null)
            val isLegal = board.legalMoves().contains(move)
            Log.d("MentorTap", "  second tap: move=$move isLegal=$isLegal")
            if (isLegal) {
                attemptMentorMove(preFen = cur.fen, move = move)
            } else {
                Log.d("MentorTap", "  illegal move — deselecting")
                _uiState.update { it.copy(boardState = cur.copy(selectedSquare = null, legalMoves = emptyList())) }
            }
        }
    }

    /** Resets a failed mentor move attempt so the user can try a different piece. */
    fun retryMentorMove() {
        val idx = _uiState.value.moveIndex
        val fen = fenSequence.getOrElse(idx) { START_FEN }
        _uiState.update {
            it.copy(
                mentorMoveResult      = null,
                mentorMoveFeedback    = "",
                mentorMoveInputActive = true,
                boardState            = it.boardState.copy(
                    fen            = fen,
                    lastMove       = null,
                    selectedSquare = null,
                    legalMoves     = emptyList(),
                    isEditorMode   = false,  // must be false: onSquareTap only fires when isEditorMode=false
                ),
            )
        }
    }

    // ─── Private: check move against engine ───────────────────────────────────

    private fun attemptMentorMove(preFen: String, move: Move) {
        val playerUci  = "${move.from.name.lowercase()}${move.to.name.lowercase()}"
        val postBoard  = Board().apply { loadFromFen(preFen) }
        postBoard.doMove(move)
        val postFen        = postBoard.fen
        val playerWasWhite = postBoard.sideToMove == Side.BLACK  // after White's move it's Black's turn

        // Show the move immediately for visual feedback
        _uiState.update {
            it.copy(
                mentorMoveChecking = true,
                boardState         = it.boardState.copy(
                    fen            = postFen,
                    lastMove       = move,
                    selectedSquare = null,
                    legalMoves     = emptyList(),
                ),
            )
        }

        viewModelScope.launch(Dispatchers.Default) {
            // Best move at the pre-move position
            val preResult  = runCatching {
                engine.analyzePosition(preFen, depth = ChessConstants.DEFAULT_ANALYSIS_DEPTH)
            }.getOrNull()
            val bestUci    = preResult?.bestMoveUci ?: ""
            val preEvalWp  = preResult?.let { r ->
                val b = Board().apply { loadFromFen(preFen) }
                r.toWhitePerspective(b)
            } ?: 0

            // Eval after the player's move
            val postResult = runCatching {
                engine.analyzePosition(postFen, depth = 10)
            }.getOrNull()
            val postEvalWp = postResult?.toWhitePerspective(postBoard) ?: preEvalWp

            // Centipawn loss from the moving player's perspective
            val cpLoss = maxOf(0,
                if (playerWasWhite) preEvalWp - postEvalWp
                else                postEvalWp - preEvalWp
            )

            val result = when {
                playerUci == bestUci -> MentorMoveResult.CORRECT
                cpLoss <= 50         -> MentorMoveResult.CLOSE      // within half a pawn — acceptable
                else                 -> MentorMoveResult.INCORRECT
            }

            val feedback = when (result) {
                MentorMoveResult.CORRECT   ->
                    "✅ That's the engine's best move! Great find."
                MentorMoveResult.CLOSE     ->
                    "👍 Good move — only ${cpLoss}cp from the best. You found the right idea."
                MentorMoveResult.INCORRECT ->
                    "❌ That loses about ${cpLoss}cp. Think again or use a hint."
            }

            if (result == MentorMoveResult.INCORRECT) {
                // Restore original position so the user can retry; keep editor mode on
                _uiState.update {
                    it.copy(
                        mentorMoveChecking             = false,
                        mentorMoveResult               = result,
                        mentorMoveFeedback             = feedback,
                        guidedDiscoveryInsightRevealed = true,
                        boardState                     = it.boardState.copy(
                            fen            = preFen,
                            lastMove       = null,
                            selectedSquare = null,
                            legalMoves     = emptyList(),
                            isEditorMode   = false,
                        ),
                    )
                }
            } else {
                // Correct/close — leave the move on the board; exit move-input mode
                _uiState.update {
                    it.copy(
                        mentorMoveChecking             = false,
                        mentorMoveResult               = result,
                        mentorMoveFeedback             = feedback,
                        mentorMoveInputActive          = false,
                        guidedDiscoveryInsightRevealed = true,
                        boardState                     = it.boardState.copy(isEditorMode = false),
                    )
                }
            }
            // Show the classification quiz after any move attempt so the user
            // reflects on the category of mistake regardless of the outcome.
            triggerClassificationQuiz()
        }
    }

    /**
     * Toggles the evaluation bar visibility.
     * If there is no truth-map entry for the current position, fires an on-demand
     * engine analysis so the bar always shows a value when it is switched on.
     */
    fun toggleEvalBar() {
        val newVisible = !_uiState.value.evalBarVisible
        _uiState.update { it.copy(evalBarVisible = newVisible) }
        if (newVisible && _uiState.value.currentEvalCp == null) {
            val fen = _uiState.value.boardState.fen
            viewModelScope.launch(Dispatchers.Default) { fetchOnDemandEval(fen) }
        }
    }

    /**
     * Toggles best-move arrow visibility.
     * When turning on, fires an engine analysis and draws the best move as an arrow.
     * When navigating while this toggle is on, [applyMoveIndex] refreshes the arrow
     * automatically so it tracks the current position.
     */
    fun toggleBestMove() {
        val newVisible = !_uiState.value.bestMoveVisible
        _uiState.update { it.copy(bestMoveVisible = newVisible) }
        if (newVisible) {
            val fen = _uiState.value.boardState.fen
            viewModelScope.launch(Dispatchers.Default) { fetchOnDemandBestMove(fen) }
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
        _uiState.update { it.copy(guidedDiscoveryHintVisible = true, guidedDiscoveryInsightRevealed = true) }
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
                    guidedDiscoveryEngineThinking  = false,
                    guidedDiscoveryAnswerRevealed  = true,
                    guidedDiscoveryRevealedEvalCp  = evalCp,
                    guidedDiscoveryInsightRevealed = true,
                    boardState = state.boardState.copy(
                        arrows = if (engineArrow != null) listOf(engineArrow) else emptyList(),
                    ),
                )
            }
            // Show the classification quiz when the answer is revealed (if not already shown).
            if (!_uiState.value.showClassificationQuiz) {
                triggerClassificationQuiz()
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
        if (_uiState.value.mentorSessionQueue.isNotEmpty()) {
            advanceMentorSession()
        } else {
            exitMentorMode()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC — Mentor session (top-N mistakes review)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Starts a structured Mentor session built around the "Big Three" pivotal moments.
     *
     * Computes [PivotalMoments] from the hidden truth map, then shows an overview
     * panel ([showPivotalMomentsPanel]) before the user enters the first position.
     * The session queue is derived from the three identified moments.
     */
    fun enterMentorSession() {
        viewModelScope.launch {
            val currentMoments = _uiState.value.criticalMoments
                .filter { it.type == CriticalMoment.Type.ENGINE_MARKED.name && isUserMove(it.moveIndex) }

            if (currentMoments.isEmpty()) return@launch

            // Load cross-game data to identify top weakness
            val allMoments         = withContext(Dispatchers.IO) { criticalMomentDao.getAll() }
            val totalGamesAnalyzed = withContext(Dispatchers.IO) { criticalMomentDao.countGamesAnalyzed() }

            val topTrend      = withContext(Dispatchers.Default) {
                BehavioralDiagnostic.diagnose(allMoments, topN = 1).firstOrNull()
            }
            val topCategories = topTrend?.triggerCategories ?: emptySet()

            // Identify the Big Three pivotal moments
            val pivotalMoments = withContext(Dispatchers.Default) {
                com.acepero13.android.gamereviewer.domain.PivotalMomentsSelector.select(
                    truthMap        = truthMap,
                    criticalMoments = currentMoments,
                    isUserMove      = ::isUserMove,
                )
            }

            // Session queue: pivotal moment indices when available, fallback to severity-sorted list
            val queue = pivotalMoments.moveIndices.ifEmpty {
                buildWeaknessPrioritizedQueue(currentMoments, topCategories)
            }
            if (queue.isEmpty()) return@launch

            val matchingIndices = currentMoments
                .filter { it.toReason() in topCategories }
                .map { it.moveIndex }

            val weaknessCtx = topTrend?.let { trend ->
                WeaknessContext(
                    trendTitle          = trend.title,
                    trendEmoji          = trend.emoji,
                    trendDescription    = trend.description,
                    gamesAffected       = trend.frequency,
                    totalGamesAnalyzed  = totalGamesAnalyzed,
                    matchingMoveIndices = matchingIndices,
                )
            }

            _uiState.update {
                it.copy(
                    mentorSessionQueue      = queue,
                    mentorSessionIdx        = 0,
                    weaknessContext         = weaknessCtx,
                    showCoachsBriefing      = false,    // briefing shown inside pivotal panel instead
                    pivotalMoments          = pivotalMoments,
                    showPivotalMomentsPanel = true,
                    reviewMode              = ReviewMode.MENTOR,
                    previousReviewMode      = it.reviewMode,
                )
            }
            // Navigation is frozen — the user selects which moment to review from the panel.
        }
    }

    /**
     * Dismisses the Pivotal Moments overview panel and begins the session in order,
     * navigating to the first moment in the queue.
     */
    fun dismissPivotalMomentsPanel() {
        _uiState.update { it.copy(showPivotalMomentsPanel = false) }
        val queue = _uiState.value.mentorSessionQueue
        if (queue.isNotEmpty()) {
            enterMentorMode(targetMoveIndex = queue[0])
        } else {
            exitMentorMode()
        }
    }

    /**
     * Jumps directly to a specific pivotal moment chosen by the user from the overview panel.
     * Updates the session index so [advanceMentorSession] continues from the right position.
     */
    fun reviewPivotalMoment(moveIndex: Int) {
        _uiState.update { it.copy(showPivotalMomentsPanel = false) }
        val queue = _uiState.value.mentorSessionQueue
        val idx   = queue.indexOf(moveIndex)
        if (idx >= 0) _uiState.update { it.copy(mentorSessionIdx = idx) }
        enterMentorMode(targetMoveIndex = moveIndex)
    }

    /**
     * Maps the player's top failure archetypes (from [BehavioralDiagnostic]) to the
     * coaching trigger type names they correspond to, so those triggers are promoted
     * by [CoachingTriggerEvaluator] when they fire in the current game.
     */
    private fun buildWeakTriggerTypes(allMoments: List<CriticalMoment>): Set<String> {
        if (allMoments.isEmpty()) return emptySet()
        val trends = BehavioralDiagnostic.diagnose(allMoments, topN = 2)
        return trends.flatMap { trend ->
            trend.triggerCategories.flatMap { category ->
                when (category) {
                    CriticalMoment.ReasonCategory.MISSED_TACTIC,
                    CriticalMoment.ReasonCategory.HANGING_PIECE    -> listOf("PRE_MOVE_CHECKLIST", "FORCING_MOVE")
                    CriticalMoment.ReasonCategory.KING_SAFETY       -> listOf("SAFETY")
                    CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE -> listOf("CANDIDATE_MOVES", "CANDIDATE_SEARCH")
                    CriticalMoment.ReasonCategory.MISSED_WIN        -> listOf("FORCING_MOVE", "CCT_CHECK")
                    CriticalMoment.ReasonCategory.TIME_PRESSURE     -> listOf("IMPULSE_CONTROL", "CALCULATION_BLUNDER")
                    CriticalMoment.ReasonCategory.OPENING_DEVIATION -> listOf("CANDIDATE_MOVES")
                    CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE -> listOf("WORST_PIECE", "ROOK_ACTIVATION")
                }
            }
        }.toSet()
    }

    private fun buildWeaknessPrioritizedQueue(
        moments: List<CriticalMoment>,
        topCategories: Set<CriticalMoment.ReasonCategory>,
    ): List<Int> {
        val weaknessMoments = moments
            .filter { it.toReason() in topCategories }
            .sortedByDescending { it.severity }
        val otherMoments = moments
            .filter { it.toReason() !in topCategories }
            .sortedByDescending { it.severity }
        return (weaknessMoments + otherMoments).take(3).map { it.moveIndex }
    }

    fun dismissCoachsBriefing() {
        _uiState.update { it.copy(showCoachsBriefing = false) }
    }

    /**
     * Advances to the next mistake in the Mentor session queue.
     * If all mistakes have been reviewed the session ends cleanly.
     */
    fun advanceMentorSession() {
        val nextIdx = _uiState.value.mentorSessionIdx + 1
        val queue   = _uiState.value.mentorSessionQueue
        if (nextIdx < queue.size) {
            _uiState.update { it.copy(mentorSessionIdx = nextIdx) }
            enterMentorMode(targetMoveIndex = queue[nextIdx])
        } else {
            // Session complete — clear queue, then offer Board Scan Reflection if applicable
            _uiState.update { it.copy(mentorSessionQueue = emptyList(), mentorSessionIdx = 0) }
            val hasReflectionItems = _uiState.value.triggersByMove
                .keys.any { it !in _uiState.value.triggersEngaged }
            if (hasReflectionItems) enterReflectionMode() else exitMentorMode()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC — Post-move classification quiz
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Generates a 4-option shuffled classification quiz for the current guided-discovery
     * position.  One option is the true [CriticalMoment.ReasonCategory]; the other three
     * are randomly sampled from the remaining 7 categories.
     * No-ops if the current critical moment is unknown.
     */
    fun triggerClassificationQuiz() {
        val moment  = _uiState.value.guidedDiscoveryCriticalMoment ?: return
        val correct = moment.toReason()
        val options = buildClassificationOptions(correct)
        val correctIdx = options.indexOfFirst { it.category == correct }
        _uiState.update {
            it.copy(
                showClassificationQuiz      = true,
                classificationOptions       = options,
                classificationCorrectIndex  = correctIdx,
                classificationSelectedIndex = -1,
            )
        }
    }

    /**
     * Records the user's selected classification option.
     * Appends the selected category label to the position annotation so it is stored.
     */
    fun selectClassificationOption(index: Int) {
        val options = _uiState.value.classificationOptions
        if (index !in options.indices) return
        val selected = options[index]
        _uiState.update { it.copy(classificationSelectedIndex = index) }
        // Persist as a one-line annotation note
        val fen = _uiState.value.boardState.fen
        viewModelScope.launch(Dispatchers.IO) {
            val existing   = getCachedAnnotation(fen)
            val prev       = existing?.moveComment?.takeIf { it.isNotBlank() }
            val quizNote   = "🔖 My assessment: ${selected.label}"
            val newComment = buildString {
                if (prev != null) { appendLine(prev); appendLine() }
                appendLine(quizNote)
            }.trim()
            val upd = (existing ?: PositionAnnotation(fen = fen)).copy(moveComment = newComment)
            annotationDao.upsert(upd)
            annotationCache[fen] = upd
            _uiState.update { it.copy(currentComment = newComment) }
            refreshTreeItems()
        }
    }

    // ─── Private: classification option builder ───────────────────────────────

    private fun buildClassificationOptions(
        correct: CriticalMoment.ReasonCategory,
    ): List<ClassificationOption> {
        val allCategories = CriticalMoment.ReasonCategory.entries.toList()
        val distractors = allCategories
            .filter { it != correct }
            .shuffled()
            .take(3)
        return (listOf(correct) + distractors)
            .shuffled()
            .map { cat ->
                ClassificationOption(
                    label       = categoryLabel(cat),
                    description = categoryDescription(cat),
                    category    = cat,
                )
            }
    }

    private fun categoryLabel(cat: CriticalMoment.ReasonCategory) = when (cat) {
        CriticalMoment.ReasonCategory.MISSED_TACTIC    -> "Missed a tactical pattern"
        CriticalMoment.ReasonCategory.HANGING_PIECE    -> "Left a piece undefended"
        CriticalMoment.ReasonCategory.KING_SAFETY      -> "King safety neglected"
        CriticalMoment.ReasonCategory.OPENING_DEVIATION -> "Opening principle violated"
        CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE -> "Endgame technique error"
        CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE -> "Strategic / positional error"
        CriticalMoment.ReasonCategory.TIME_PRESSURE    -> "Rushed under time pressure"
        CriticalMoment.ReasonCategory.MISSED_WIN       -> "Missed a winning resource"
    }

    private fun categoryDescription(cat: CriticalMoment.ReasonCategory) = when (cat) {
        CriticalMoment.ReasonCategory.MISSED_TACTIC    -> "Fork, pin, skewer or other combination"
        CriticalMoment.ReasonCategory.HANGING_PIECE    -> "Undefended material left en prise"
        CriticalMoment.ReasonCategory.KING_SAFETY      -> "Attack on the king went unnoticed"
        CriticalMoment.ReasonCategory.OPENING_DEVIATION -> "Development or opening-rule breach"
        CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE -> "Incorrect endgame technique"
        CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE -> "Long-term positional weakness created"
        CriticalMoment.ReasonCategory.TIME_PRESSURE    -> "Move played too quickly"
        CriticalMoment.ReasonCategory.MISSED_WIN       -> "Decisive resource or mate was available"
    }

    // ─── Private: player-side helper ──────────────────────────────────────────

    /**
     * Returns true if the move at [moveIndex] was played by the user.
     *
     * White plays odd-indexed moves (1, 3, 5…), Black plays even-indexed moves (2, 4, 6…).
     * When [playerSideKnown] is false (no username set), all moves are treated as the user's
     * so no mistakes are silently hidden.
     */
    private fun isUserMove(moveIndex: Int): Boolean {
        if (!playerSideKnown) return true          // side unknown — include all
        val isBlackMove = moveIndex % 2 == 0
        return boardFlippedForBlack == isBlackMove // true only for the user's own moves
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
                boardState     = it.boardState.copy(isEditorMode = false),
            )
        }
    }

    fun exitSandboxMode() {
        val idx = _uiState.value.moveIndex
        _uiState.update {
            it.copy(
                sandboxMode              = false,
                blunderGuardActive       = false,
                blunderReflectionMode    = false,
                sandboxEngineThinking    = false,
                analyseSubMode           = AnalyseSubMode.VIEW,
                forcingSequenceMode      = false,
                forcingSequenceAnimating = false,
                forcingSequenceComplete  = false,
                forcingSequencePvMoves   = emptyList(),
                forcingSequenceStartFen  = "",
                forcingSequenceCurrentStep = 0,
            )
        }
        viewModelScope.launch(Dispatchers.Default) { applyMoveIndex(idx) }
    }

    // ── Forcing sequence explorer ─────────────────────────────────────────────

    fun enterForcingSequenceMode() {
        val state = _uiState.value
        val pvLine = truthMap.find { it.moveIndex == state.moveIndex }?.pvLine ?: ""
        val pvMoves = pvLine.split(",").filter { it.isNotBlank() }
        val startFen = state.boardState.fen
        _uiState.update {
            it.copy(
                forcingSequenceMode    = true,
                forcingSequencePvMoves = pvMoves,
                forcingSequenceStartFen = startFen,
                forcingSequenceCurrentStep = 0,
                forcingSequenceComplete    = false,
                forcingSequenceAnimating   = false,
            )
        }
        enterSandboxMode()
    }

    fun showForcingSequence() {
        val state    = _uiState.value
        val startFen = state.forcingSequenceStartFen.ifBlank { state.boardState.fen }

        // Re-open panel so animation progress is visible (works in any reviewMode)
        _uiState.update { it.copy(showProactiveCoaching = true) }

        val cachedPv = state.forcingSequencePvMoves.ifEmpty {
            val pvLine = truthMap.find { it.moveIndex == state.moveIndex }?.pvLine ?: ""
            pvLine.split(",").filter { it.isNotBlank() }
        }

        viewModelScope.launch(Dispatchers.Default) {
            val pvMoves = if (cachedPv.isNotEmpty()) {
                cachedPv
            } else {
                // Game was analyzed before pvLine support — fetch PV live from engine
                val result = runCatching {
                    engine.analyzePosition(startFen, ChessConstants.DEFAULT_ANALYSIS_DEPTH)
                }.getOrNull()
                result?.pv?.take(ChessConstants.MAX_FORCING_SEQUENCE_DEPTH)?.filter { it.isNotBlank() }
                    ?: emptyList()
            }
            if (pvMoves.isEmpty()) {
                _uiState.update { it.copy(showProactiveCoaching = false) }
                return@launch
            }
            _uiState.update { it.copy(forcingSequencePvMoves = pvMoves, forcingSequenceStartFen = startFen) }
            animatePvMoves(startFen, pvMoves)
        }
    }

    fun replayForcingSequence() {
        _uiState.update {
            it.copy(
                forcingSequenceCurrentStep = 0,
                forcingSequenceComplete    = false,
            )
        }
        showForcingSequence()
    }

    fun exitForcingSequenceMode() {
        exitSandboxMode()
    }

    private suspend fun animatePvMoves(startFen: String, pvMoves: List<String>) {
        _uiState.update {
            it.copy(
                forcingSequenceAnimating   = true,
                forcingSequenceCurrentStep = 0,
                boardState = it.boardState.copy(fen = startFen, selectedSquare = null, legalMoves = emptyList()),
            )
        }
        val board = Board().apply { loadFromFen(startFen) }
        pvMoves.take(ChessConstants.MAX_FORCING_SEQUENCE_DEPTH).forEachIndexed { index, uci ->
            if (uci.length < 4) return@forEachIndexed
            val from = runCatching { Square.valueOf(uci.substring(0, 2).uppercase()) }.getOrNull() ?: return@forEachIndexed
            val to   = runCatching { Square.valueOf(uci.substring(2, 4).uppercase()) }.getOrNull() ?: return@forEachIndexed
            val move = if (uci.length == 5) {
                val side = board.sideToMove
                val prom = when (uci[4].lowercaseChar()) {
                    'r' -> if (side == Side.WHITE) Piece.WHITE_ROOK   else Piece.BLACK_ROOK
                    'b' -> if (side == Side.WHITE) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
                    'n' -> if (side == Side.WHITE) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
                    else -> if (side == Side.WHITE) Piece.WHITE_QUEEN  else Piece.BLACK_QUEEN
                }
                Move(from, to, prom)
            } else {
                Move(from, to)
            }
            board.doMove(move)
            val newFen = board.fen
            _uiState.update { st ->
                st.copy(
                    boardState = st.boardState.copy(fen = newFen, lastMove = move),
                    forcingSequenceCurrentStep = index + 1,
                )
            }
            delay(ChessConstants.FORCING_SEQUENCE_STEP_DELAY_MS)
        }
        _uiState.update {
            it.copy(
                forcingSequenceAnimating = false,
                forcingSequenceComplete  = true,
            )
        }
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

    /** Cached board-flip flag — set once during loadGame and preserved across navigation. */
    private var boardFlippedForBlack: Boolean = false
    /** True when the username setting matched a player name — meaning we know which side the user played. */
    private var playerSideKnown: Boolean = false

    private fun loadGame() {
        viewModelScope.launch(Dispatchers.IO) {
            val game = repo.findById(gameId) ?: run {
                Log.e(tag, "loadGame: game $gameId NOT FOUND in DB"); return@launch
            }
            Log.d(tag, "loadGame: found game '${game.whitePlayer} vs ${game.blackPlayer}' movesUci.length=${game.movesUci.length} pgn.length=${game.pgn.length}")

            // ── Settings ──────────────────────────────────────────────────────
            val positionCoachEnabled = settingsRepo.positionCoachEnabled.first()
            val developerModeEnabled = settingsRepo.developerModeEnabled.first()
            _uiState.update {
                it.copy(
                    positionCoachEnabled = positionCoachEnabled,
                    developerModeEnabled = developerModeEnabled,
                    gameStoryUnlocked    = it.gameStoryUnlocked || positionCoachEnabled,
                )
            }

            // ── Board orientation — flip when the user played as Black ─────────
            val username = settingsRepo.username.first().trim()
            boardFlippedForBlack = username.isNotEmpty() &&
                game.blackPlayer.equals(username, ignoreCase = true) &&
                !game.whitePlayer.equals(username, ignoreCase = true)
            playerSideKnown = username.isNotEmpty() &&
                (game.whitePlayer.equals(username, ignoreCase = true) ||
                 game.blackPlayer.equals(username, ignoreCase = true))
            Log.d(tag, "loadGame: username='$username' flippedForBlack=$boardFlippedForBlack playerSideKnown=$playerSideKnown")

            // If movesUci is blank the game was imported before the PGN-comment-stripping
            // fix landed.  Re-parse on the fly from the stored raw PGN so the user doesn't
            // need to re-import.
            val rawUci = if (game.movesUci.isNotBlank()) {
                Log.d(tag, "loadGame: using stored movesUci")
                game.movesUci
            } else {
                Log.w(tag, "loadGame: movesUci is blank — falling back to raw PGN parse")
                extractUciMovesFromFullPgn(game.pgn)
            }
            uciMoves = rawUci.split(' ').filter { it.isNotBlank() }
            Log.d(tag, "loadGame: uciMoves.size=${uciMoves.size}  first3=${uciMoves.take(3)}")
            buildFenAndSanSequence()
            Log.d(tag, "loadGame: fenSequence.size=${fenSequence.size}  sanMoves.size=${sanMoves.size}")

            // Pre-warm annotation cache so navigation never needs runBlocking on the main thread.
            for (fen in fenSequence) {
                if (!annotationCache.containsKey(fen)) {
                    annotationCache[fen] = annotationDao.getByFen(fen)
                }
            }

            val storedMoments = criticalMomentDao.getByGameId(gameId)
            val openingEntry  = runCatching { opening.classifyByMoves(uciMoves.take(20)) }.getOrNull()
            val deviation     = withContext(Dispatchers.Default) {
                runCatching { deviationAnalyzer.analyze(uciMoves, sanMoves) }.getOrNull()
            }
            Log.d(tag, "loadGame: openingDeviation=$deviation")

            val endgameClassification = withContext(Dispatchers.Default) {
                runCatching { endgameRecognizer.analyze(fenSequence) }.getOrNull()
            }
            Log.d(tag, "loadGame: endgameClassification=$endgameClassification")
            endgameClassification?.let { ec ->
                val existing = endgameEncounterDao.getByGameId(gameId)
                if (existing == null) {
                    endgameEncounterDao.upsert(
                        com.acepero13.android.gamereviewer.data.model.EndgameEncounter(
                            gameId    = gameId,
                            moveIndex = ec.firstEndgameMoveIndex,
                            chapter   = ec.entry.chapter,
                            category  = ec.entry.category,
                            name      = ec.entry.name,
                            fen       = ec.fen,
                        )
                    )
                }
            }

            val middlegamePlanClassification = withContext(Dispatchers.Default) {
                val startIndex = (deviation?.moveIndex ?: 20).coerceIn(fenSequence.indices)
                runCatching {
                    middlegamePlanDetector.detect(
                        fens           = fenSequence,
                        playerIsWhite  = !boardFlippedForBlack,
                        startFromIndex = startIndex,
                    )
                }.getOrNull()
            }
            Log.d(tag, "loadGame: middlegamePlanClassification=$middlegamePlanClassification")

            _uiState.update {
                it.copy(
                    game                         = game,
                    totalMoves                   = uciMoves.size,
                    openingSummary               = openingEntry?.let { e -> "${e.eco} · ${e.name}" } ?: "",
                    phaseSummary                 = buildPhaseSummary(),
                    criticalMoments              = storedMoments,
                    openingDeviation             = deviation,
                    endgameClassification        = endgameClassification,
                    middlegamePlanClassification = middlegamePlanClassification,
                )
            }
            applyMoveIndex(0)
            _uiState.update { it.copy(showPredictionGate = true) }
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
                Log.e(tag, "buildFenAndSanSequence: uciToMove returned null for '$uci' at index $idx — stopping")
                break
            }
            val applied = board.doMove(move)
            if (!applied) {
                Log.e(tag, "buildFenAndSanSequence: board.doMove($uci) returned FALSE at index $idx — stopping")
                break
            }
            fens.add(board.fen)
        }
        Log.d(tag, "buildFenAndSanSequence: built ${fens.size} FENs for ${uciMoves.size} moves")
        fenSequence = fens
        sanMoves    = sans
    }

    private fun applyMoveIndex(index: Int) {
        val fen = fenSequence.getOrElse(index) { START_FEN }
        Log.d(tag, "applyMoveIndex($index): fenSequence.size=${fenSequence.size}  fen=${fen.take(40)}")
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

        // Proactive trigger for this position (first trigger in the list if any)
        val positionTrigger = _uiState.value.triggersByMove[index]?.firstOrNull()

        // Opening deviation panel — show automatically when user arrives at the deviation move
        val shouldShowDeviationPanel =
            live.reviewMode == ReviewMode.NAVIGATE &&
            !live.openingDeviationDismissed &&
            live.openingDeviation?.moveIndex == index &&
            index > 0

        // Endgame recognition panel — show when user arrives at the first endgame move
        val shouldShowEndgamePanel =
            live.reviewMode == ReviewMode.NAVIGATE &&
            !live.endgamePanelDismissed &&
            live.endgameClassification?.firstEndgameMoveIndex == index &&
            index > 0

        // Middlegame plan panel — show at the detected middlegame start move
        val shouldShowMiddlegamePanel =
            live.reviewMode == ReviewMode.NAVIGATE &&
            !live.middlegamePlanPanelDismissed &&
            !shouldShowDeviationPanel &&
            live.middlegamePlanClassification?.moveIndex == index &&
            index > 0

        _uiState.update { s ->
            s.copy(
                moveIndex  = index,
                boardState = s.boardState.copy(
                    fen              = fen,
                    lastMove         = lastMove,
                    selectedSquare   = null,
                    legalMoves       = emptyList(),
                    userArrows       = arrows,
                    markedSquares    = marks,
                    arrows           = emptyList(),
                    isEditorMode     = inEditMode,
                    showingFlash     = false,
                    flippedForBlack  = boardFlippedForBlack,
                ),
                currentComment         = comment,
                hasAnnotationAtCurrent = comment.isNotBlank() || arrows.isNotEmpty() || marks.isNotEmpty(),
                treeItems              = buildTreeItems(index),
                blunderGuardActive     = false,
                mentorAvailable        = computeMentorAvailable(index),
                currentEvalCp          = evalCp,
                showOpeningDeviationPanel    = shouldShowDeviationPanel,
                showEndgameRecognitionPanel  = shouldShowEndgamePanel,
                showMiddlegamePlanPanel      = shouldShowMiddlegamePanel,
                // Dismiss any open coaching panel when navigating; the lamp icon
                // will re-light if the new position also has triggers.
                showProactiveCoaching    = false,
                activeProactiveTrigger   = null,
                proactiveInteractiveMode = false,
                proactiveAnswerFeedback  = null,
                proactiveAnswerIsCorrect = null,
                coachHighlightSquares    = emptyList(),
                proactiveHangingSquares     = emptyList(),
                proactiveHangingOwnSquares  = emptySet(),
                proactiveFoundSquares       = emptySet(),
                // Dismiss calibration panel when navigating
                showCalibrationPanel        = false,
                calibrationTrigger          = null,
                calibrationUserValue        = 0,
                calibrationLocked           = false,
                calibrationFeedback         = "",
                calibrationFeedbackPositive = false,
            )
        }
        Log.d(tag, "applyMoveIndex($index): state updated — moveIndex=${_uiState.value.moveIndex} fen=${_uiState.value.boardState.fen.take(40)}")

        // Trigger post-game debrief when user reaches the final position
        val st = _uiState.value
        if (index == uciMoves.size && st.isBackgroundAnalysisDone && !st.showPostGameDebrief) {
            val result = buildDebrief(st.gamePrediction, st.criticalMoments)
            _uiState.update { it.copy(showPostGameDebrief = true, predictionMatchResult = result, gameStoryUnlocked = true) }
        }

        // Keep engine-toggle overlays in sync when navigating in Analyse mode
        val updated = _uiState.value
        if (updated.reviewMode == ReviewMode.ANALYSE) {
            if (updated.evalBarVisible && evalCp == null) {
                // Truth map has no entry for this position — fetch eval on demand
                viewModelScope.launch(Dispatchers.Default) { fetchOnDemandEval(fen) }
            }
            if (updated.bestMoveVisible) {
                // Always refresh the best-move arrow for the new position
                viewModelScope.launch(Dispatchers.Default) { fetchOnDemandBestMove(fen) }
            }
        }
    }

    /**
     * Mentor is available at any non-starting position once a game is loaded.
     *
     * When a critical moment exists for this index the panel shows targeted questions;
     * otherwise it falls back to a general strategic-reflection insight.
     * This ensures the coach is always reachable — not just at engine-flagged moves.
     */
    private fun computeMentorAvailable(index: Int): Boolean {
        return index > 0 && uciMoves.isNotEmpty()
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
                            pvLine    = ev.pvLine,
                        )
                    }
                }
                // Re-evaluate coaching triggers from stored evaluations so that the latest
                // detection logic (including exchange-aware hanging-piece checks) is always
                // applied — stored coachingTriggers strings from earlier app versions may
                // contain false positives that the current evaluator would correctly suppress.
                val mastered    = masteryRepo.getMasteredTypes()
                val moveTimes   = moveTimeDao.getByGameId(gameId).associateBy { it.moveIndex }
                val allMoments  = criticalMomentDao.getAll()
                val weakTypes   = buildWeakTriggerTypes(allMoments)
                val allTriggers = withContext(Dispatchers.Default) {
                    CoachingTriggerEvaluator.evaluate(
                        evaluations      = dbEvals,
                        fenByMoveIndex   = { idx -> fenSequence.getOrElse(idx) { "" } },
                        timeByMoveIndex  = { idx -> moveTimes[idx]?.timeSpentSeconds },
                        playerIsWhite    = !boardFlippedForBlack,
                        gameId           = gameId,
                        weakTriggerTypes = weakTypes,
                    )
                }
                Log.d(tag, "CoachTrigger (DB restore re-eval): ${allTriggers.values.sumOf { it.size }} triggers across ${allTriggers.size} positions  mastered=$mastered")
                val restoredTriggers: Map<Int, List<CoachingTrigger>> = allTriggers
                    .mapValues { (_, triggers) -> triggers.filter { it.typeName() !in mastered } }
                    .filter { (_, triggers) -> triggers.isNotEmpty() }
                Log.d(tag, "CoachTrigger (DB restore): restoredTriggers.size=${restoredTriggers.size}  keys=${restoredTriggers.keys.take(10)}")

                // Run highlights against the restored truth map (moveTimes already fetched above)
                val highlights = GameHighlightEngine.run(
                    truthMap    = truthMap,
                    sanMoves    = sanMoves,
                    fenSequence = fenSequence,
                    moveTimes   = moveTimes,
                )
                _uiState.update { st ->
                    val story       = buildGameStory(st.criticalMoments, uciMoves.size)
                    val atEnd       = st.moveIndex == uciMoves.size && !st.showPostGameDebrief
                    val matchResult = if (atEnd) buildDebrief(st.gamePrediction, st.criticalMoments) else st.predictionMatchResult
                    st.copy(
                        isBackgroundAnalysisDone   = true,
                        backgroundAnalysisProgress = 1f,
                        gameHighlights             = highlights,
                        triggersByMove             = restoredTriggers,
                        weakTriggerTypes           = weakTypes,
                        gameStory                  = story,
                        showPostGameDebrief        = st.showPostGameDebrief || atEnd,
                        predictionMatchResult      = matchResult,
                        gameStoryUnlocked          = st.gameStoryUnlocked || atEnd,
                    )
                }
                // Check if the user already navigated past unreviewed critical moments
                // before the analysis data became available.
                val currentIdx = _uiState.value.moveIndex
                if (currentIdx > 0) checkMissedMoments(0, currentIdx)
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
                    pvLine    = entry.pvLine,
                )
            }
            if (evaluations.isNotEmpty()) {
                launch(Dispatchers.IO) { gameEvaluationDao.insertAll(evaluations) }
            }

            // Fetch move-time data (already persisted during import) for impulse-control detection
            val moveTimes = withContext(Dispatchers.IO) {
                moveTimeDao.getByGameId(gameId).associateBy { it.moveIndex }
            }

            // Load cross-game weak areas for recency-bias coaching promotion
            val allMoments = withContext(Dispatchers.IO) { criticalMomentDao.getAll() }
            val weakTypes  = buildWeakTriggerTypes(allMoments)

            // Detect proactive coaching triggers from the fresh truth map
            val allTriggers = CoachingTriggerEvaluator.evaluate(
                evaluations      = evaluations,
                fenByMoveIndex   = { idx -> fenSequence.getOrElse(idx) { "" } },
                timeByMoveIndex  = { idx -> moveTimes[idx]?.timeSpentSeconds },
                playerIsWhite    = !boardFlippedForBlack,
                gameId           = gameId,
                weakTriggerTypes = weakTypes,
            )
            Log.d(tag, "CoachTrigger: ${allTriggers.values.sumOf { it.size }} raw triggers across ${allTriggers.size} positions — by type: ${allTriggers.values.flatten().groupBy { it.typeName() }.mapValues { it.value.size }}")
            Log.d(tag, "CoachTrigger: fenSequence.size=${fenSequence.size}  evaluations.size=${evaluations.size}  sampleEvalDeltas=${evaluations.take(5).map { it.evalDelta }}  sampleMotifs=${evaluations.take(5).map { it.motif }}")

            // Filter out trigger types the user has already mastered
            val masteredTypes  = masteryRepo.getMasteredTypes()
            Log.d(tag, "CoachTrigger: masteredTypes=$masteredTypes")
            val triggerMap     = allTriggers.mapValues { (_, triggers) ->
                triggers.filter { it.typeName() !in masteredTypes }
            }.filter { (_, triggers) -> triggers.isNotEmpty() }
            Log.d(tag, "CoachTrigger: after mastery filter — ${triggerMap.size} positions remain, keys=${triggerMap.keys.take(10)}")

            _uiState.update { it.copy(triggersByMove = triggerMap, weakTriggerTypes = weakTypes) }
            // Persist the full (unfiltered) trigger data for future sessions
            if (allTriggers.isNotEmpty()) {
                launch(Dispatchers.IO) {
                    allTriggers.forEach { (moveIndex, triggers) ->
                        val encoded = triggers.joinToString(",") { it.typeName() }
                        gameEvaluationDao.updateTriggers(gameId, moveIndex, encoded)
                    }
                }
            }

            val moments = map.filter { it.isCritical || it.isSignificantTacticalMiss }.map { entry ->
                CriticalMoment(
                    gameId           = gameId,
                    moveIndex        = entry.moveIndex,
                    type             = CriticalMoment.Type.ENGINE_MARKED.name,
                    severity         = kotlin.math.abs(entry.playerEvalDelta),
                    reasonCategory   = motifToReason(entry),
                    explanationState = CriticalMoment.ExplanationState.HIDDEN.name,
                    fen              = entry.fen,
                )
            }
            if (moments.isNotEmpty()) {
                launch(Dispatchers.IO) {
                    criticalMomentDao.insertAll(moments)
                    val hasEndgameMistake = moments.any {
                        it.reasonCategory == com.acepero13.android.gamereviewer.data.model.CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE.name
                    }
                    if (hasEndgameMistake) {
                        endgameEncounterDao.markMistake(gameId)
                    }
                }
            }

            // Run highlight rules against the completed truth map (moveTimes already fetched above)
            val highlights = GameHighlightEngine.run(
                truthMap    = map,
                sanMoves    = sanMoves,
                fenSequence = fenSequence,
                moveTimes   = moveTimes,
            )

            val freshMoments = criticalMomentDao.getByGameId(gameId)
            _uiState.update { st ->
                val story       = buildGameStory(freshMoments, uciMoves.size)
                val atEnd       = st.moveIndex == uciMoves.size && !st.showPostGameDebrief
                val matchResult = if (atEnd) buildDebrief(st.gamePrediction, freshMoments) else st.predictionMatchResult
                st.copy(
                    isBackgroundAnalysisDone   = true,
                    backgroundAnalysisProgress = 1f,
                    criticalMoments            = freshMoments,
                    gameHighlights             = highlights,
                    gameStory                  = story,
                    showPostGameDebrief        = st.showPostGameDebrief || atEnd,
                    predictionMatchResult      = matchResult,
                    gameStoryUnlocked          = st.gameStoryUnlocked || atEnd,
                )
            }
            // Check if the user navigated past unreviewed critical positions while analysis ran
            val currentIdx = _uiState.value.moveIndex
            if (currentIdx > 0) checkMissedMoments(0, currentIdx)
        }
    }

    // ─── Missed Moment detection ──────────────────────────────────────────────

    private fun checkMissedMoments(fromIndex: Int, toIndex: Int) {
        if (truthMap.isEmpty() && _uiState.value.criticalMoments.isEmpty()) return

        val criticalIndices = if (truthMap.isNotEmpty()) {
            truthMap.filter { it.isCritical || it.isSignificantTacticalMiss }.map { it.moveIndex }.toSet()
        } else {
            _uiState.value.criticalMoments
                .filter { it.type == CriticalMoment.Type.ENGINE_MARKED.name }
                .map { it.moveIndex }.toSet()
        }

        for (idx in (fromIndex + 1)..toIndex) {
            if (idx !in criticalIndices) continue
            // Only show the banner for moves the USER played, not opponent's blunders.
            if (!isUserMove(idx)) continue
            val fen     = fenSequence.getOrElse(idx) { continue }
            val annot   = getCachedAnnotation(fen)
            val reviewed = (annot?.moveComment?.isNotBlank() == true) ||
                (annot?.arrowsJson?.length ?: 0) > 2
            if (!reviewed) {
                val category = getCategoryForMove(idx)
                val lastAt   = _uiState.value.shownCategoryAtMove[category]
                if (lastAt != null && idx - lastAt <= CATEGORY_COOLDOWN_MOVES) continue
                _uiState.update {
                    it.copy(
                        showMissedMomentBanner = true,
                        missedMomentMoveIndex  = idx,
                        shownCategoryAtMove    = it.shownCategoryAtMove + (category to idx),
                    )
                }
                return
            }
        }
    }

    private fun getCategoryForMove(idx: Int): String =
        if (truthMap.isNotEmpty())
            truthMap.firstOrNull { it.moveIndex == idx }
                ?.let { motifToReason(it) }
                ?: CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE.name
        else
            _uiState.value.criticalMoments
                .firstOrNull { it.moveIndex == idx }?.reasonCategory
                ?: CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE.name

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

        // Optimistically show the move; clear stale best-move arrow immediately
        _uiState.update {
            it.copy(
                boardState = it.boardState.copy(
                    fen            = postFen,
                    lastMove       = move,
                    selectedSquare = null,
                    legalMoves     = emptyList(),
                    showingFlash   = false,
                    arrows         = emptyList(),
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
                if (_uiState.value.bestMoveVisible) fetchOnDemandBestMove(postFen)
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
                            boardState = it.boardState.copy(fen = replyFen, lastMove = move, arrows = emptyList()),
                            sandboxEngineThinking = false,
                        )
                    }
                    if (_uiState.value.bestMoveVisible) fetchOnDemandBestMove(replyFen)
                    return@launch
                }
            }
            _uiState.update { it.copy(sandboxEngineThinking = false) }
        }
    }

    // ─── On-demand engine helpers (eval bar / best move) ─────────────────────

    /**
     * Runs engine analysis for [fen] and updates [AnalysisUiState.currentEvalCp].
     * No-ops if the user has already navigated away or toggled off the eval bar.
     */
    private suspend fun fetchOnDemandEval(fen: String) {
        val result = runCatching {
            engine.analyzePosition(fen, depth = ChessConstants.DEFAULT_ANALYSIS_DEPTH)
        }.getOrNull() ?: run {
            Log.w(tag, "fetchOnDemandEval: engine returned null for fen=${fen.take(40)}")
            return
        }
        val board  = Board().apply { loadFromFen(fen) }
        val evalCp = result.toWhitePerspective(board)
        // Guard: only apply if the user is still on the same position
        if (_uiState.value.boardState.fen == fen && _uiState.value.evalBarVisible) {
            _uiState.update { it.copy(currentEvalCp = evalCp) }
        }
    }

    /**
     * Runs engine analysis for [fen] and draws the best-move as a green arrow.
     * No-ops if the user has navigated away or toggled off the best-move overlay.
     */
    private suspend fun fetchOnDemandBestMove(fen: String) {
        val result = runCatching {
            engine.analyzePosition(fen, depth = 10)
        }.getOrNull() ?: run {
            Log.w(tag, "fetchOnDemandBestMove: engine returned null for fen=${fen.take(40)}")
            return
        }
        val arrow = result.toArrow()
        // Guard: only apply if the user is still on the same position
        if (_uiState.value.boardState.fen == fen && _uiState.value.bestMoveVisible) {
            _uiState.update { s ->
                s.copy(
                    boardState = s.boardState.copy(
                        arrows = if (arrow != null) listOf(arrow) else emptyList(),
                    ),
                )
            }
            if (arrow == null) {
                Log.w(tag, "fetchOnDemandBestMove: no valid arrow for fen=${fen.take(40)} bestMove='${result.bestMoveUci}'")
            }
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

    /**
     * Maps a [TruthMapEntry] to the most appropriate [CriticalMoment.ReasonCategory].
     *
     * Priority order:
     * 1. Tactical motifs from [MotifClassifier] — highest confidence.
     * 2. Opening phase heuristic (moveIndex ≤ 20 half-moves ≈ first 10 moves each side).
     * 3. Endgame detection from FEN material count (no queens + ≤ 4 minor/rook pieces).
     * 4. Severe positional collapse (≥ 4 pawns lost in a single move) → missed winning chance.
     * 5. Default: STRATEGIC_MISTAKE.
     */
    private fun motifToReason(entry: TruthMapEntry): String = when {
        entry.motif == "checkmate"              -> CriticalMoment.ReasonCategory.MISSED_WIN.name
        entry.motif == "hanging"                -> CriticalMoment.ReasonCategory.HANGING_PIECE.name
        entry.motif == "fork"                   -> CriticalMoment.ReasonCategory.MISSED_TACTIC.name
        // Opening phase — first 20 half-moves (roughly moves 1–10 per side)
        entry.moveIndex <= 20                   -> CriticalMoment.ReasonCategory.OPENING_DEVIATION.name
        // Endgame: no queens remaining and combined major+minor material ≤ 4 pieces
        isEndgamePosition(entry.fen)            -> CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE.name
        // Blunder that swings ≥ 4 pawns in a single move — likely a decisive resource was missed
        entry.playerEvalDelta <= -400           -> CriticalMoment.ReasonCategory.MISSED_WIN.name
        else                                    -> CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE.name
    }

    /**
     * Returns true when the position represented by [fen] is an endgame:
     * both queens are gone **and** the total minor + rook count ≤ 4.
     *
     * Intentionally uses simple character counting on the piece-placement field
     * to avoid constructing a full [com.github.bhlangonijr.chesslib.Board] object.
     */
    private fun isEndgamePosition(fen: String): Boolean {
        val placement = fen.substringBefore(' ')
        val queens    = placement.count { it == 'Q' || it == 'q' }
        val rooks     = placement.count { it == 'R' || it == 'r' }
        val minors    = placement.count { it in "BbNn" }
        return queens == 0 && (rooks + minors) <= 4
    }

    // ─── Game story & debrief helpers ─────────────────────────────────────────

    private fun buildGameStory(moments: List<CriticalMoment>, totalHalfMoves: Int): String {
        val userMoments = moments.filter {
            it.type == CriticalMoment.Type.ENGINE_MARKED.name && isUserMove(it.moveIndex)
        }
        if (userMoments.isEmpty()) return "Solid game — the engine found no major errors."
        val worst        = userMoments.maxByOrNull { it.severity }!!
        val worstMoveNum = worst.moveIndex / 2 + 1
        val phase = when {
            worst.moveIndex < 20                          -> "the opening"
            worst.moveIndex < totalHalfMoves * 2 / 3     -> "the middlegame"
            else                                          -> "the endgame"
        }
        val topCatName = dominantCategory(userMoments)?.let { categoryLabel(it) }
        return when (userMoments.size) {
            1    -> "The game hinged on one moment — around move $worstMoveNum in $phase."
            2    -> "Two errors shaped this game. Biggest shift: move $worstMoveNum in $phase."
            else -> "${userMoments.size} errors detected. Biggest: move $worstMoveNum.${topCatName?.let { " Pattern: $it." } ?: ""}"
        }
    }

    private fun buildDebrief(
        prediction: GamePrediction?,
        moments:    List<CriticalMoment>,
    ): PredictionMatchResult {
        val userMoments = moments.filter {
            it.type == CriticalMoment.Type.ENGINE_MARKED.name && isUserMove(it.moveIndex)
        }
        if (prediction == null || prediction == GamePrediction.NOT_SURE) {
            if (userMoments.isEmpty()) {
                return PredictionMatchResult(isAccurate = true, headline = "Solid game — no major errors detected.")
            }
            val worst = userMoments.maxByOrNull { it.severity }!!
            return PredictionMatchResult(
                isAccurate = true,
                headline   = dominantCategory(userMoments)?.let { "Main pattern: ${categoryLabel(it)}." }
                             ?: "${userMoments.size} error(s) detected.",
                detail     = "Biggest moment: move ${worst.moveIndex / 2 + 1}.",
            )
        }
        return when (prediction) {
            GamePrediction.SPECIFIC_BLUNDER -> {
                val blunders = userMoments.filter { it.severity >= 150 }
                if (blunders.isNotEmpty()) {
                    val worst = blunders.maxByOrNull { it.severity }!!
                    PredictionMatchResult(
                        isAccurate = true,
                        headline   = "Your read was right — blunder on move ${worst.moveIndex / 2 + 1}.",
                        detail     = if (blunders.size > 1) "${blunders.size} blunders total (${worst.severity}cp worst)." else "${worst.severity}cp loss.",
                    )
                } else {
                    PredictionMatchResult(
                        isAccurate = false,
                        headline   = "No clean blunder — errors were more subtle.",
                        detail     = dominantCategory(userMoments)?.let { "Main pattern: ${categoryLabel(it)}." },
                    )
                }
            }
            GamePrediction.TIME_PRESSURE -> {
                val timeMoments = userMoments.filter {
                    it.reasonCategory == CriticalMoment.ReasonCategory.TIME_PRESSURE.name
                }
                if (timeMoments.isNotEmpty()) {
                    PredictionMatchResult(
                        isAccurate = true,
                        headline   = "Confirmed — ${timeMoments.size} time-pressure mistake(s).",
                    )
                } else {
                    PredictionMatchResult(
                        isAccurate = false,
                        headline   = "Time wasn't the main issue here.",
                        detail     = dominantCategory(userMoments)?.let { "Main pattern: ${categoryLabel(it)}." },
                    )
                }
            }
            GamePrediction.OUTPLAYED_POSITIONALLY -> {
                val positional = userMoments.filter {
                    it.reasonCategory in listOf(
                        CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE.name,
                        CriticalMoment.ReasonCategory.OPENING_DEVIATION.name,
                    )
                }
                if (positional.isNotEmpty()) {
                    PredictionMatchResult(
                        isAccurate = true,
                        headline   = "Correct — ${positional.size} strategic error(s) detected.",
                    )
                } else {
                    PredictionMatchResult(
                        isAccurate = false,
                        headline   = "The problem was more tactical than positional.",
                        detail     = dominantCategory(userMoments)?.let { "Main pattern: ${categoryLabel(it)}." },
                    )
                }
            }
            GamePrediction.NOT_SURE -> error("handled above")
        }
    }

    private fun dominantCategory(moments: List<CriticalMoment>): CriticalMoment.ReasonCategory? =
        moments.groupBy { it.reasonCategory }
            .maxByOrNull { it.value.size }?.key
            ?.let { runCatching { CriticalMoment.ReasonCategory.valueOf(it) }.getOrNull() }
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
