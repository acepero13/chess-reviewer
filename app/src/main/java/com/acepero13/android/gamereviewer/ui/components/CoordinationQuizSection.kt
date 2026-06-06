package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.screens.CoordinationQuizPhase
import com.acepero13.chess.core.ui.theme.AnalyzeBlue

private val QuizBorder = Color(0xFF2D6A4F)

@Composable
internal fun CoordinationQuizSection(
    phase:    CoordinationQuizPhase,
    onReveal: () -> Unit,
) {
    HorizontalDivider(color = QuizBorder.copy(alpha = 0.25f))
    when (phase) {
        CoordinationQuizPhase.ASKING -> {
            Text(
                text      = "Can you spot how these pieces are working together? Try to identify the square they're all targeting before I show you.",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                fontStyle = FontStyle.Italic,
            )
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick  = onReveal,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(8.dp),
                    border   = BorderStroke(1.dp, AnalyzeBlue.copy(alpha = 0.7f)),
                ) { Text("I see it", color = AnalyzeBlue, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) }
                OutlinedButton(
                    onClick  = onReveal,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(8.dp),
                    border   = BorderStroke(1.dp, QuizBorder.copy(alpha = 0.7f)),
                ) { Text("Show me", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall) }
            }
        }
        CoordinationQuizPhase.REVEALING -> {
            Text("↑ Arrows show the coordination on the board.", style = MaterialTheme.typography.bodySmall, color = AnalyzeBlue.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
        }
    }
}
