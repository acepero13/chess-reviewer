package com.acepero13.android.gamereviewer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.db.CriticalMomentDao
import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.domain.InsightReconciler
import com.acepero13.chess.core.data.model.ChessConstants
import com.acepero13.chess.core.engine.EngineResult
import com.acepero13.chess.core.engine.StockfishEngine
import com.acepero13.chess.core.ui.board.Arrow
import com.acepero13.chess.core.ui.board.BoardState
import com.acepero13.chess.core.util.ChessUtils
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DRILL_START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

data class DrillItem(
    val gameId:    Long,
    val moveIndex: Int,
    val preFen:    String,
    val category:  CriticalMoment.ReasonCategory,
    val severity:  Int,
    val gameLabel: String,
)

data class WeaknessDrillUiState(
    val isLoading:   Boolean = true,
    val drillTitle:  String  = "",
    val drillEmoji:  String  = "",
    val queue:       List<DrillItem> = emptyList(),
    val currentIdx:  Int     = 0,
    val isComplete:  Boolean = false,
    // GuidedDiscovery fields (mirror of AnalysisUiState mentor fields)
    val insight:                        InsightReconciler.Insight? = null,
    val guidedDiscoveryInsightRevealed: Boolean = false,
    val guidedDiscoveryThoughts:        String  = "",
    val guidedDiscoveryHintVisible:     Boolean = false,
    val guidedDiscoveryAnswerRevealed:  Boolean = false,
    val guidedDiscoveryEngineThinking:  Boolean = false,
    val guidedDiscoveryRevealedEvalCp:  Int?    = null,
    val mentorMoveInputActive:          Boolean = false,
    val mentorMoveChecking:             Boolean = false,
    val mentorMoveResult:               MentorMoveResult? = null,
    val mentorMoveFeedback:             String  = "",
    val showClassificationQuiz:         Boolean = false,
    val classificationOptions:          List<ClassificationOption> = emptyList(),
    val classificationCorrectIndex:     Int = -1,
    val classificationSelectedIndex:    Int = -1,
    val boardState:                     BoardState = BoardState(fen = DRILL_START_FEN),
)

