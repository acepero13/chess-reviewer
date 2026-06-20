package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.acepero13.chess.core.ui.theme.LocalAppColors
import kotlin.math.roundToInt

private val RecoveryColor = Color(0xFFFFB74D)

/**
 * Oversight recovery rate: after an inaccuracy (or worse), how often the user's next move is a
 * best move — i.e. their ability to stop a blunder "streak" after the first slip.
 */
@Composable
fun RecoveryRateCard(
    recoveryRate: Float,
    oversightCount: Int,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    InsightCard(title = "Oversight recovery", modifier = modifier) {
        Text(
            text       = "${recoveryRate.roundToInt()}%",
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color      = RecoveryColor,
        )
        Text(
            text     = "of your moves right after an inaccuracy were best moves " +
                "(across $oversightCount inaccuracies). Higher means you steady the ship faster.",
            style    = MaterialTheme.typography.bodySmall,
            color    = appColors.textSecondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
