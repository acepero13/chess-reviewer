package com.acepero13.android.gamereviewer.ui.screens.analysis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.screens.ReflectionItem
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors

internal val ReflectionBorder = Color(0xFF2D6A4F)
internal val CorrectGreen     = Color(0xFF4CAF50)
internal val WrongAmber       = Color(0xFFF0A500)

internal val REFLECTION_LABELS = listOf(
    "Safety Issue", "Multiple Plans", "Restricted Piece",
    "Forcing Move", "Opponent's Plan", "Pre-Move Check", "Rook Activation",
    "Conversion Strategy",
)

@Composable
internal fun BoardScanReflectionPanel(
    items:    List<ReflectionItem>,
    onAnswer: (moveIndex: Int, answer: String) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val answered  = items.count { it.userAnswer != null }
    val correct   = items.count { it.userAnswer == it.correctLabel }
    val appColors = LocalAppColors.current
    Column(modifier = modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Board Scan Review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
        Text("You navigated past these patterns. Can you name each one?", style = MaterialTheme.typography.bodySmall, color = appColors.textSecondary)
        HorizontalDivider(color = ReflectionBorder.copy(alpha = 0.4f))
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { item ->
                ReflectionItemCard(item = item, onAnswer = { answer -> onAnswer(item.moveIndex, answer) })
            }
        }
        if (answered == items.size) {
            HorizontalDivider(color = ReflectionBorder.copy(alpha = 0.4f))
            Text(
                text       = "Board Scan score: $correct / ${items.size}",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = if (correct == items.size) CorrectGreen else ChessGold,
            )
        }
        Button(
            onClick  = onFinish,
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(8.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = ReflectionBorder),
        ) {
            Text(
                text       = if (answered < items.size) "Skip reflection" else "Finish session",
                fontWeight = FontWeight.SemiBold,
                color      = Color.White,
            )
        }
    }
}

@Composable
internal fun ReflectionItemCard(item: ReflectionItem, onAnswer: (String) -> Unit) {
    val appColors = LocalAppColors.current
    Card(
        shape  = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = appColors.surface),
        border = BorderStroke(1.dp, ReflectionBorder),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text       = "Move ${(item.moveIndex + 1) / 2} — what coaching pattern was here?",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color      = appColors.textPrimary,
            )
            if (item.userAnswer == null) {
                REFLECTION_LABELS.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { label ->
                            OutlinedButton(
                                onClick  = { onAnswer(label) },
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(6.dp),
                                border   = BorderStroke(1.dp, ReflectionBorder),
                            ) { Text(label, style = MaterialTheme.typography.labelSmall, color = appColors.textPrimary) }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                val isCorrect = item.userAnswer == item.correctLabel
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text       = if (isCorrect) "Correct" else "Not quite",
                        style      = MaterialTheme.typography.bodySmall,
                        color      = if (isCorrect) CorrectGreen else WrongAmber,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("— you said: ${item.userAnswer}", style = MaterialTheme.typography.bodySmall, color = appColors.textSecondary)
                }
                if (!isCorrect) {
                    Text("Answer: ${item.correctLabel}", style = MaterialTheme.typography.bodySmall, color = CorrectGreen)
                }
            }
        }
    }
}
