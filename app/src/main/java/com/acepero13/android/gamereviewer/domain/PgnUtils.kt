package com.acepero13.android.gamereviewer.domain

import android.util.Log
import com.github.bhlangonijr.chesslib.move.MoveList

private const val PTAG = "PgnUtils"

/**
 * Strips brace comments `{ … }` and variation parentheses `( … )` from a PGN
 * move-text string, returning only the main-line tokens.
 *
 * Chess.com and Lichess both embed clock annotations (`{ [%clk 0:09:58] }`) that
 * chesslib's `MoveList.loadFromSan` cannot handle — it calls `addSanMove("{")` and
 * throws `MoveConversionException`, leaving the game with an empty UCI move list.
 */
fun stripPgnAnnotations(pgn: String): String {
    val sb = StringBuilder(pgn.length)
    var braceDepth = 0
    var parenDepth = 0
    for (ch in pgn) {
        when {
            ch == '{' -> braceDepth++
            ch == '}' -> { if (braceDepth > 0) braceDepth--  }
            ch == '(' -> parenDepth++
            ch == ')' -> { if (parenDepth > 0) parenDepth--  }
            braceDepth == 0 && parenDepth == 0 -> sb.append(ch)
        }
    }
    return sb.toString()
}

/**
 * Parses a PGN move-text string (with or without clock annotations) into a
 * space-separated list of UCI moves, e.g. `"e2e4 e7e5 g1f3"`.
 *
 * Returns an empty string if parsing fails.
 */
fun pgnToUciMoves(movesPgn: String): String {
    val stripped = stripPgnAnnotations(movesPgn)
    // Remove NAG annotations ($1, $18, …) which chesslib cannot parse
    val noNags = stripped.replace(Regex("""\$\d+"""), " ")
    // Remove game-result token at the end (1-0, 0-1, 1/2-1/2, *)
    // chesslib's MoveList.loadFromSan throws MoveConversionException on these tokens
    val clean = noNags.replace(Regex("""(?:\s+|^)(1-0|0-1|1/2-1/2|\*)\s*$"""), "").trim()
    Log.d(PTAG, "pgnToUciMoves: input.length=${movesPgn.length}  clean.length=${clean.length}  clean_preview='${clean.take(80)}'")
    return runCatching {
        val ml = MoveList()
        ml.loadFromSan(clean)
        val uci = ml.joinToString(" ") { m ->
            "${m.from.name.lowercase()}${m.to.name.lowercase()}" +
                (if (m.promotion != com.github.bhlangonijr.chesslib.Piece.NONE)
                    m.promotion.fenSymbol.lowercase() else "")
        }
        Log.d(PTAG, "pgnToUciMoves: parsed ${ml.size} moves — uci_preview='${uci.take(60)}'")
        uci
    }.getOrElse { e ->
        Log.e(PTAG, "pgnToUciMoves: loadFromSan FAILED — ${e.javaClass.simpleName}: ${e.message}")
        ""
    }
}

/**
 * Extracts the move-text block from a full PGN string (headers + moves) and
 * converts it to a UCI move list.  Used as a fallback when `movesUci` was stored
 * empty due to the comment-parsing bug.
 */
fun extractUciMovesFromFullPgn(pgn: String): String {
    // Split off the header block (lines starting with "[")
    val lines = pgn.lines()
    val sb = StringBuilder()
    var pastHeaders = false
    for (line in lines) {
        val t = line.trim()
        if (!pastHeaders && t.startsWith("[")) continue
        if (t.isEmpty()) { pastHeaders = true; continue }
        pastHeaders = true
        sb.append(t).append(' ')
    }
    val movesPgn = sb.toString().trim()
    return if (movesPgn.isBlank()) "" else pgnToUciMoves(movesPgn)
}
