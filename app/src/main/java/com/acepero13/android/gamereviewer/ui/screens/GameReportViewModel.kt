package com.acepero13.android.gamereviewer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.db.CriticalMomentDao
import com.acepero13.android.gamereviewer.data.db.GameEvaluationDao
import com.acepero13.android.gamereviewer.data.db.MoveTimeDao
import com.acepero13.chess.core.data.db.PositionAnnotationDao
import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.data.model.MoveTimeData
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.data.repository.SettingsRepository
import com.acepero13.android.gamereviewer.domain.GameNarrativeSummary
import com.acepero13.android.gamereviewer.domain.TimeAnalyzer
import com.acepero13.android.gamereviewer.domain.VelocityConsistency
import com.acepero13.android.gamereviewer.domain.VelocityConsistencyAnalyzer
import com.acepero13.chess.core.data.model.ChessConstants
import com.acepero13.chess.core.opening.OpeningClassifier
import com.acepero13.chess.core.util.ChessUtils
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhaseAccuracyData(
    val openingAvgCpl: Float = 0f,
    val middlegameAvgCpl: Float = 0f,
    val endgameAvgCpl: Float = 0f,
    val hasOpening: Boolean = false,
    val hasMiddlegame: Boolean = false,
    val hasEndgame: Boolean = false,
)

data class ReasonBreakdownData(
    val label: String,
    val severity: Int,
    val count: Int,
    val category: CriticalMoment.ReasonCategory,
)

data class SelfAwarenessData(
    val noticed: Int,
    val total: Int,
) {
    val score: Float get() = if (total == 0) 0f else noticed.toFloat() / total.toFloat()
}

data class BestMomentData(
    val moveLabel: String,
    val evalCp: Int,
)

data class MoveListEntry(
    val moveNumber: Int,
    val whiteSan: String,
    val blackSan: String?,
    val evalWhiteCp: Int,
    val evalBlackCp: Int?,
    val cplWhite: Int,
    val cplBlack: Int?,
    val assessWhite: String,
    val assessBlack: String?,
    val bestWhite: String?,
    val bestBlack: String?,
    val whiteIsTop3: Boolean,
    val blackIsTop3: Boolean,
    val whiteDepth: Int,
    val blackDepth: Int,
)

data class GameReportUiState(
    val isLoading: Boolean = true,
    val gameTitle: String = "",
    val decisions: List<TimeAnalyzer.MoveDecision> = emptyList(),
    val evaluations: List<GameEvaluation> = emptyList(),
    val moveTimes: List<MoveTimeData> = emptyList(),

    // Aggregate stats
    val totalMoves: Int = 0,
    val blunderCount: Int = 0,
    val rushedBlunders: Int = 0,
    val carefulBlunders: Int = 0,
    val avgTimeOnBlunders: Float = 0f,
    val avgTimeOnGoodMoves: Float = 0f,
    val hasTimeData: Boolean = false,

    val narrative: GameNarrativeSummary.Summary? = null,
    val moveListEntries: List<MoveListEntry> = emptyList(),

    val moveQualityCounts: Map<String, Int> = emptyMap(),
    val phaseAccuracy: PhaseAccuracyData? = null,
    val mistakeReasons: List<ReasonBreakdownData> = emptyList(),
    val selfAwareness: SelfAwarenessData? = null,
    val bestMoments: List<BestMomentData> = emptyList(),

    // ── New insight fields ──────────────────────────────────────────────────────
    /** Move indices where the player over-calculated but still blundered. */
    val overthougtMoveIndices: Set<Int> = emptySet(),
    /**
     * Fraction of engine-critical positions where the player wrote candidate moves
     * in the questionnaire. Null when no engine analysis exists for this game.
     */
    val candidateEngagementRate: Float? = null,
    val velocityConsistency: VelocityConsistency? = null,

    val error: String? = null,
)

/**
 * Backs the per-game report screen (Task 4.2).
 *
 * Loads [GameEvaluation] and [MoveTimeData] from Room, runs [TimeAnalyzer.analyze],
 * and exposes the result for the [GameReportScreen] to render as a Decision Velocity chart.
 */
