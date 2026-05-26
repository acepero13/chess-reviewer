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
     */
    val hasTacticalMotif: Boolean get() = motif != "mixed"
}
