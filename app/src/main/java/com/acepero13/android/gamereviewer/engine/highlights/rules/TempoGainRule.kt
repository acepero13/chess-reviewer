package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece

/** Detects moves that gain tempo by delivering check or attacking a high-value piece. */
class TempoGainRule : HighlightRule {
    override val ruleType = "tempo_gain"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.phase == GamePhase.OPENING) return emptyList()
        if (context.playerDelta < 0f) return emptyList()

        val next = context.nextContext ?: return emptyList()

        val isCheckMove      = context.isCheck
        val isValuableCapture = context.isCapture && capturedValue(context) >= 3

        if (!isCheckMove && !isValuableCapture) return emptyList()

        if (next.playerDelta >= -0.2f) return emptyList()

        val side     = if (context.isWhiteMove) "White" else "Black"
        val moveType = when {
            isCheckMove && isValuableCapture -> "check and capture"
            isCheckMove                       -> "check"
            else                              -> "capture"
        }
        return listOf(
            GameHighlight(
                moveIndex      = context.moveIndex,
                moveNumber     = context.moveNumber,
                isWhiteMove    = context.isWhiteMove,
                moveSan        = context.moveSan,
                fenBefore      = context.fenBefore,
                phase          = context.phase,
                ruleType       = ruleType,
                severity       = HighlightSeverity.NOTABLE,
                title          = "Tempo gain",
                description    = "$side gained a tempo with a $moveType (${context.moveSan}), forcing the opponent into a difficult response.",
                improvementTip = "Moves that gain tempo by creating threats are powerful. Look for checks and attacks on undefended pieces to keep your opponent on the back foot."
            )
        )
    }

    private fun capturedValue(context: HighlightRuleContext): Int {
        val destSq = BoardAttackHelper.destinationSquare(context.moveSan) ?: return 0
        val p = context.boardBefore.getPiece(destSq)
        return if (p != Piece.NONE) p.materialValue else 0
    }
}