class GameReportViewModel(
    private val gameId: Long,
    private val repo: GameRepository,
    private val evalDao: GameEvaluationDao,
    private val moveTimeDao: MoveTimeDao,
    private val criticalMomentDao: CriticalMomentDao,
    private val annotationDao: PositionAnnotationDao,
    private val settingsRepo: SettingsRepository,
    private val openingClassifier: OpeningClassifier,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameReportUiState())
    val uiState: StateFlow<GameReportUiState> = _uiState.asStateFlow()

    init { loadReport() }

    private fun loadReport() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val game    = repo.findById(gameId)
                val evals   = evalDao.getByGameId(gameId)
                val times   = moveTimeDao.getByGameId(gameId)
                val moments = criticalMomentDao.getByGameId(gameId)
                val username = settingsRepo.username.firstOrNull() ?: ""
                val decisions = TimeAnalyzer.analyze(evals, times)

                val title  = game?.let { "${it.whitePlayer} vs ${it.blackPlayer}" } ?: "Game #$gameId"
                val blunders = decisions.count { it.isBlunder }
                val narrative = game?.let {
                    GameNarrativeSummary.build(it, moments, username)
                }
                val moveList       = game?.let { buildMoveList(it.movesUci, evals) } ?: emptyList()
                val phaseAccuracy  = computePhaseAccuracy(evals)
                val moveQuality    = computeMoveQualityCounts(moveList)
                val mistakeReasons = computeMistakeReasons(moments)
                val selfAwareness  = computeSelfAwareness(moments)
                val bestMoments    = computeBestMoments(moveList, evals)
                val overthougt     = TimeAnalyzer.overthougtMoveIndices(decisions)
                val candidateEngagement = computeCandidateEngagement(moments)
                val velocityConsistency = VelocityConsistencyAnalyzer.compute(mapOf(gameId to times))

                _uiState.update {
                    it.copy(
                        isLoading       = false,
                        gameTitle       = title,
                        decisions       = decisions,
                        evaluations     = evals,
                        moveTimes       = times,
                        totalMoves      = decisions.size,
                        blunderCount    = blunders,
                        rushedBlunders  = TimeAnalyzer.countRushedBlunders(decisions),
                        carefulBlunders = TimeAnalyzer.countCarefulBlunders(decisions),
                        avgTimeOnBlunders  = TimeAnalyzer.avgTimeOnBlunders(decisions),
                        avgTimeOnGoodMoves = TimeAnalyzer.avgTimeOnGoodMoves(decisions),
                        hasTimeData         = times.isNotEmpty(),
                        narrative           = narrative,
                        moveListEntries     = moveList,
                        moveQualityCounts   = moveQuality,
                        phaseAccuracy       = phaseAccuracy,
                        mistakeReasons      = mistakeReasons,
                        selfAwareness       = selfAwareness,
                        bestMoments         = bestMoments,
                        overthougtMoveIndices    = overthougt,
                        candidateEngagementRate  = candidateEngagement,
                        velocityConsistency      = velocityConsistency,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ── Insight computations ──────────────────────────────────────────────────

    private fun computePhaseAccuracy(evals: List<GameEvaluation>): PhaseAccuracyData {
        fun cpls(list: List<GameEvaluation>) = list.map { ev ->
            val loss = if (ev.moveIndex % 2 == 1) -ev.evalDelta else ev.evalDelta
            maxOf(0, loss)
        }
        val opening    = evals.filter { it.moveIndex <= 20 }
        val middlegame = evals.filter { it.moveIndex in 21..60 }
        val endgame    = evals.filter { it.moveIndex > 60 }
        return PhaseAccuracyData(
            openingAvgCpl    = cpls(opening).let    { if (it.isEmpty()) 0f else it.average().toFloat() },
            middlegameAvgCpl = cpls(middlegame).let { if (it.isEmpty()) 0f else it.average().toFloat() },
            endgameAvgCpl    = cpls(endgame).let   { if (it.isEmpty()) 0f else it.average().toFloat() },
            hasOpening       = opening.isNotEmpty(),
            hasMiddlegame    = middlegame.isNotEmpty(),
            hasEndgame       = endgame.isNotEmpty(),
        )
    }

    private fun computeMoveQualityCounts(moveList: List<MoveListEntry>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        for (entry in moveList) {
            counts[entry.assessWhite] = (counts[entry.assessWhite] ?: 0) + 1
            entry.assessBlack?.let { counts[it] = (counts[it] ?: 0) + 1 }
        }
        return counts
    }

    private fun computeMistakeReasons(moments: List<CriticalMoment>): List<ReasonBreakdownData> =
        moments
            .groupBy { it.toReason() }
            .map { (reason, list) ->
                ReasonBreakdownData(
                    label    = reasonLabel(reason),
                    severity = list.sumOf { it.severity },
                    count    = list.size,
                    category = reason,
                )
            }
            .sortedByDescending { it.severity }
            .take(5)

    private fun computeSelfAwareness(moments: List<CriticalMoment>): SelfAwarenessData {
        val engineMarked = moments.filter { it.toType() == CriticalMoment.Type.ENGINE_MARKED }
        val userIndices  = moments
            .filter { it.toType() == CriticalMoment.Type.USER_MARKED }
            .map { it.moveIndex }
            .toSet()
        return SelfAwarenessData(
            noticed = engineMarked.count { it.moveIndex in userIndices },
            total   = engineMarked.size,
        )
    }

    private fun computeBestMoments(
        moveList: List<MoveListEntry>,
        evals: List<GameEvaluation>,
    ): List<BestMomentData> {
        val evalByIndex = evals.associateBy { it.moveIndex }
        data class Candidate(val data: BestMomentData, val prevAbsEval: Int)

        val candidates = mutableListOf<Candidate>()
        for (entry in moveList) {
            val wi = entry.moveNumber * 2 - 1
            if (entry.assessWhite == "Best Move") {
                val prevAbs = kotlin.math.abs(evalByIndex[wi - 1]?.evalCp ?: 0)
                if (prevAbs <= 300) {
                    candidates += Candidate(
                        BestMomentData("${entry.moveNumber}. ${entry.whiteSan}", entry.evalWhiteCp),
                        prevAbs,
                    )
                }
            }
            entry.blackSan?.let { bSan ->
                if (entry.assessBlack == "Best Move") {
                    val prevAbs = kotlin.math.abs(entry.evalWhiteCp)
                    if (prevAbs <= 300) {
                        candidates += Candidate(
                            BestMomentData("${entry.moveNumber}... $bSan", entry.evalBlackCp ?: 0),
                            prevAbs,
                        )
                    }
                }
            }
        }
        return candidates.sortedBy { it.prevAbsEval }.map { it.data }.take(3)
    }

    /**
     * Computes the fraction of engine-critical positions where the player wrote
     * candidate moves in the self-analysis questionnaire.
     *
     * The questionnaire stores responses as markdown in [PositionAnnotation.moveComment]
     * with the line prefix `**Candidates:**`. A position is counted as "engaged" when
     * its annotation contains this prefix.
     */
    private suspend fun computeCandidateEngagement(moments: List<CriticalMoment>): Float? {
        val engineMoments = moments.filter { it.toType() == CriticalMoment.Type.ENGINE_MARKED }
        if (engineMoments.isEmpty()) return null
        val withCandidates = engineMoments.count { m ->
            if (m.fen.isBlank()) return@count false
            val annot = runCatching { annotationDao.getByFen(m.fen) }.getOrNull()
            annot?.moveComment?.contains("**Candidates:**") == true
        }
        return withCandidates.toFloat() / engineMoments.size
    }

    private fun reasonLabel(reason: CriticalMoment.ReasonCategory) = when (reason) {
        CriticalMoment.ReasonCategory.MISSED_TACTIC     -> "Missed Tactics"
        CriticalMoment.ReasonCategory.OPENING_DEVIATION -> "Opening Gap"
        CriticalMoment.ReasonCategory.HANGING_PIECE     -> "Hanging Pieces"
        CriticalMoment.ReasonCategory.KING_SAFETY       -> "King Safety"
        CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE -> "Endgame Technique"
        CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE -> "Strategic Errors"
        CriticalMoment.ReasonCategory.TIME_PRESSURE     -> "Time Pressure"
        CriticalMoment.ReasonCategory.MISSED_WIN        -> "Missed Wins"
    }

    private fun buildMoveList(
        movesUciStr: String,
        evals: List<GameEvaluation>,
    ): List<MoveListEntry> {
        val uciMoves = movesUciStr.split(" ").filter { it.isNotBlank() }
        if (uciMoves.isEmpty() || evals.isEmpty()) return emptyList()

        data class PositionInfo(val san: String, val postFen: String)

        val positions = mutableListOf<PositionInfo>()
        val board = Board()
        for (uci in uciMoves) {
            val san = runCatching { ChessUtils.uciToSan(board, uci) }.getOrElse { uci }
            val move = uciToMove(board, uci) ?: break
            board.doMove(move)
            positions.add(PositionInfo(san, board.fen))
        }

        val evalByIndex = evals.associateBy { it.moveIndex }
        val depth = ChessConstants.DEFAULT_ANALYSIS_DEPTH
        val entries = mutableListOf<MoveListEntry>()

        val fullMoves = (positions.size + 1) / 2
        for (moveNum in 1..fullMoves) {
            val wi = moveNum * 2 - 1  // white's 1-based moveIndex
            val bi = moveNum * 2      // black's 1-based moveIndex

            val whitePos = positions.getOrNull(wi - 1) ?: break
            val blackPos = positions.getOrNull(bi - 1)

            val whiteEval = evalByIndex[wi]
            val blackEval = evalByIndex[bi]
            val prevBlackEval = evalByIndex[wi - 1]  // black's previous move (if any)

            val cplWhite = whiteEval?.let { maxOf(0, -it.evalDelta) } ?: 0
            val cplBlack = blackEval?.let { maxOf(0, it.evalDelta) }

            val isWhiteBook = runCatching {
                openingClassifier.classify(whitePos.postFen) != null
            }.getOrDefault(false)
            val isBlackBook = blackPos?.let {
                runCatching { openingClassifier.classify(it.postFen) != null }.getOrDefault(false)
            } ?: false

            // Best alternative comes from the previous eval's pvLine (that position's engine suggestion)
            val prevBlackPostFen = positions.getOrNull(wi - 2)?.postFen
            val bestWhiteSan = deriveBestSan(prevBlackEval?.pvLine, prevBlackPostFen)
                ?.takeIf { it != whitePos.san }
            val bestBlackSan = deriveBestSan(whiteEval?.pvLine, whitePos.postFen)
                ?.takeIf { it != blackPos?.san }

            entries.add(
                MoveListEntry(
                    moveNumber  = moveNum,
                    whiteSan    = whitePos.san,
                    blackSan    = blackPos?.san,
                    evalWhiteCp = whiteEval?.evalCp ?: 0,
                    evalBlackCp = blackEval?.evalCp,
                    cplWhite    = cplWhite,
                    cplBlack    = cplBlack,
                    assessWhite = if (isWhiteBook) "Book Move" else assessFromCpl(cplWhite),
                    assessBlack = blackPos?.let {
                        if (isBlackBook) "Book Move" else assessFromCpl(cplBlack ?: 0)
                    },
                    bestWhite   = bestWhiteSan,
                    bestBlack   = bestBlackSan,
                    whiteIsTop3 = cplWhite <= 5,
                    blackIsTop3 = (cplBlack ?: 0) <= 5,
                    whiteDepth  = depth,
                    blackDepth  = depth,
                )
            )
        }
        return entries
    }

    private fun deriveBestSan(pvLine: String?, boardFen: String?): String? {
        if (pvLine.isNullOrBlank() || boardFen == null) return null
        val firstUci = pvLine.split(",").firstOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            val b = Board().apply { loadFromFen(boardFen) }
            ChessUtils.uciToSan(b, firstUci)
        }.getOrNull()
    }

    private fun assessFromCpl(cpl: Int): String = when {
        cpl == 0    -> "Best Move"
        cpl <= 10   -> "Excellent"
        cpl <= 25   -> "Good Move"
        cpl <= 50   -> "Inaccuracy"
        cpl <= 100  -> "Mistake"
        else        -> "Blunder"
    }

    private fun uciToMove(board: Board, uci: String): Move? {
        if (uci.length < 4) return null
        return runCatching {
            val from = Square.valueOf(uci.substring(0, 2).uppercase())
            val to   = Square.valueOf(uci.substring(2, 4).uppercase())
            if (uci.length == 5) {
                val side = board.sideToMove
                val prom = when (uci[4].lowercaseChar()) {
                    'r'  -> if (side == Side.WHITE) Piece.WHITE_ROOK   else Piece.BLACK_ROOK
                    'b'  -> if (side == Side.WHITE) Piece.WHITE_BISHOP else Piece.BLACK_BISHOP
                    'n'  -> if (side == Side.WHITE) Piece.WHITE_KNIGHT else Piece.BLACK_KNIGHT
                    else -> if (side == Side.WHITE) Piece.WHITE_QUEEN  else Piece.BLACK_QUEEN
                }
                Move(from, to, prom)
            } else {
                Move(from, to)
            }
        }.getOrNull()
    }
}
