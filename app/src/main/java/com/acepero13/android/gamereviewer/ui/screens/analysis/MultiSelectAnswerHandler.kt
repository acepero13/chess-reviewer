package com.acepero13.android.gamereviewer.ui.screens.analysis

import androidx.compose.ui.graphics.Color
import com.acepero13.android.gamereviewer.domain.CoachingTrigger
import com.acepero13.chess.core.ui.board.MarkedSquare
import com.github.bhlangonijr.chesslib.Square
import kotlinx.coroutines.flow.update

internal class MultiSelectAnswerHandler(private val session: GameSession) {

    fun handle(
        trigger: CoachingTrigger,
        state: com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState,
        square: Square,
    ) {
        val green = Color(0xFF22C55E); val blue = Color(0xFF3B82F6); val red = Color(0xFFEF4444)
        val sqName = square.name
        if (sqName in state.proactiveHangingSquares) {
            val newFound = state.proactiveFoundSquares + sqName
            val allFound = newFound.size == state.proactiveHangingSquares.size
            val hitColor = if (sqName in state.proactiveHangingOwnSquares) green else blue
            session.uiState.update {
                it.copy(
                    proactiveAnswerFeedback = buildFoundFeedback(trigger, newFound.size, state.proactiveHangingSquares.size, allFound),
                    proactiveAnswerIsCorrect = true, proactiveFoundSquares = newFound,
                    proactiveInteractiveMode = !allFound,
                    coachHighlightSquares = it.coachHighlightSquares + MarkedSquare(square, hitColor.copy(alpha = 0.6f)),
                )
            }
        } else {
            session.uiState.update {
                it.copy(
                    proactiveAnswerFeedback = buildMissFeedback(trigger),
                    proactiveAnswerIsCorrect = false,
                    coachHighlightSquares = it.coachHighlightSquares + MarkedSquare(square, red.copy(alpha = 0.45f)),
                )
            }
        }
    }

    private fun buildFoundFeedback(trigger: CoachingTrigger, found: Int, total: Int, allFound: Boolean) = when {
        trigger is CoachingTrigger.CctCheck && allFound ->
            "All $total opponent CCT target${if (total == 1) "" else "s"} found! Good threat awareness."
        trigger is CoachingTrigger.CctCheck ->
            "Correct! $found/$total found — keep scanning for checks and captures."
        allFound -> "All $total hanging piece${if (total == 1) "" else "s"} found! Great board scan."
        else -> "Correct! $found/$total found — keep looking for more loose pieces."
    }

    private fun buildMissFeedback(trigger: CoachingTrigger) =
        if (trigger is CoachingTrigger.CctCheck)
            "That square isn't targeted by a check or capture. Look for opponent moves that win material or give check."
        else
            "That piece is adequately defended. Count attackers vs. defenders — find one where attackers win."
}
