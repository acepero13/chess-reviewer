package com.acepero13.android.gamereviewer.ui.screens

import android.content.Context
import com.acepero13.android.gamereviewer.domain.pgnToUciMoves
import com.acepero13.chess.core.pgn.ChessComFetcher
import com.acepero13.chess.core.pgn.LichessFetcher
import com.acepero13.chess.core.pgn.PgnImporter

private const val MIN_GAME_HALF_MOVES = 20

internal class MasterGameService(
    private val context: Context,
    private val importer: PgnImporter,
) {

    suspend fun fetch(
        source: MasterGameSource,
        customUsername: String,
        platform: String,
    ): Pair<String, String> = when (source) {
        is MasterGameSource.Offline ->
            loadAssets() to "Offline"
        is MasterGameSource.OnlineFamousPlayer ->
            fetchOnline(source.platform, source.username) to
                "${source.platform.replaceFirstChar { it.uppercase() }}: ${source.displayName}"
        is MasterGameSource.OnlineCustom ->
            fetchOnline(platform, customUsername) to
                "${platform.replaceFirstChar { it.uppercase() }}: $customUsername"
    }

    fun splitGames(pgn: String): List<String> = importer.splitGames(pgn)

    fun parseGame(pgn: String) = importer.parseGame(pgn)

    fun loadGameAtIndex(index: Int): String {
        val games = importer.splitGames(loadAssets())
        return games.getOrNull(index) ?: games.first()
    }

    fun pickGame(games: List<String>): String? {
        val suitable = games.filter { game ->
            val uci = pgnToUciMoves(importer.parseGame(game)?.movesPgn ?: return@filter false)
            uci.split(" ").count { it.isNotBlank() } >= MIN_GAME_HALF_MOVES
        }
        return suitable.randomOrNull() ?: games.firstOrNull()
    }

    fun buildGameDescription(headers: Map<String, String>): String {
        val white = headers["White"] ?: "?"
        val black = headers["Black"] ?: "?"
        val event = headers["Event"] ?: ""
        val date  = headers["Date"]?.substringBefore(".") ?: ""
        return buildString {
            append("$white vs $black")
            if (event.isNotBlank()) append(" · $event")
            if (date.isNotBlank()) append(" · $date")
        }
    }

    private suspend fun fetchOnline(platform: String, username: String): String =
        if (platform == "lichess") LichessFetcher.fetchPgn(username)
        else ChessComFetcher.fetchPgn(username)

    private fun loadAssets(): String =
        context.assets.open("master_games.pgn").bufferedReader().readText()
}
