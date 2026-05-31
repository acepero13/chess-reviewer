package com.acepero13.android.gamereviewer.engine.highlights.rules

import com.acepero13.android.gamereviewer.engine.highlights.*

/** Detects when a piece is sacrificed to break through opponent's defences with a large eval swing. */
class BreakthroughSacrificeRule : HighlightRule {
    override val ruleType = "breakthrough_sacrifice"

    override fun evaluate(context: HighlightRuleContext): List<GameHighlight> {
        if (context.phase == GamePhase.OPENING) return emptyList()
        if (!context.isCapture) return emptyList()

        val next = context.nextContext ?: return emptyList()

        // Mover must give up material (piece lost on this move minus piece captured)
        val moverMaterialBefore = BoardAttackHelper.totalMaterial(context.boardBefore, context.moverColor)
        val moverMaterialAfter  = BoardAttackHelper.totalMaterial(context.boardAfter,  context.moverColor)
        val enemyMaterialBefore = BoardAttackHelper.totalMaterial(context.boardBefore, context.enemyColor)
        val enemyMaterialAfter  = BoardAttackHelper.totalMaterial(context.boardAfter,  context.enemyColor)

        val moverLost    = moverMaterialBefore - moverMaterialAfter
        val enemyLost    = enemyMaterialBefore - enemyMaterialAfter
        val netSacrifice = moverLost - enemyLost  // positive = mover sacrificed more than gained

        // Sacrifice must be at least a piece (~3pt) net
        if (netSacrifice < 3) return emptyList()

        // Immediate eval should not be catastrophic
        if (context.playerDelta < -1.5f) return emptyList()

        // After opponent responds, mover should be doing well
        if (next.playerDelta >= -0.5f) return emptyList()

        // Total eval gain across both plies must be significant
        val totalGain = context.playerDelta - next.playerDelta
        if (totalGain < 1.0f) return emptyList()

        val side = if (context.isWhiteMove) "White" else "Black"
        return listOf(
            GameHighlight(
                moveIndex      = context.moveIndex,
                moveNumber     = context.moveNumber,
                isWhiteMove    = context.isWhiteMove,
                moveSan        = context.moveSan,
                fenBefore      = context.fenBefore,
                phase          = context.phase,
                ruleType       = ruleType,
                severity       = HighlightSeverity.CRITICAL,
                title          = "Breakthrough sacrifice",
                description    = "$side sacrificed material with ${context.moveSan} to break through the opponent's defences — a powerful breakthrough.",
                improvementTip = "Sacrifices that open lines to the king or create decisive passed pawns are often the strongest moves. Don't be afraid to give up material for a concrete advantage."
            )
        )
    }
}
