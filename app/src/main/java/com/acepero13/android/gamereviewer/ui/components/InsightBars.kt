package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.chess.core.ui.theme.LocalAppColors

/** Shared card shell for the Insights metric cards (matches PhaseAccuracyCard styling). */
@Composable
fun InsightCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = appColors.textPrimary,
            )
            content()
        }
    }
}

/**
 * A label + animated progress bar + trailing value, shared by the new Insights cards.
 * [fraction] drives the bar (0–1); [valueText] is the right-aligned readout.
 */
@Composable
fun LabeledBar(
    label: String,
    valueText: String,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    labelWidth: Int = 120,
) {
    val appColors = LocalAppColors.current
    val animated by animateFloatAsState(
        targetValue   = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700),
        label         = "insight_bar_$label",
    )
    Row(
        modifier              = modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodySmall,
            color    = appColors.textSecondary,
            modifier = Modifier.width(labelWidth.dp),
        )
        LinearProgressIndicator(
            progress   = { animated },
            modifier   = Modifier.weight(1f).height(8.dp),
            color      = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeCap  = StrokeCap.Round,
        )
        Text(
            text       = valueText,
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color      = color,
            modifier   = Modifier.width(52.dp),
        )
    }
}
