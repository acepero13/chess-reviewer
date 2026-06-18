package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.CriticalMoment

enum class TrainingCategory(
    val displayName: String,
    val emoji: String,
    val shortDescription: String,
    val targetWeaknesses: Set<CriticalMoment.ReasonCategory>,
) {
    WOODPECKER(
        displayName      = "Pattern Recognition",
        emoji            = "🔁",
        shortDescription = "Solve the same critical positions repeatedly until you achieve " +
            "N correct in a row — builds automatic tactical reflexes.",
        targetWeaknesses = setOf(
            CriticalMoment.ReasonCategory.MISSED_TACTIC,
            CriticalMoment.ReasonCategory.HANGING_PIECE,
        ),
    ),
    BLIND_RECOGNITION(
        displayName      = "Blind Recognition",
        emoji            = "👁️",
        shortDescription = "The board is shown briefly, then pieces disappear. Make your " +
            "moves from memory and reveal to check — trains board visualisation.",
        targetWeaknesses = setOf(
            CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE,
            CriticalMoment.ReasonCategory.KING_SAFETY,
        ),
    ),
    DEEP_CALCULATION(
        displayName      = "Deep Calculation",
        emoji            = "🧮",
        shortDescription = "Submit your best move and the opponent's best reply in sequence. " +
            "The engine validates both plies — sharpens 2-ply ahead calculation.",
        targetWeaknesses = setOf(
            CriticalMoment.ReasonCategory.MISSED_TACTIC,
            CriticalMoment.ReasonCategory.MISSED_WIN,
        ),
    ),
    PREDICT_OPPONENT(
        displayName      = "Predict Opponent",
        emoji            = "🤔",
        shortDescription = "The engine plays a move against you — find the correct defensive " +
            "response. Trains anticipation of opponent threats.",
        targetWeaknesses = setOf(
            CriticalMoment.ReasonCategory.KING_SAFETY,
            CriticalMoment.ReasonCategory.STRATEGIC_MISTAKE,
        ),
    ),
    SPEED_DRILL(
        displayName      = "Speed Drill",
        emoji            = "⚡",
        shortDescription = "Solve a sequence of positions under a countdown timer — builds " +
            "confident, fast pattern recognition under time pressure.",
        targetWeaknesses = setOf(
            CriticalMoment.ReasonCategory.TIME_PRESSURE,
        ),
    ),
    OPENING_REHEARSAL(
        displayName      = "Opening Rehearsal",
        emoji            = "📖",
        shortDescription = "Replay your most-deviated opening lines move by move with instant " +
            "feedback — anchors opening knowledge to memory.",
        targetWeaknesses = setOf(
            CriticalMoment.ReasonCategory.OPENING_DEVIATION,
        ),
    ),
    ENDGAME_TECHNIQUE(
        displayName      = "Endgame Technique",
        emoji            = "♟️",
        shortDescription = "Drill classic endgame positions (Lucena, Philidor, K+P) with guided " +
            "technique checking — converts theoretical knowledge to muscle memory.",
        targetWeaknesses = setOf(
            CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE,
        ),
    ),
}
