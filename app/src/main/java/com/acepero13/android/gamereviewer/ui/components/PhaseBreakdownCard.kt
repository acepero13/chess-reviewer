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
import com.acepero13.android.gamereviewer.ui.screens.PhaseBreakdown

private val CardBg          = Color(0xFF1A1A1A)
private val CardBorder      = Color(0xFF2A2A2A)
private val RowText         = Color(0xFFCCCCCC)
private val OpeningColor    = Color(0xFF4FC3F7)
private val MiddlegameColor = Color(0xFFFFB74D)
private val EndgameColor    = Color(0xFFEF9A9A)

/**
 * Dashboard card showing how critical mistakes are distributed across the three game phases.
 *
 * Phase classification uses [reasonCategory] as the primary signal
 * (OPENING_DEVIATION → Opening, ENDGAME_PRINCIPLE → Endgame) with
 * move index as fallback for all other categories.
 */
@Composable
fun PhaseBreakdownCard(
    breakdown: PhaseBreakdown,
    modifier:  Modifier = Modifier,
) {
    Card(
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = CardBg),
        border   = BorderStroke(1.dp, CardBorder),
        modifier = modifier,
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text  = "Where in the game do your critical mistakes occur? " +
                    "The longest bar is your highest-risk phase.",
                style = MaterialTheme.typography.bodySmall,
                color = RowText.copy(alpha = 0.6f),
            )

            PhaseRow(
                label = "Opening",
                count = breakdown.opening,
                frac  = breakdown.fraction(breakdown.opening),
                color = OpeningColor,
                total = breakdown.total,
            )
            PhaseRow(
                label = "Middlegame",
                count = breakdown.middlegame,
                frac  = breakdown.fraction(breakdown.middlegame),
                color = MiddlegameColor,
                total = breakdown.total,
            )
            PhaseRow(
                label = "Endgame",
                count = breakdown.endgame,
                frac  = breakdown.fraction(breakdown.endgame),
                color = EndgameColor,
                total = breakdown.total,
            )
        }
    }
}

@Composable
private fun PhaseRow(
    label: String,
    count: Int,
    frac:  Float,
    color: Color,
    total: Int,
) {
    val animatedFrac by animateFloatAsState(
        targetValue   = frac,
        animationSpec = tween(durationMillis = 700),
        label         = "phase_bar_$label",
    )
    val pct = if (total == 0) 0 else (frac * 100).toInt()

    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodySmall,
            color    = RowText,
            modifier = Modifier.width(82.dp),
        )
        LinearProgressIndicator(
            progress   = { animatedFrac },
            modifier   = Modifier.weight(1f).height(8.dp),
            color      = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeCap  = StrokeCap.Round,
        )
        Text(
            text       = "$count ($pct%)",
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color      = color,
            modifier   = Modifier.width(62.dp),
        )
    }
}
