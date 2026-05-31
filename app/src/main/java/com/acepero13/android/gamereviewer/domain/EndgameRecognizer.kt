package com.acepero13.android.gamereviewer.domain

import com.acepero13.chess.core.endgame.EndgameClassifier
import com.acepero13.chess.core.endgame.EndgameEntry

/**
 * Records the first recognised endgame position in a game.
 *
 * @param firstEndgameMoveIndex 0-based index into the game's FEN sequence (0 = starting position).
 * @param entry                 The matched chapter entry from the "100 Endgames" database.
 * @param fen                   FEN of the position where the endgame was detected.
 */
data class EndgameClassification(
    val firstEndgameMoveIndex: Int,
    val entry: EndgameEntry,
    val fen: String,
)

/**
 * Scans a game's FEN sequence to find the first position that matches a known endgame
 * type from [EndgameClassifier].
 */
class EndgameRecognizer(private val classifier: EndgameClassifier) {

    /**
     * Iterates [fens] (index 0 = starting position) and returns the first position
     * where the material configuration matches a chapter in the "100 Endgames" database,
     * or `null` if no recognised endgame is reached.
     */
    fun analyze(fens: List<String>): EndgameClassification? {
        for ((index, fen) in fens.withIndex()) {
            val entry = classifier.classify(fen) ?: continue
            return EndgameClassification(
                firstEndgameMoveIndex = index,
                entry = entry,
                fen = fen,
            )
        }
        return null
    }
}
