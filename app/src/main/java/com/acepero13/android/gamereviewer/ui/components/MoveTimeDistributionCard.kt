package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.acepero13.chess.core.ui.theme.LocalAppColors
import kotlin.math.roundToInt

private val WinningColor = Color(0xFF81C784)
private val LosingColor  = Color(0xFFE57373)

/**
 * Average time spent per move in winning vs losing positions — surfaces "blitzing" through
 * critical moments. A shorter bar when losing than when winning means the user speeds up when worse.
 */
@Composable
fun MoveTimeDistributionCard(
    winningSec: Float,
    losingSec: Float,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    val max = maxOf(winningSec, losingSec, 1f)
    InsightCard(title = "Move time distribution", modifier = modifier) {
        LabeledBar("Winning positions", winningSec.secLabel(), winningSec / max, WinningColor)
        LabeledBar("Losing positions", losingSec.secLabel(), losingSec / max, LosingColor)
        if (winningSec > 0f && losingSec > 0f && losingSec < winningSec) {
            Text(
                text     = "You speed up when worse — slow down in losing positions to avoid compounding mistakes.",
                style    = MaterialTheme.typography.bodySmall,
                color    = appColors.textSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun Float.secLabel(): String = if (this <= 0f) "—" else "${roundToInt()}s"
