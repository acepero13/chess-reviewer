package com.acepero13.android.gamereviewer.domain

/**
 * Parses `{ [%clk HH:MM:SS] }` clock annotations embedded in PGN move text (Task 4.1).
 *
 * Lichess and Chess.com both emit these annotations in their PGN exports.
 * The clock value is the player's **remaining** time immediately after the move.
 *
 * Time spent on move N = clock[N-1] - clock[N], where clock[-1] is the starting
 * time from the TimeControl header (or inferred from the first clock annotation).
 */
object ClockParser {

    /** Matches `[%clk H:MM:SS]` or `[%clk H:MM:SS.d]`. */
    private val CLOCK_REGEX = Regex("""\[%clk\s+(\d+):(\d{1,2}):(\d{1,2}(?:\.\d+)?)\]""")

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Extracts all clock readings (remaining seconds after each half-move) from
     * the PGN move text, in order: [White's move 1, Black's move 1, White's move 2, …].
     *
     * Returns an empty list if no clock annotations are present.
     */
    fun parseMoveClocks(movesPgn: String): List<Int> =
        CLOCK_REGEX.findAll(movesPgn).map { m ->
            val (h, min, sec) = m.destructured
            h.toInt() * 3600 + min.toInt() * 60 + sec.toFloat().toInt()
        }.toList()

    /**
     * Converts a list of remaining-time clock readings to a list of **time spent**
     * per half-move.
     *
     * Each player's clock is tracked independently so that games with increment or
     * asymmetric time controls are handled correctly.
     *
     * @param clocks         Remaining-time readings from [parseMoveClocks], indexed 0…N-1.
     * @param initialSeconds Base time for each player in seconds.  If 0 or negative,
     *                       the first move's clock is used as the baseline (time spent ≈ 0).
     * @return Time spent on each move, in seconds, same length as [clocks].
     */
    fun computeTimeSpent(clocks: List<Int>, initialSeconds: Int): List<Int> {
        if (clocks.isEmpty()) return emptyList()
        val result    = mutableListOf<Int>()
        val base      = if (initialSeconds > 0) initialSeconds else clocks.first()
        var prevWhite = base
        var prevBlack = base

        clocks.forEachIndexed { idx, clock ->
            val isWhite = idx % 2 == 0
            val prev    = if (isWhite) prevWhite else prevBlack
            val spent   = maxOf(0, prev - clock)
            result.add(spent)
            if (isWhite) prevWhite = clock else prevBlack = clock
        }
        return result
    }

    /**
     * Parses the PGN `TimeControl` header value into initial seconds.
     *
     * Handles common formats:
     * - `"300+0"` → 300 s  (5 min bullet)
     * - `"600+5"` → 600 s  (10 min + increment; increment ignored for baseline)
     * - `"1800"`  → 1800 s (30 min classical)
     * - `"-"`     → null   (unlimited/correspondence)
     * - `"?"`     → null   (unknown)
     *
     * Returns `null` if the format is unrecognised.
     */
    fun parseTimeControl(tc: String?): Int? {
        if (tc == null || tc == "-" || tc == "?") return null
        return runCatching {
            tc.substringBefore('+').trim().toInt()
        }.getOrNull()
    }

    /**
     * Convenience: parse `movesPgn`, convert to time-spent list, and return it.
     * Uses [parseTimeControl] for the initial-time baseline; if the time control is
     * absent or unrecognised the first clock reading is treated as the baseline.
     */
    fun extractTimeSpent(movesPgn: String, timeControlHeader: String?): List<Int> {
        val clocks  = parseMoveClocks(movesPgn)
        if (clocks.isEmpty()) return emptyList()
        val initial = parseTimeControl(timeControlHeader) ?: 0
        return computeTimeSpent(clocks, initial)
    }
}
