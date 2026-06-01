package com.acepero13.android.gamereviewer.domain

/**
 * A single entry in the hidden "Truth Map" — the engine's verdict on one position.
 *
 * This data is never surfaced directly to the user; it drives the Insight Reconciliation
 * System (missed-moment detection, blunder guard, progressive reveal).
 *
 * @param moveIndex 1-based index of the move (1 = position after move 1).
 * @param fen       FEN string of the position after this move.
 * @param evalCp    Evaluation in centipawns from **White's perspective** (positive = White better).
 * @param evalDelta Swing relative to the previous position (negative = side that moved lost material/position).
 * @param motif     Tactical label from [MotifClassifier]: "checkmate", "fork", "hanging", "mixed".
 */
data class TruthMapEntry(
    val moveIndex: Int,
    val fen: String,
    val evalCp: Int,
    val evalDelta: Int,
    val motif: String,
    /** Comma-separated UCI moves from the engine's PV, up to [ChessConstants.MAX_FORCING_SEQUENCE_DEPTH] half-moves. */
    val pvLine: String = "",
) {
    /**
     * The side that just played this move (0 = White on moves 1,3,5…; 1 = Black on 2,4,6…).
     * moveIndex is 1-based, so White played on odd indices.
     */
    val isWhiteMove: Boolean get() = moveIndex % 2 == 1

    /**
     * Centipawn loss FROM THE PLAYER WHO MOVED'S perspective.
     * Positive = they improved; negative = they lost material/position.
     */
    val playerEvalDelta: Int get() = if (isWhiteMove) evalDelta else -evalDelta

    /**
     * True when this move is a blunder or large mistake (≥ 150 cp loss for the player who moved).
     */
    val isCritical: Boolean get() = playerEvalDelta <= -150

    /**
     * True when a tactical motif is present (excluding generic "mixed").
     *
     * NOTE: this is purely "any motif exists" and makes no claim about significance.
     * Use [isSignificantTacticalMiss] for critical-moment detection.
     */
    val hasTacticalMotif: Boolean get() = motif != "mixed"

    /**
     * True when a meaningful tactical motif is paired with a real eval drop (≥ 100 cp).
     *
     * Prevents the mentor from firing on incidental motif labels (e.g. "undefended material"
     * classified on a move where the evaluation only shifted 0.5–0.8 pawns and the piece
     * was never actually in danger). 100 cp is the value of a pawn — below that threshold
     * a tactical label is almost always noise from the classifier.
     */
    val isSignificantTacticalMiss: Boolean get() = motif != "mixed" && playerEvalDelta <= -100
}
