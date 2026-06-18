package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors
import kotlin.math.roundToInt

private val BlunderColor    = Color(0xFFEF5350)
private val MistakeColor    = Color(0xFFFFB74D)
private val InaccuracyColor = Color(0xFFFFD54F)

/**
 * Headline accuracy + move-quality counts aggregated across all analyzed games.
 */
@Composable
fun AccuracyStatsCard(
    avgAccuracy: Float,
    avgAcpl: Int,
    blunders: Int,
    mistakes: Int,
    inaccuracies: Int,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    Card(
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = appColors.surface),
        border   = BorderStroke(1.dp, appColors.border),
        modifier = modifier,
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                BigStat(value = "${avgAccuracy.roundToInt()}%", label = "Accuracy", color = ChessGold)
                BigStat(value = "$avgAcpl", label = "Avg CPL", color = appColors.textPrimary)
            }
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                BigStat(value = "$blunders", label = "Blunders", color = BlunderColor)
                BigStat(value = "$mistakes", label = "Mistakes", color = MistakeColor)
                BigStat(value = "$inaccuracies", label = "Inaccuracies", color = InaccuracyColor)
            }
        }
    }
}

@Composable
private fun BigStat(value: String, label: String, color: Color) {
    val appColors = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = color,
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = appColors.textSecondary,
        )
    }
}
