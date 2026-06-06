package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.CoachingTrigger
import com.acepero13.chess.core.ui.theme.ChessGold

@Composable
internal fun BoardAnswerSection(
    trigger:              CoachingTrigger,
    interactive:          Boolean,
    feedback:             String?,
    feedbackIsCorrect:    Boolean?,
    foundCount:           Int,
    totalCount:           Int,
    onStartInteraction:   () -> Unit,
) {
    when {
        interactive -> {
            val hint = when {
                trigger is CoachingTrigger.CctCheck && totalCount > 0         -> "👆 Tap each square your opponent can Check or Capture ($foundCount/$totalCount found)…"
                trigger is CoachingTrigger.CctCheck                           -> "👆 Tap squares where your opponent has a Check or Capture…"
                trigger is CoachingTrigger.PreMoveChecklist && totalCount > 0 -> "👆 Tap each hanging piece on the board ($foundCount/$totalCount found)…"
                trigger is CoachingTrigger.PreMoveChecklist                   -> "👆 Tap the undefended piece(s) on the board…"
                else                                                          -> "👆 Tap the square on the board to answer…"
            }
            Card(
                shape    = RoundedCornerShape(8.dp),
                colors   = CardDefaults.cardColors(containerColor = Color(0xFFB45309).copy(alpha = 0.15f)),
                border   = BorderStroke(1.dp, Color(0xFFB45309).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(hint, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFBBF24), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
        feedback != null -> {
            val feedbackColor = when (feedbackIsCorrect) {
                true  -> Color(0xFF22C55E)
                false -> Color(0xFFEF4444)
                null  -> MaterialTheme.colorScheme.onSurface
            }
            Text(feedback, style = MaterialTheme.typography.bodySmall, color = feedbackColor, fontWeight = FontWeight.SemiBold)
        }
        else -> {
            OutlinedButton(
                onClick  = onStartInteraction,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(8.dp),
                border   = BorderStroke(1.dp, ChessGold.copy(alpha = 0.7f)),
            ) { Text("Identify on board ↓", color = ChessGold, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) }
        }
    }
}
