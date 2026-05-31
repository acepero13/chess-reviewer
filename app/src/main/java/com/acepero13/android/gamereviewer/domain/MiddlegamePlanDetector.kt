package com.acepero13.android.gamereviewer.domain

import com.acepero13.chess.core.middlegame.MiddlegamePlanClassifier

class MiddlegamePlanDetector(private val classifier: MiddlegamePlanClassifier) {

    fun detect(
        fens: List<String>,
        playerIsWhite: Boolean,
        startFromIndex: Int,
    ): MiddlegamePlanClassification? {
        if (fens.isEmpty()) return null
        val clamped = startFromIndex.coerceIn(fens.indices)
        for (offset in 0..4) {
            val idx = clamped + offset * 2
            if (idx !in fens.indices) break
            val plans = classifier.classify(fens[idx], playerIsWhite).take(3)
            if (plans.isNotEmpty()) {
                return MiddlegamePlanClassification(
                    moveIndex = idx,
                    plans     = plans,
                    fen       = fens[idx],
                )
            }
        }
        return null
    }
}
