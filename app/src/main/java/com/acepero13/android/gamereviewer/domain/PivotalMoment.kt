package com.acepero13.android.gamereviewer.domain

enum class PivotalMomentRole(
    val label: String,
    val emoji: String,
    val tagline: String,
    val description: String,
) {
    TURNING_POINT(
        label       = "The Turning Point",
        emoji       = "⚖️",
        tagline     = "Where the game's balance shifted",
        description = "The single move where the position changed most dramatically.",
    ),
    MISSED_OPPORTUNITY(
        label       = "Missed Opportunity",
        emoji       = "🎯",
        tagline     = "Where you could have taken control",
        description = "You had the tools to seize the advantage — it slipped through.",
    ),
    EDUCATIONAL_MOMENT(
        label       = "Educational Moment",
        emoji       = "📚",
        tagline     = "Your recurring pattern — most to learn here",
        description = "This error appears across your games. Understanding it matters most.",
    ),
}

data class PivotalMoment(
    val moveIndex: Int,
    val fen: String,
    val evalDeltaFromPlayer: Int,
    val motif: String,
    val role: PivotalMomentRole,
    val recurringCategory: String = "",
) {
    val fullMoveNumber: Int get() = (moveIndex + 1) / 2
    val isWhiteMove: Boolean get() = moveIndex % 2 == 1
    val sideLabel: String get() = if (isWhiteMove) "White" else "Black"

    val severityLabel: String get() = when {
        evalDeltaFromPlayer <= -400 -> "Major blunder"
        evalDeltaFromPlayer <= -200 -> "Significant mistake"
        evalDeltaFromPlayer <= -100 -> "Inaccuracy"
        else                        -> "Critical moment"
    }
}

data class PivotalMoments(
    val turningPoint: PivotalMoment?,
    val missedOpportunity: PivotalMoment?,
    val educationalMoment: PivotalMoment?,
) {
    val all: List<PivotalMoment>
        get() = listOfNotNull(turningPoint, missedOpportunity, educationalMoment)

    val moveIndices: List<Int>
        get() = all.map { it.moveIndex }

    companion object {
        val EMPTY = PivotalMoments(null, null, null)
    }
}
