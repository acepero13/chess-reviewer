package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.screens.MentorMoveResult

@Composable
private fun MoveFeedbackCard(result: MentorMoveResult, feedback: String, onRetry: () -> Unit) {
    val (bg, fg) = when (result) {
        MentorMoveResult.CORRECT   -> CorrectGreen to CorrectOnGreen
        MentorMoveResult.CLOSE     -> CloseAmber   to CloseOnAmber
        MentorMoveResult.INCORRECT -> WrongRed     to WrongOnRed
    }
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(bg).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(feedback, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = fg)
        if (result == MentorMoveResult.INCORRECT) {
            FilledTonalButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Try a different move")
            }
        }
    }
}

@Composable
internal fun MoveInputSection(
    active: Boolean, checking: Boolean, result: MentorMoveResult?,
    feedback: String, answerRevealed: Boolean,
    onToggle: () -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier,
) {
    if (answerRevealed) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (result != null && feedback.isNotBlank())
            MoveFeedbackCard(result = result, feedback = feedback, onRetry = onRetry)
        if (result == null || result == MentorMoveResult.INCORRECT) {
            if (!active) {
                OutlinedButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.SportsEsports, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Play your answer on the board")
                }
            } else if (!checking) {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Tap a piece, then tap its destination square.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
