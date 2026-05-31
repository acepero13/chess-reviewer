package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.screens.GamePrediction
import com.acepero13.android.gamereviewer.ui.screens.PredictionMatchResult
import com.acepero13.chess.core.ui.theme.ChessGold

private val AccurateBg     = Color(0xFF0D2010)
private val AccurateBorder = Color(0xFF2D6A4F)
private val AccurateText   = Color(0xFF81C995)
private val NeutralBg      = Color(0xFF181400)
private val NeutralBorder  = Color(0xFF4A3800)
private val NeutralText    = Color(0xFFD4B870)

@Composable
fun PostGameDebrief(
    visible:      Boolean,
    prediction:   GamePrediction?,
    matchResult:  PredictionMatchResult?,
    onDismiss:    () -> Unit,
    onViewReport: () -> Unit,
    modifier:     Modifier = Modifier,
) {
    AnimatedVisibility(
        visible  = visible && matchResult != null,
        enter    = fadeIn() + expandVertically(),
        exit     = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        val result    = matchResult ?: return@AnimatedVisibility
        val hasComparison = prediction != null && prediction != GamePrediction.NOT_SURE
        val accurate  = result.isAccurate && hasComparison
        val bg        = if (accurate) AccurateBg else NeutralBg
        val border    = if (accurate) AccurateBorder else NeutralBorder
        val textColor = if (accurate) AccurateText else NeutralText

        Card(
            modifier = modifier.border(1.dp, border, RoundedCornerShape(10.dp)),
            shape    = RoundedCornerShape(10.dp),
            colors   = CardDefaults.cardColors(containerColor = bg),
        ) {
            Column(
                modifier            = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text       = if (accurate) "✓" else "→",
                            style      = MaterialTheme.typography.titleSmall,
                            color      = textColor,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text       = if (accurate) "Good read" else "Engine verdict",
                            style      = MaterialTheme.typography.labelMedium,
                            color      = textColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Dismiss",
                            tint     = textColor.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }

                if (hasComparison && prediction != null) {
                    Text(
                        text  = "You predicted: ${prediction.emoji} ${prediction.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.65f),
                    )
                }

                Text(
                    text       = result.headline,
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = textColor,
                    fontWeight = FontWeight.SemiBold,
                )

                result.detail?.let {
                    Text(
                        text  = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.8f),
                    )
                }

                TextButton(
                    onClick  = onViewReport,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        text  = "View full report →",
                        style = MaterialTheme.typography.labelMedium,
                        color = ChessGold,
                    )
                }
            }
        }
    }
}
