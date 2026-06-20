package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.acepero13.chess.core.ui.theme.LocalAppColors
import kotlin.math.roundToInt

private val SharpColor = Color(0xFFE57373)
private val QuietColor = Color(0xFF4FC3F7)

/**
 * How often the user's moves match the engine in **sharp** (tactical/volatile) positions versus
 * **quiet** ones. A large gap (quiet ≫ sharp) means calculation breaks down under complexity.
 */
@Composable
fun EngineCorrelationCard(
    sharpCorrelation: Float,
    quietCorrelation: Float,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    InsightCard(title = "Engine match: sharp vs quiet", modifier = modifier) {
        LabeledBar("Sharp positions", sharpCorrelation.pct(), sharpCorrelation / 100f, SharpColor)
        LabeledBar("Quiet positions", quietCorrelation.pct(), quietCorrelation / 100f, QuietColor)
        if (quietCorrelation - sharpCorrelation >= 10f) {
            Text(
                text     = "Your accuracy drops in sharp positions — practice calculation in tactical structures.",
                style    = MaterialTheme.typography.bodySmall,
                color    = appColors.textSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun Float.pct(): String = "${roundToInt()}%"
