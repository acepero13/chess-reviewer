package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*
import com.github.bhlangonijr.chesslib.Piece

/** Detects strong tactical moves (check or capture) that meaningfully improved the position. */
class TacticalResourceRule : HighlightRule {
    override val ruleType = "tactical_resource"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.phase == GamePhase.OPENING) return emptyList()
        if (!context.isCapture && !context.isCheck) return emptyList()
        if (context.playerDelta < 0.3f) return emptyList()

        if (context.isCapture && !context.isCheck) {
            val destSq        = BoardAttackHelper.destinationSquare(context.moveSan) ?: return emptyList()
            val p             = context.boardBefore.getPiece(destSq)
            val capturedValue = if (p != Piece.NONE) p.materialValue else 0
            if (capturedValue <= 1) return emptyList()
        }

        val side     = if (context.isWhiteMove) "White" else "Black"
        val moveType = when {
            context.isCheck && context.isCapture -> "check and capture"
            context.isCheck                       -> "check"
            else                                  -> "capture"
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
                title          = "Tactical resource",
                description    = "$side found a strong tactical resource — a $moveType (${context.moveSan}) that improved the position.",
                improvementTip = "Always scan for checks and captures before moving. Tactical resources are often hidden in plain sight."
            )
        )
    }
}
