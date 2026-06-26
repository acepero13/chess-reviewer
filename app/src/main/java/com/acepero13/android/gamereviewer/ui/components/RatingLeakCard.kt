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
import androidx.compose.ui.unit.sp
import com.acepero13.android.gamereviewer.domain.RatingLeak
import com.acepero13.chess.core.ui.theme.LocalAppColors

private val LeakRed = Color(0xFFE57373)
private val LeakSurface = Color(0xFF2A1719)
private val TipSurface = Color(0xFF3A1F22)

/**
 * Hero "rating leak" card: a big (estimated) ELO figure lost to blunders, the headline insight,
 * and a coaching tip. The ELO number is a deliberately rough heuristic — labelled as such.
 */
@Composable
fun RatingLeakCard(
    leak: RatingLeak,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    Card(
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = LeakSurface),
        border   = BorderStroke(1.dp, LeakRed.copy(alpha = 0.4f)),
        modifier = modifier,
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(0.4f)) {
                    Text(
                        text  = "RATING LEAK",
                        style = MaterialTheme.typography.labelSmall,
                        color = LeakRed,
                    )
                    Text(
                        text       = "~${leak.estimatedEloLost}",
                        fontSize   = 40.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color      = appColors.textPrimary,
                        maxLines   = 1,
                        softWrap   = false,
                    )
                    Text(
                        text  = "Est. ELO lost to blunders",
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.textTertiary,
                    )
                }
                Column(
                    modifier            = Modifier.weight(0.6f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text  = "INSIGHT",
                        style = MaterialTheme.typography.labelSmall,
                        color = appColors.textTertiary,
                    )
                    Text(
                        text       = leak.headline,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = appColors.textPrimary,
                    )
                    Text(
                        text  = leak.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.textSecondary,
                    )
                }
            }

            TipBox(tip = leak.coachingTip)
        }
    }
}

@Composable
private fun TipBox(tip: String) {
    Card(
        shape  = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = TipSurface),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "💡", style = MaterialTheme.typography.bodyMedium)
            Text(
                text  = tip,
                style = MaterialTheme.typography.bodySmall,
                color = LeakRed.copy(alpha = 0.9f),
            )
        }
    }
}
