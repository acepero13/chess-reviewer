package com.acepero13.android.gamereviewer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a pivotal move in a game — either flagged by the engine (hidden truth map)
 * or explicitly marked by the user during self-analysis.
 *
 * The [explanationState] drives the "Progressive Engine Reveal" flow:
 * HIDDEN → HINTED (guided questions shown) → REVEALED (engine line unlocked).
 */
@Entity(tableName = "critical_moments")
data class CriticalMoment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val moveIndex: Int,                     // 1-based move number (after the move was played)
    val type: String = Type.ENGINE_MARKED.name,
    val severity: Int = 0,                  // absolute centipawn delta
    val reasonCategory: String = ReasonCategory.STRATEGIC_MISTAKE.name,
    val explanationState: String = ExplanationState.HIDDEN.name,
    val fen: String = "",                   // FEN after this move
) {
    enum class Type { USER_MARKED, ENGINE_MARKED }

    enum class ReasonCategory {
        MISSED_TACTIC,
        OPENING_DEVIATION,
        HANGING_PIECE,
        KING_SAFETY,
        ENDGAME_PRINCIPLE,
        STRATEGIC_MISTAKE,
        TIME_PRESSURE,
        MISSED_WIN,
    }

    enum class ExplanationState { HIDDEN, HINTED, REVEALED }

    fun toType() = Type.valueOf(type)
    fun toReason() = ReasonCategory.valueOf(reasonCategory)
    fun toExplanation() = ExplanationState.valueOf(explanationState)
}
