package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.acepero13.android.gamereviewer.domain.BlunderCause

private val CauseColor = Color(0xFFE57373)

/** "What causes your blunders" — flagged blunder reasons ranked by frequency. */
@Composable
fun BlunderCausesCard(
    causes: List<BlunderCause>,
    modifier: Modifier = Modifier,
) {
    if (causes.isEmpty()) return
    val max = causes.maxOf { it.count }.coerceAtLeast(1)
    InsightCard(title = "What causes your blunders", modifier = modifier) {
        causes.forEach { cause ->
            LabeledBar(
                label     = cause.label,
                valueText = cause.count.toString(),
                fraction  = cause.count.toFloat() / max,
                color     = CauseColor,
            )
        }
    }
}
