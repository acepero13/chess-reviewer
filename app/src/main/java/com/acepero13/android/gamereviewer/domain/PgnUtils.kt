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
    // Remove black-move restart notation: "23..." / "23. ..." / "10 ..."
    // These appear after comments in annotated PGN and confuse chesslib's loadFromSan
    val noRestarts = noNags.replace(Regex("""\b\d+\.?\s*\.{2,}"""), "")
    // Remove move numbers without a trailing dot (e.g. "1 e4" style PGN).
    // chesslib treats bare numbers as SAN tokens and throws MoveConversionException.
    // Lookahead (?=\s|$) keeps numbers followed by "." (chesslib handles those).
    // Lookbehind (?<!-) prevents stripping the digits inside "0-0" / "0-0-0".
    val noBareMoveNums = noRestarts.replace(Regex("""(?<!-)\b\d+(?=\s|$)"""), "")
    // Normalise castling: some PGN files use zeros ("0-0") rather than the standard
    // capital-O form ("O-O") that chesslib requires. Replace longest first.
    val castlingNorm = noBareMoveNums.replace("0-0-0", "O-O-O").replace("0-0", "O-O")
    // Remove game-result token at the end (1-0, 0-1, 1/2-1/2, *)
    // chesslib's MoveList.loadFromSan throws MoveConversionException on these tokens
    val clean = castlingNorm.replace(Regex("""(?:\s+|^)(1-0|0-1|1/2-1/2|\*)\s*$"""), "").trim()
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
 * Extracts PGN brace comments and maps them to their 0-based half-move index.
 * Clock annotations ([%clk ...]) and NAG codes are skipped; only human-readable
 * main-line comments are captured (variation parentheses and their content are ignored).
 *
 * Example: "1. e4 {King's pawn opening} e5 2. Nf3" → {0 → "King's pawn opening"}
 */
fun extractMoveAnnotations(movesPgn: String): Map<Int, String> {
    val result = mutableMapOf<Int, String>()
    var halfMoveIndex = -1
    var i = 0
    var parenDepth = 0

    while (i < movesPgn.length) {
        val ch = movesPgn[i]
        when {
            ch == '(' -> { parenDepth++; i++ }
            ch == ')' -> { if (parenDepth > 0) parenDepth--; i++ }
            parenDepth > 0 -> i++
            ch == '{' -> {
                val start = i + 1
                val end = movesPgn.indexOf('}', start).takeIf { it >= 0 } ?: movesPgn.length
                val comment = movesPgn.substring(start, end).trim()
                if (!comment.startsWith("[%") && comment.isNotBlank() && halfMoveIndex >= 0) {
                    result[halfMoveIndex] = comment
                }
                i = end + 1
            }
            ch.isDigit() && (i == 0 || movesPgn[i - 1].isWhitespace() || movesPgn[i - 1] == '{' || movesPgn[i - 1] == '}') -> {
                // Skip move number tokens like "1." or "23..."
                while (i < movesPgn.length && (movesPgn[i].isDigit() || movesPgn[i] == '.')) i++
            }
            ch.isLetter() -> {
                // A SAN move token — advance half-move index
                halfMoveIndex++
                while (i < movesPgn.length && !movesPgn[i].isWhitespace() && movesPgn[i] != '{' && movesPgn[i] != '(') i++
            }
            else -> i++
        }
    }
    return result
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
