package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.BlunderDirection
import com.acepero13.chess.core.ui.theme.LocalAppColors

private val AllowedColor = Color(0xFFE57373)
private val MissedColor  = Color(0xFFFFD54F)

/**
 * Blunder direction: did you *create* a weakness ([BlunderDirection.allowed]) or *miss* punishing
 * the opponent's ([BlunderDirection.missed])?
 */
@Composable
fun BlunderDirectionCard(
    direction: BlunderDirection,
    modifier: Modifier = Modifier,
) {
    if (direction.total == 0) return
    val appColors = LocalAppColors.current
    val allowedFrac = direction.allowed.toFloat() / direction.total

    InsightCard(title = "Blunder direction", modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth().height(14.dp)) {
            if (direction.allowed > 0) {
                Box(
                    modifier = Modifier
                        .weight(allowedFrac.coerceAtLeast(0.001f))
                        .fillMaxHeight()
                        .background(AllowedColor, RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp)),
                )
            }
            if (direction.missed > 0) {
                Box(
                    modifier = Modifier
                        .weight((1f - allowedFrac).coerceAtLeast(0.001f))
                        .fillMaxHeight()
                        .background(MissedColor, RoundedCornerShape(topEnd = 7.dp, bottomEnd = 7.dp)),
                )
            }
        }
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DirectionLabel(
                title = "Allowed · ${direction.allowed}",
                subtitle = "You created a weakness",
                color = AllowedColor,
                modifier = Modifier.weight(1f),
            )
            DirectionLabel(
                title = "Missed · ${direction.missed}",
                subtitle = "Missed opponent's weakness",
                color = MissedColor,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DirectionLabel(
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    Column(modifier = modifier) {
        Text(
            text       = title,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color      = color,
        )
        Text(
            text  = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = appColors.textTertiary,
        )
    }
}
