package com.acepero13.android.gamereviewer.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.db.CriticalMomentDao
import com.acepero13.android.gamereviewer.data.db.GameEvaluationDao
import com.acepero13.android.gamereviewer.data.db.MoveTimeDao
import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.data.model.MoveTimeData
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.data.repository.SettingsRepository
import com.acepero13.android.gamereviewer.domain.GameNarrativeSummary
import com.acepero13.android.gamereviewer.domain.TimeAnalyzer
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
                val moveList = game?.let { buildMoveList(it.movesUci, evals) } ?: emptyList()

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
                        hasTimeData     = times.isNotEmpty(),
                        narrative       = narrative,
                        moveListEntries = moveList,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
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
