package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.data.repository.TriggerMasteryRepository
import com.acepero13.android.gamereviewer.ui.screens.HabitMasteryRow
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors

private val MasteredGreen = Color(0xFF4CAF50)
private val InProgress    = Color(0xFF2D6A4F)

/**
 * Dashboard card showing the user's Board Scan habit mastery progress.
 *
 * Each row represents one coaching trigger type. A progress bar fills as the user
 * correctly identifies the pattern in Reflection Mode. A green checkmark appears
 * once the streak reaches [TriggerMasteryRepository.MASTERY_THRESHOLD].
 */
@Composable
fun HabitProgressCard(
    rows:      List<HabitMasteryRow>,
    modifier:  Modifier = Modifier,
    showTitle: Boolean = true,
) {
    if (rows.isEmpty()) return

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
            if (showTitle) {
                Text(
                    text       = "Board Scan Habits",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = ChessGold,
                )
            }
            Text(
                text  = "Correctly identify each pattern ${TriggerMasteryRepository.MASTERY_THRESHOLD}× in Reflection Mode to master it — the Coach Lamp then goes silent for that habit.",
                style = MaterialTheme.typography.bodySmall,
                color = appColors.textTertiary,
            )

            rows.forEach { row ->
                HabitRow(row = row)
            }
        }
    }
}

@Composable
private fun HabitRow(row: HabitMasteryRow) {
    val appColors = LocalAppColors.current
    val progress = (row.streak.toFloat() / TriggerMasteryRepository.MASTERY_THRESHOLD).coerceIn(0f, 1f)

    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (row.mastered) {
            Icon(
                imageVector        = Icons.Outlined.CheckCircle,
                contentDescription = "Mastered",
                tint               = MasteredGreen,
                modifier           = Modifier.size(18.dp),
            )
        } else {
            Spacer(Modifier.size(18.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text       = row.label,
                    style      = MaterialTheme.typography.bodySmall,
                    fontWeight = if (row.mastered) FontWeight.Bold else FontWeight.Normal,
                    color      = if (row.mastered) MasteredGreen else appColors.textSecondary,
                )
                Text(
                    text  = if (row.mastered) "Mastered"
                            else "${row.streak} / ${TriggerMasteryRepository.MASTERY_THRESHOLD}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (row.mastered) MasteredGreen else appColors.textTertiary,
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress         = { progress },
                modifier         = Modifier.fillMaxWidth().height(4.dp),
                color            = if (row.mastered) MasteredGreen else InProgress,
                trackColor       = appColors.border,
                strokeCap        = StrokeCap.Round,
            )
        }
    }
}
