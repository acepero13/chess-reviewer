package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.ReviewGame
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Active scope for the cross-game Analytics screen. A single value is shared across all tabs,
 * so the date and game-count windows are mutually exclusive by construction.
 */
sealed interface AnalyticsFilter {
    /** No scoping — aggregate over every analyzed game. */
    object All : AnalyticsFilter

    /** Games whose played date falls within the last [days] days (today inclusive). */
    data class ByDays(val days: Int) : AnalyticsFilter

    /** The [count] most-recently played games. */
    data class ByGames(val count: Int) : AnalyticsFilter
}

/** Day windows offered by the date dropdown. */
val DAY_OPTIONS = listOf(7, 15, 30, 60, 90)

/** Game-count windows offered by the games dropdown. */
val GAME_OPTIONS = listOf(10, 15, 20, 25, 30)

/**
 * Resolves an [AnalyticsFilter] to the set of game IDs it selects. Every analytics table keys
 * rows by `gameId`, so callers filter their loaded lists by membership in this set.
 */
object AnalyticsGameFilter {

    private val PGN_DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    /** Tolerant parse of a PGN `"YYYY.MM.DD"` header; null for empty or `"????.??.??"`. */
    fun parseDate(raw: String): LocalDate? = runCatching { LocalDate.parse(raw, PGN_DATE) }.getOrNull()

    /** The played date, falling back to the import timestamp when the PGN date is missing. */
    private fun playedDate(game: ReviewGame): LocalDate =
        parseDate(game.date)
            ?: Instant.ofEpochMilli(game.importedAt).atZone(ZoneId.systemDefault()).toLocalDate()

    /**
     * Game IDs that pass [filter]:
     *  - [AnalyticsFilter.All]      — every id.
     *  - [AnalyticsFilter.ByDays]   — games with a parseable played date within the window;
     *    games with an unparseable date are excluded.
     *  - [AnalyticsFilter.ByGames]  — the N most recent by played date (import time as fallback).
     */
    fun eligibleIds(
        games: List<ReviewGame>,
        filter: AnalyticsFilter,
        today: LocalDate = LocalDate.now(),
    ): Set<Long> = when (filter) {
        AnalyticsFilter.All -> games.mapTo(HashSet()) { it.id }

        is AnalyticsFilter.ByDays -> {
            val cutoff = today.minusDays((filter.days - 1).toLong())
            games.asSequence()
                .mapNotNull { game -> parseDate(game.date)?.let { game to it } }
                .filter { (_, date) -> !date.isBefore(cutoff) && !date.isAfter(today) }
                .mapTo(HashSet()) { (game, _) -> game.id }
        }

        is AnalyticsFilter.ByGames ->
            games.sortedByDescending { playedDate(it) }
                .take(filter.count)
                .mapTo(HashSet()) { it.id }
    }
}
