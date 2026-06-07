package com.acepero13.android.gamereviewer.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.db.GuessMoveProgressDao
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.domain.SessionDebrief
import com.acepero13.android.gamereviewer.domain.pgnToUciMoves
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

private const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

data class MasterGamePreview(
    val index: Int,
    val white: String,
    val black: String,
    val event: String,
    val year: String,
    val eco: String,
    val fen: String = START_FEN,
)

class HomeViewModel(
    repo: GameRepository,
    private val context: Context,
    private val progressDao: GuessMoveProgressDao,
) : ViewModel() {

    val gameCount = repo.countAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val hasRecentSession = repo.countRecentGames(SessionDebrief.sessionCutoff())
        .map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val recentGames: kotlinx.coroutines.flow.StateFlow<List<ReviewGame>> = repo.observeAll()
        .map { it.take(6) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val nextToReview: kotlinx.coroutines.flow.StateFlow<ReviewGame?> = repo.observeAll()
        .map { games ->
            games.filter { game ->
                val total = game.movesUci.split(" ").count { it.isNotBlank() }
                game.lastReviewedMoveIndex < total
            }.maxByOrNull { game ->
                val total = game.movesUci.split(" ").count { it.isNotBlank() }
                if (total == 0) 0f else game.lastReviewedMoveIndex.toFloat() / total
            } ?: games.firstOrNull()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val masterGamePreviews = flow {
        val previews = withContext(Dispatchers.IO) { loadMasterGamePreviews() }
        val progressByIndex = withContext(Dispatchers.IO) {
            runCatching { progressDao.getAll() }.getOrDefault(emptyList()).associateBy { it.gameIndex }
        }
        val sorted = previews.sortedByDescending { p ->
            val prog = progressByIndex[p.index]
            if (prog != null && prog.totalMoves > 0) prog.currentMoveIndex.toFloat() / prog.totalMoves else 0f
        }
        emit(sorted)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun loadMasterGamePreviews(): List<MasterGamePreview> = runCatching {
        val pgn = context.assets.open("master_games.pgn").bufferedReader().readText()
        val headerRegex = Regex("""\[(\w+)\s+"([^"]*)"]""")
        pgn.split(Regex("(?=\\[Event )"))
            .filter { it.trimStart().startsWith("[") }
            .mapIndexed { idx, block ->
                val h = headerRegex.findAll(block).associate { it.groupValues[1] to it.groupValues[2] }
                val movesPgn = block.lines()
                    .dropWhile { it.trimStart().startsWith("[") || it.isBlank() }
                    .joinToString(" ")
                MasterGamePreview(
                    index = idx,
                    white = h["White"] ?: "?",
                    black = h["Black"] ?: "?",
                    event = h["Event"] ?: "",
                    year  = h["Date"]?.substringBefore(".") ?: "",
                    eco   = h["ECO"] ?: "",
                    fen   = fenAtMoveN(movesPgn, targetMoveN = 15 + (idx % 10)),
                )
            }
    }.getOrDefault(emptyList())

    private fun fenAtMoveN(movesPgn: String, targetMoveN: Int): String {
        if (movesPgn.isBlank()) return START_FEN
        return runCatching {
            val ucis = pgnToUciMoves(movesPgn).split(" ").filter { it.isNotBlank() }
            if (ucis.isEmpty()) return@runCatching START_FEN
            val board = Board()
            board.loadFromFen(START_FEN)
            val limit = minOf(targetMoveN, ucis.size)
            for (i in 0 until limit) {
                val uci = ucis[i]
                if (uci.length < 4) continue
                val from = Square.valueOf(uci.substring(0, 2).uppercase())
                val to   = Square.valueOf(uci.substring(2, 4).uppercase())
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
            }
            board.fen
        }.getOrDefault(START_FEN)
    }
}
