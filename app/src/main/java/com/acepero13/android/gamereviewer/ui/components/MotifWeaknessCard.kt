package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.acepero13.android.gamereviewer.ui.screens.MotifWeakness

private val WeaknessColor = Color(0xFFBA68C8)

/**
 * Recurring weaknesses ranked by how often they have cost the user — combining engine-detected
 * motif blunders (forks, hanging pieces) with critical-moment reason categories from reviews.
 */
@Composable
fun MotifWeaknessCard(
    items: List<MotifWeakness>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val max = items.maxOf { it.count }.coerceAtLeast(1)
    InsightCard(title = "Recurring weaknesses", modifier = modifier) {
        items.forEach { item ->
            LabeledBar(
                label     = item.label,
                valueText = item.count.toString(),
                fraction  = item.count.toFloat() / max,
                color     = WeaknessColor,
            )
        }
    }
}
