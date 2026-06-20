package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.acepero13.android.gamereviewer.ui.screens.StructureAccuracy
import kotlin.math.roundToInt

private val StructureColor = Color(0xFF4DB6AC)

/**
 * Average accuracy bucketed by middlegame pawn structure (IQP, Hanging Pawns, …) — points the user
 * at exactly which structures to study next. Game count shown alongside each structure.
 */
@Composable
fun PawnStructureCard(
    items: List<StructureAccuracy>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    InsightCard(title = "Accuracy by pawn structure", modifier = modifier) {
        items.forEach { item ->
            LabeledBar(
                label      = "${item.label} (${item.games})",
                valueText  = "${item.accuracy.roundToInt()}%",
                fraction   = item.accuracy / 100f,
                color      = StructureColor,
                labelWidth = 150,
            )
        }
    }
}
