package com.acepero13.android.gamereviewer.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.db.MoveTimeDao
import com.acepero13.android.gamereviewer.data.model.MoveTimeData
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.domain.ClockParser
import com.acepero13.android.gamereviewer.domain.pgnToUciMoves
import com.acepero13.chess.core.engine.StockfishEngine
import com.acepero13.chess.core.opening.OpeningClassifier
import com.acepero13.chess.core.pgn.ChessComFetcher
import com.acepero13.chess.core.pgn.LichessFetcher
import com.acepero13.chess.core.pgn.PgnImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImportUiState(
    val isImporting: Boolean = false,
    val importedCount: Int = 0,
    val skippedCount: Int = 0,
    val totalGames: Int = 0,
    val currentGame: String = "",
    val done: Boolean = false,
    val error: String? = null,
)

private const val TAG = "ImportViewModel"

class ImportViewModel(
    private val repo: GameRepository,
    private val importer: PgnImporter,
    private val opening: OpeningClassifier,
    @Suppress("UNUSED_PARAMETER") engine: StockfishEngine,   // reserved for future analysis
    private val moveTimeDao: MoveTimeDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    // ── PGN file import ───────────────────────────────────────────────────────

    fun importFromUri(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { ImportUiState(isImporting = true) }
            try {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText() ?: return@launch
                val filename = uri.lastPathSegment ?: "unknown.pgn"
                importPgnText(text, sourceType = "file", sourceIdPrefix = filename)
            } catch (e: Exception) {
                _uiState.update { it.copy(isImporting = false, error = e.message) }
            }
        }
    }

    // ── Chess.com import ──────────────────────────────────────────────────────

    fun importFromChessCom(username: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { ImportUiState(isImporting = true, currentGame = "Fetching from Chess.com…") }
            try {
                val pgn = ChessComFetcher.fetchPgn(username)
                importPgnText(pgn, sourceType = "chesscom", sourceIdPrefix = username)
            } catch (e: Exception) {
                _uiState.update { it.copy(isImporting = false, error = e.message) }
            }
        }
    }

    // ── Lichess import ────────────────────────────────────────────────────────

    fun importFromLichess(username: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { ImportUiState(isImporting = true, currentGame = "Fetching from Lichess…") }
            try {
                val pgn = LichessFetcher.fetchPgn(username)
                Log.d(TAG, "Lichess raw PGN length=${pgn.length}")
                // Log first 500 chars so we can see if [%clk ...] annotations are present
                Log.d(TAG, "Lichess PGN preview:\n${pgn.take(500)}")
                importPgnText(pgn, sourceType = "lichess", sourceIdPrefix = username)
            } catch (e: Exception) {
                _uiState.update { it.copy(isImporting = false, error = e.message) }
            }
        }
    }

    // ── Shared parsing ────────────────────────────────────────────────────────

    private suspend fun importPgnText(pgn: String, sourceType: String, sourceIdPrefix: String) {
        val games = importer.splitGames(pgn)
        _uiState.update { it.copy(totalGames = games.size) }

        var imported = 0; var skipped = 0

        games.forEachIndexed { idx, gamePgn ->
            val parsed = importer.parseGame(gamePgn) ?: run { skipped++; return@forEachIndexed }
            val white  = parsed.headers["White"]  ?: "?"
            val black  = parsed.headers["Black"]  ?: "?"
            val siteId = parsed.headers["Site"]?.substringAfterLast('/') ?: "$sourceIdPrefix-$idx"

            _uiState.update { it.copy(currentGame = "$white vs $black") }

            // Skip duplicates — but backfill clock data if it was never stored for this game
            // (i.e. the game was imported before clock-parsing support was added).
            val duplicate = repo.findDuplicate(sourceType, siteId)
            if (duplicate != null) {
                if (moveTimeDao.countByGameId(duplicate.id) == 0) {
                    val timeControl = parsed.headers["TimeControl"]
                    val clocks      = ClockParser.parseMoveClocks(parsed.movesPgn)
                        .ifEmpty { ClockParser.parseMoveClocks(gamePgn) }
                    if (clocks.isNotEmpty()) {
                        val timeSpent = ClockParser.computeTimeSpent(
                            clocks         = clocks,
                            initialSeconds = ClockParser.parseTimeControl(timeControl) ?: 0,
                        )
                        moveTimeDao.insertAll(clocks.indices.map { i ->
                            MoveTimeData(
                                gameId                = duplicate.id,
                                moveIndex             = i + 1,
                                timeSpentSeconds      = timeSpent.getOrElse(i) { 0 },
                                clockRemainingSeconds = clocks[i],
                            )
                        })
                    }
                }
                skipped++
                return@forEachIndexed
            }

            // Parse UCI move list.
            // pgnToUciMoves strips { } comments and ( ) variations before parsing,
            // so clock annotations from Chess.com / Lichess are handled correctly.
            val movesUci = pgnToUciMoves(parsed.movesPgn)

            // Classify opening
            val entry = runCatching { opening.classifyByMoves(movesUci.split(' ')) }.getOrNull()

            val game = ReviewGame(
                whitePlayer  = white,
                blackPlayer  = black,
                result       = parsed.headers["Result"] ?: "*",
                date         = parsed.headers["Date"]   ?: "",
                event        = parsed.headers["Event"]  ?: "",
                movesUci     = movesUci,
                pgn          = gamePgn,
                openingEco   = entry?.eco  ?: "",
                openingName  = entry?.name ?: "",
                sourceType   = sourceType,
                sourceId     = siteId,
            )
            val gameDbId = repo.insert(game)

            // ── Parse clock annotations (Task 4.1) ────────────────────────────
            // Runs on the same IO coroutine — small overhead compared to the network fetch.
            // Use parsed.movesPgn (raw moves text with {[%clk ...]} annotations intact) for
            // reliable clock extraction; fall back to the full gamePgn if needed.
            if (gameDbId > 0) {
                val timeControl = parsed.headers["TimeControl"]
                Log.d(TAG, "[$white vs $black] movesPgn preview='${parsed.movesPgn.take(120)}'")
                val clocksFromMoves = ClockParser.parseMoveClocks(parsed.movesPgn)
                val clocksFromFull  = if (clocksFromMoves.isEmpty()) ClockParser.parseMoveClocks(gamePgn) else emptyList()
                val clocks = clocksFromMoves.ifEmpty { clocksFromFull }
                Log.d(TAG, "[$white vs $black] TimeControl=$timeControl  " +
                    "clocksFromMoves=${clocksFromMoves.size}  clocksFromFull=${clocksFromFull.size}  " +
                    "clocks.size=${clocks.size}  first5=${clocks.take(5)}")
                if (clocks.isNotEmpty()) {
                    val timeSpent = ClockParser.computeTimeSpent(
                        clocks         = clocks,
                        initialSeconds = ClockParser.parseTimeControl(timeControl) ?: 0,
                    )
                    Log.d(TAG, "[$white vs $black] timeSpent.size=${timeSpent.size}  first5=${timeSpent.take(5)}")
                    val moveTimes = clocks.indices.map { i ->
                        MoveTimeData(
                            gameId                 = gameDbId,
                            moveIndex              = i + 1,     // 1-based
                            timeSpentSeconds       = timeSpent.getOrElse(i) { 0 },
                            clockRemainingSeconds  = clocks[i],
                        )
                    }
                    moveTimeDao.insertAll(moveTimes)
                } else {
                    Log.w(TAG, "[$white vs $black] No clock data found — gamePgn snippet='${gamePgn.take(200)}'")
                }
            }

            imported++
            _uiState.update { it.copy(importedCount = imported, skippedCount = skipped) }
        }

        _uiState.update { it.copy(isImporting = false, done = true, skippedCount = skipped) }
    }

    fun reset() { _uiState.value = ImportUiState() }
}
