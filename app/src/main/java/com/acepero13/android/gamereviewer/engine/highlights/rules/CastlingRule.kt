package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*

/** Detects when either side castles. */
class CastlingRule : HighlightRule {
    override val ruleType = "castling"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        val san = context.moveSan
        if (!san.startsWith("O-O") && !san.startsWith("0-0")) return emptyList()

        val isKingside = !san.startsWith("O-O-O") && !san.startsWith("0-0-0")
        val side       = if (context.isWhiteMove) "White" else "Black"
        val wing       = if (isKingside) "kingside" else "queenside"

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
                title          = "Castled $wing",
                description    = "$side castled $wing.",
                improvementTip = "Castling is a key safety move. Always ensure your king is tucked away early."
            )
        )
    }
}
