package com.acepero13.android.gamereviewer.ui.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.chess.core.engine.StockfishEngine
import com.acepero13.chess.core.opening.OpeningClassifier
import com.acepero13.chess.core.pgn.ChessComFetcher
import com.acepero13.chess.core.pgn.LichessFetcher
import com.acepero13.chess.core.pgn.PgnImporter
import com.github.bhlangonijr.chesslib.move.MoveList
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

class ImportViewModel(
    private val repo: GameRepository,
    private val importer: PgnImporter,
    private val opening: OpeningClassifier,
    @Suppress("UNUSED_PARAMETER") engine: StockfishEngine,   // reserved for future analysis
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

            // Skip duplicates
            if (repo.isDuplicate(sourceType, siteId)) { skipped++; return@forEachIndexed }

            // Parse UCI move list
            val movesUci = runCatching {
                val ml = MoveList()
                ml.loadFromSan(parsed.movesPgn)
                ml.joinToString(" ") { m -> "${m.from.name.lowercase()}${m.to.name.lowercase()}" +
                    (if (m.promotion != com.github.bhlangonijr.chesslib.Piece.NONE) m.promotion.fenSymbol.lowercase() else "") }
            }.getOrDefault("")

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
            repo.insert(game)
            imported++
            _uiState.update { it.copy(importedCount = imported, skippedCount = skipped) }
        }

        _uiState.update { it.copy(isImporting = false, done = true, skippedCount = skipped) }
    }

    fun reset() { _uiState.value = ImportUiState() }
}
