package com.acepero13.android.gamereviewer.engine.highlights

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Side

/**
 * All data available to a [HighlightRule] for a single half-move.
 *
 * Board objects are lazily constructed from FEN strings to avoid parsing
 * every position when rules only need eval data.
 */
data class HighlightRuleContext(
    val moveIndex: Int,               // 1-based half-move number
    val moveNumber: Int,              // chess move number (1,1,2,2,...)
    val isWhiteMove: Boolean,
    val moveSan: String,
    val fenBefore: String,
    val fenAfter: String,
    /** White-perspective eval before this move (pawns). */
    val evalBefore: Float,
    /** White-perspective eval after this move (pawns). */
    val evalAfter: Float,
    /** Eval change from the mover's perspective (pawns). Positive = mover improved. */
    val playerDelta: Float,
    val phase: GamePhase,
    val isCapture: Boolean,
    val isCheck: Boolean,
    val isPawnMove: Boolean,
    /** Seconds spent on this move; null when no PGN clock annotations. */
    val timeSpentSeconds: Int?,
    /** Clock remaining after this move; null when unavailable. */
    val clockRemainingSeconds: Int?,
    val prevContext: HighlightRuleContext? = null,
    val nextContext: HighlightRuleContext? = null,
) {
    val moverColor: Side = if (isWhiteMove) Side.WHITE else Side.BLACK
    val enemyColor: Side = if (isWhiteMove) Side.BLACK else Side.WHITE

    val boardBefore: Board by lazy { Board().apply { loadFromFen(fenBefore) } }
    val boardAfter:  Board by lazy { Board().apply { loadFromFen(fenAfter) } }
}
