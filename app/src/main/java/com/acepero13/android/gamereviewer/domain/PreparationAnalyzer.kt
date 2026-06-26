package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.GameStats

/**
 * Aggregated opening-repertoire view — backs the **Preparation** tab.
 *
 * Groups cached [GameStats] by opening (ECO + name), joins each game's result for win/draw/loss,
 * and surfaces the user's best "weapon", a per-opening table, and how concentrated / deep their
 * repertoire is.
 */
data class PreparationReport(
    val openings: List<OpeningRow>,
    val topOpening: OpeningRow?,
    val overallBookDepthPly: Float,
    val concentrationPct: Int,
    val distinctOpenings: Int,
) {
    val hasData: Boolean get() = openings.isNotEmpty()
}

data class OpeningRow(
    val eco: String,
    val name: String,
    val games: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val precision: Float,
    val bookDepthPly: Float,
) {
    val winRatePct: Int get() = if (games == 0) 0 else wins * 100 / games
}

object PreparationAnalyzer {

    /** Minimum games for an opening to be eligible as the user's headline "weapon". */
    private const val WEAPON_MIN_GAMES = 3

    /**
     * @param resultByGame game id → PGN result string ("1-0" / "0-1" / "1/2-1/2" / "*").
     */
    fun analyze(
        stats: List<GameStats>,
        resultByGame: Map<Long, String>,
    ): PreparationReport {
        val withOpening = stats.filter { it.openingName.isNotBlank() || it.openingEco.isNotBlank() }
        if (withOpening.isEmpty()) {
            return PreparationReport(emptyList(), null, 0f, 0, 0)
        }

        val rows = withOpening
            .groupBy { it.openingEco.ifBlank { it.openingName } }
            .map { (_, group) -> toRow(group, resultByGame) }
            .sortedByDescending { it.games }

        val totalGames = withOpening.size
        val topByGames = rows.firstOrNull()
        val weapon = rows.filter { it.games >= WEAPON_MIN_GAMES }.maxByOrNull { it.winRatePct }
            ?: topByGames

        return PreparationReport(
            openings            = rows,
            topOpening          = weapon,
            overallBookDepthPly = withOpening.map { it.bookDepthPly }.average().toFloat(),
            concentrationPct    = if (totalGames == 0) 0 else (topByGames?.games ?: 0) * 100 / totalGames,
            distinctOpenings    = rows.size,
        )
    }

    private fun toRow(group: List<GameStats>, resultByGame: Map<Long, String>): OpeningRow {
        var wins = 0; var draws = 0; var losses = 0
        group.forEach { gs ->
            when (outcome(resultByGame[gs.gameId], gs.playerIsWhite)) {
                Outcome.WIN  -> wins++
                Outcome.DRAW -> draws++
                Outcome.LOSS -> losses++
                Outcome.NONE -> {}
            }
        }
        return OpeningRow(
            eco          = group.firstOrNull { it.openingEco.isNotBlank() }?.openingEco.orEmpty(),
            name         = group.firstOrNull { it.openingName.isNotBlank() }?.openingName
                ?: group.first().openingEco,
            games        = group.size,
            wins         = wins,
            draws        = draws,
            losses       = losses,
            precision    = group.map { it.openingAccuracy }.filter { it > 0f }.let {
                if (it.isEmpty()) 0f else it.average().toFloat()
            },
            bookDepthPly = group.map { it.bookDepthPly }.average().toFloat(),
        )
    }

    private enum class Outcome { WIN, DRAW, LOSS, NONE }

    private fun outcome(result: String?, isWhite: Boolean): Outcome = when (result) {
        "1-0"     -> if (isWhite) Outcome.WIN else Outcome.LOSS
        "0-1"     -> if (isWhite) Outcome.LOSS else Outcome.WIN
        "1/2-1/2" -> Outcome.DRAW
        else      -> Outcome.NONE
    }
}
