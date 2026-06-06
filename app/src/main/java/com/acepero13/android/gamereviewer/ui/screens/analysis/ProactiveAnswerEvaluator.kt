package com.acepero13.android.gamereviewer.ui.screens.analysis

import androidx.compose.ui.graphics.Color
import com.acepero13.android.gamereviewer.domain.CoachingTrigger
import com.acepero13.chess.core.ui.board.MarkedSquare
import com.github.bhlangonijr.chesslib.Square

internal object ProactiveAnswerEvaluator {

    fun evaluate(
        trigger: CoachingTrigger,
        tapped: Square,
    ): Triple<String, Boolean?, List<MarkedSquare>> {
        val green = Color(0xFF22C55E); val red = Color(0xFFEF4444)
        fun sq(name: String?) = if (name.isNullOrBlank()) null
            else runCatching { Square.valueOf(name) }.getOrNull()
        return when (trigger) {
            is CoachingTrigger.WorstPiece -> evaluateWorstPiece(trigger, tapped, sq(trigger.pieceSquare), green, red)
            is CoachingTrigger.Safety -> evaluateSafety(trigger, tapped, sq(trigger.kingSquare), green, red)
            else -> Triple("Good thinking! Continue reviewing this position.", true, emptyList())
        }
    }

    private fun evaluateWorstPiece(
        trigger: CoachingTrigger.WorstPiece,
        tapped: Square,
        answer: Square?,
        green: Color,
        red: Color,
    ): Triple<String, Boolean?, List<MarkedSquare>> {
        if (answer != null && tapped == answer) {
            return Triple(
                "Correct! That piece has only ${trigger.mobility} move${if (trigger.mobility == 1) "" else "s"} — it needs rerouting.",
                true, listOf(MarkedSquare(tapped, green.copy(alpha = 0.6f))),
            )
        }
        return Triple(
            "Not quite — find the piece with the fewest legal moves.", false,
            buildList {
                add(MarkedSquare(tapped, red.copy(alpha = 0.5f)))
                if (answer != null) add(MarkedSquare(answer, green.copy(alpha = 0.6f)))
            },
        )
    }

    private fun evaluateSafety(
        trigger: CoachingTrigger.Safety,
        tapped: Square,
        kingSquare: Square?,
        green: Color,
        red: Color,
    ): Triple<String, Boolean?, List<MarkedSquare>> {
        val isKing = kingSquare != null && tapped == kingSquare
        return Triple(
            if (isKing) "That's your King — now count the friendly pieces guarding the adjacent squares."
            else "That's not your King. Tap the King to assess its exposure.",
            isKing,
            if (isKing) listOf(MarkedSquare(tapped, green.copy(alpha = 0.6f)))
            else buildList {
                add(MarkedSquare(tapped, red.copy(alpha = 0.5f)))
                if (kingSquare != null) add(MarkedSquare(kingSquare, green.copy(alpha = 0.6f)))
            },
        )
    }
}