class WeaknessDrillViewModel(
    private val categoryNames: List<String>,
    private val criticalMomentDao: CriticalMomentDao,
    private val repo: GameRepository,
    private val engine: StockfishEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeaknessDrillUiState())
    val uiState: StateFlow<WeaknessDrillUiState> = _uiState.asStateFlow()

    init {
        loadDrillQueue()
    }

    private fun loadDrillQueue() {
        viewModelScope.launch {
            val targetCategories = categoryNames
                .mapNotNull { name ->
                    runCatching { CriticalMoment.ReasonCategory.valueOf(name) }.getOrNull()
                }
                .toSet()

            if (targetCategories.isEmpty()) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            val allMoments = withContext(Dispatchers.IO) { criticalMomentDao.getAll() }

            // Filter to target categories, engine-marked only, sorted by severity
            val filtered = allMoments
                .filter {
                    it.type == CriticalMoment.Type.ENGINE_MARKED.name &&
                    it.toReason() in targetCategories
                }
                .sortedByDescending { it.severity }

            // Cache: gameId → movesUci splits
            val gameMoveCache = mutableMapOf<Long, List<String>>()
            val gameLabelCache = mutableMapOf<Long, String>()

            val drillItems = withContext(Dispatchers.Default) {
                filtered.mapNotNull { moment ->
                    val uciMoves = gameMoveCache.getOrPut(moment.gameId) {
                        val game = withContext(Dispatchers.IO) { repo.findById(moment.gameId) }
                            ?: return@mapNotNull null
                        val label = "${game.whitePlayer} vs ${game.blackPlayer}"
                            .takeIf { it.isNotBlank() } ?: "Game"
                        gameLabelCache[moment.gameId] = label
                        game.movesUci.split(" ").filter { it.isNotBlank() }
                    }

                    val preFen = replayToPreMoveFen(uciMoves, moment.moveIndex)
                        ?: return@mapNotNull null

                    DrillItem(
                        gameId    = moment.gameId,
                        moveIndex = moment.moveIndex,
                        preFen    = preFen,
                        category  = moment.toReason(),
                        severity  = moment.severity,
                        gameLabel = gameLabelCache[moment.gameId] ?: "Game",
                    )
                }
            }

            val firstInsight = drillItems.firstOrNull()?.let {
                InsightReconciler.forReason(it.category)
            }

            _uiState.update {
                it.copy(
                    isLoading  = false,
                    queue      = drillItems,
                    currentIdx = 0,
                    insight    = firstInsight,
                    boardState = BoardState(
                        fen          = drillItems.firstOrNull()?.preFen ?: DRILL_START_FEN,
                        isEditorMode = false,
                    ),
                )
            }
        }
    }

    // Move index is 1-based. The FEN before move N is the state after replaying moves 0..(N-2).
    private fun replayToPreMoveFen(uciMoves: List<String>, moveIndex: Int): String? {
        return runCatching {
            val board = Board().apply { loadFromFen(DRILL_START_FEN) }
            val movesToPlay = moveIndex - 1  // play moves before the mistake
            repeat(movesToPlay.coerceAtMost(uciMoves.size)) { i ->
                board.doMove(uciMoves[i])
            }
            board.fen
        }.getOrNull()
    }

    fun advanceDrill() {
        val nextIdx = _uiState.value.currentIdx + 1
        val queue   = _uiState.value.queue
        if (nextIdx >= queue.size) {
            _uiState.update { it.copy(isComplete = true) }
            return
        }
        val next = queue[nextIdx]
        _uiState.update {
            it.copy(
                currentIdx                  = nextIdx,
                insight                     = InsightReconciler.forReason(next.category),
                guidedDiscoveryInsightRevealed = false,
                guidedDiscoveryThoughts     = "",
                guidedDiscoveryHintVisible  = false,
                guidedDiscoveryAnswerRevealed = false,
                guidedDiscoveryEngineThinking = false,
                guidedDiscoveryRevealedEvalCp = null,
                mentorMoveInputActive       = false,
                mentorMoveChecking          = false,
                mentorMoveResult            = null,
                mentorMoveFeedback          = "",
                showClassificationQuiz      = false,
                classificationOptions       = emptyList(),
                classificationCorrectIndex  = -1,
                classificationSelectedIndex = -1,
                boardState                  = BoardState(
                    fen          = next.preFen,
                    isEditorMode = false,
                ),
            )
        }
    }

    fun updateGuidedThoughts(text: String) {
        _uiState.update { it.copy(guidedDiscoveryThoughts = text) }
    }

    fun revealGuidedHint() {
        _uiState.update {
            it.copy(
                guidedDiscoveryHintVisible     = true,
                guidedDiscoveryInsightRevealed = true,
            )
        }
    }

    fun revealGuidedAnswer() {
        val fen = _uiState.value.boardState.fen
        _uiState.update { it.copy(guidedDiscoveryEngineThinking = true) }

        viewModelScope.launch(Dispatchers.Default) {
            val result = runCatching {
                engine.analyzePosition(fen, depth = ChessConstants.DEFAULT_ANALYSIS_DEPTH)
            }.getOrNull()

            val engineArrow = result?.toArrow()
            val evalCp = result?.let { r ->
                val b = Board().apply { loadFromFen(fen) }
                r.toWhitePerspective(b)
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
            if (!_uiState.value.showClassificationQuiz) {
                triggerClassificationQuiz()
            }
        }
    }

    fun submitGuidedThoughts() {
        advanceDrill()
    }

    fun toggleMentorMoveInput() {
        val nowActive = !_uiState.value.mentorMoveInputActive
        val fen = _uiState.value.boardState.fen
        _uiState.update {
            it.copy(
                mentorMoveInputActive = nowActive,
                mentorMoveChecking    = false,
                mentorMoveResult      = null,
                mentorMoveFeedback    = "",
                boardState            = it.boardState.copy(
                    fen            = fen,
                    lastMove       = null,
                    selectedSquare = null,
                    legalMoves     = emptyList(),
                    isEditorMode   = false,
                ),
            )
        }
    }

    fun onMentorSquareTap(square: Square) {
        val st = _uiState.value
        if (!st.mentorMoveInputActive || st.mentorMoveChecking) return

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
            val board  = Board().apply { loadFromFen(cur.fen) }
            val move   = ChessUtils.buildMove(board, selected, square, solutionUci = null)
            val isLegal = board.legalMoves().contains(move)
            if (isLegal) {
                attemptMove(preFen = cur.fen, move = move)
            } else {
                _uiState.update { it.copy(boardState = cur.copy(selectedSquare = null, legalMoves = emptyList())) }
            }
        }
    }

    fun retryMentorMove() {
        val fen = _uiState.value.queue.getOrNull(_uiState.value.currentIdx)?.preFen ?: DRILL_START_FEN
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
                    isEditorMode   = false,
                ),
            )
        }
    }

    private fun attemptMove(preFen: String, move: Move) {
        val playerUci  = "${move.from.name.lowercase()}${move.to.name.lowercase()}"
        val postBoard  = Board().apply { loadFromFen(preFen) }
        postBoard.doMove(move)
        val postFen        = postBoard.fen
        val playerWasWhite = postBoard.sideToMove == Side.BLACK

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
            val preResult = runCatching {
                engine.analyzePosition(preFen, depth = ChessConstants.DEFAULT_ANALYSIS_DEPTH)
            }.getOrNull()
            val bestUci   = preResult?.bestMoveUci ?: ""
            val preEvalWp = preResult?.let { r ->
                val b = Board().apply { loadFromFen(preFen) }
                r.toWhitePerspective(b)
            } ?: 0

            val postResult = runCatching {
                engine.analyzePosition(postFen, depth = 10)
            }.getOrNull()
            val postEvalWp = postResult?.toWhitePerspective(postBoard) ?: preEvalWp

            val cpLoss = maxOf(0,
                if (playerWasWhite) preEvalWp - postEvalWp
                else                postEvalWp - preEvalWp
            )

            val result = when {
                playerUci == bestUci -> MentorMoveResult.CORRECT
                cpLoss <= 50         -> MentorMoveResult.CLOSE
                else                 -> MentorMoveResult.INCORRECT
            }
            val feedback = when (result) {
                MentorMoveResult.CORRECT   -> "✅ That's the engine's best move! Great find."
                MentorMoveResult.CLOSE     -> "👍 Good move — only ${cpLoss}cp from the best."
                MentorMoveResult.INCORRECT -> "❌ That loses about ${cpLoss}cp. Think again or use a hint."
            }

            if (result == MentorMoveResult.INCORRECT) {
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
            triggerClassificationQuiz()
        }
    }

    fun triggerClassificationQuiz() {
        val current = _uiState.value.queue.getOrNull(_uiState.value.currentIdx) ?: return
        val correct = current.category
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

    fun selectClassificationOption(index: Int) {
        val options = _uiState.value.classificationOptions
        if (index !in options.indices) return
        _uiState.update { it.copy(classificationSelectedIndex = index) }
    }

    private fun buildClassificationOptions(correct: CriticalMoment.ReasonCategory): List<ClassificationOption> {
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
        CriticalMoment.ReasonCategory.MISSED_TACTIC     -> "Missed a tactical pattern"
        CriticalMoment.ReasonCategory.HANGING_PIECE     -> "Left a piece undefended"
        CriticalMoment.ReasonCategory.KING_SAFETY       -> "King safety neglected"
        CriticalMoment.ReasonCategory.OPENING_DEVIATION -> "Opening principle violated"
        CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE -> "Endgame technique error"
        CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE -> "Strategic / positional error"
        CriticalMoment.ReasonCategory.TIME_PRESSURE     -> "Rushed under time pressure"
        CriticalMoment.ReasonCategory.MISSED_WIN        -> "Missed a winning resource"
    }

    private fun categoryDescription(cat: CriticalMoment.ReasonCategory) = when (cat) {
        CriticalMoment.ReasonCategory.MISSED_TACTIC     -> "Fork, pin, skewer or other combination"
        CriticalMoment.ReasonCategory.HANGING_PIECE     -> "Undefended material left en prise"
        CriticalMoment.ReasonCategory.KING_SAFETY       -> "Attack on the king went unnoticed"
        CriticalMoment.ReasonCategory.OPENING_DEVIATION -> "Development or opening-rule breach"
        CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE -> "Incorrect endgame technique"
        CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE -> "Long-term positional weakness created"
        CriticalMoment.ReasonCategory.TIME_PRESSURE     -> "Move played too quickly"
        CriticalMoment.ReasonCategory.MISSED_WIN        -> "Decisive resource or mate was available"
    }
}

private fun EngineResult.toWhitePerspective(board: Board): Int =
    if (board.sideToMove == Side.WHITE) score else -score

private fun EngineResult.toArrow(): Arrow? {
    val uci = bestMoveUci
    if (uci.length < 4) return null
    return runCatching {
        Arrow(
            from  = Square.valueOf(uci.substring(0, 2).uppercase()),
            to    = Square.valueOf(uci.substring(2, 4).uppercase()),
            color = Color(0xCC4CAF50.toInt()),
        )
    }.getOrNull()
}
