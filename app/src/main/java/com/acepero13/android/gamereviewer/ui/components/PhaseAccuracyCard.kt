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
import kotlin.math.roundToInt

private val OpeningColor    = Color(0xFF4FC3F7)
private val MiddlegameColor = Color(0xFFFFB74D)
private val EndgameColor    = Color(0xFFEF9A9A)
private val AttackColor     = Color(0xFFE57373)
private val DefenseColor    = Color(0xFF81C784)

/**
 * Accuracy by game phase, with the middlegame split into attacking (pressing an advantage)
 * vs defensive (worse / holding) play. Values are 0–100% accuracy averages across games.
 */
@Composable
fun PhaseAccuracyCard(
    openingAccuracy: Float,
    middlegameAccuracy: Float,
    endgameAccuracy: Float,
    attackAccuracy: Float,
    defenseAccuracy: Float,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AccuracyRow("Opening", openingAccuracy, OpeningColor)
            AccuracyRow("Middlegame", middlegameAccuracy, MiddlegameColor)
            AccuracyRow("· Attacking", attackAccuracy, AttackColor)
            AccuracyRow("· Defending", defenseAccuracy, DefenseColor)
            AccuracyRow("Endgame", endgameAccuracy, EndgameColor)
        }
    }
}

@Composable
private fun AccuracyRow(label: String, accuracy: Float, color: Color) {
    val appColors = LocalAppColors.current
    val animated by animateFloatAsState(
        targetValue   = (accuracy / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700),
        label         = "phase_acc_$label",
    )
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodySmall,
            color    = appColors.textSecondary,
            modifier = Modifier.width(96.dp),
        )
        LinearProgressIndicator(
            progress   = { animated },
            modifier   = Modifier.weight(1f).height(8.dp),
            color      = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeCap  = StrokeCap.Round,
        )
        Text(
            text       = if (accuracy <= 0f) "—" else "${accuracy.roundToInt()}%",
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color      = color,
            modifier   = Modifier.width(44.dp),
        )
    }
}
