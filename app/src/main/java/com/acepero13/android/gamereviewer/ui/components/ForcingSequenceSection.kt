package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.chess.core.ui.theme.AnalyzeBlue
import com.acepero13.chess.core.ui.theme.ChessGold

private val SectionBorder = Color(0xFF2D6A4F)

internal data class ForcingSequenceStrings(
    val animatingLabel:  String,
    val activePrompt:    String,
    val sandboxPrompt:   String,
    val completeMessage: String,
    val tryLabel:        String,
    val giveUpLabel:     String,
)

@Composable
internal fun ForcingSequenceSection(
    strings:     ForcingSequenceStrings,
    animating:   Boolean,
    complete:    Boolean,
    mode:        Boolean,
    currentStep: Int,
    totalSteps:  Int,
    onTry:       (() -> Unit)?,
    onShow:      (() -> Unit)?,
    onReplay:    (() -> Unit)?,
    onDismiss:   () -> Unit,
) {
    when {
        animating -> AnimatingContent(strings, currentStep, totalSteps)
        complete  -> CompleteContent(strings, onReplay, onDismiss)
        mode      -> SandboxContent(strings, onShow)
        else      -> IdleContent(strings, onTry, onShow, onDismiss)
    }
}

@Composable
private fun AnimatingContent(strings: ForcingSequenceStrings, currentStep: Int, totalSteps: Int) {
    val progress = if (totalSteps > 0) currentStep.toFloat() / totalSteps else 0f
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(strings.animatingLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = AnalyzeBlue, trackColor = SectionBorder.copy(alpha = 0.3f))
        Text("Move $currentStep of $totalSteps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

@Composable
private fun CompleteContent(strings: ForcingSequenceStrings, onReplay: (() -> Unit)?, onDismiss: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(strings.completeMessage, style = MaterialTheme.typography.bodySmall, color = AnalyzeBlue.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onReplay?.invoke() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, AnalyzeBlue.copy(alpha = 0.7f))) {
                Text("Replay", color = AnalyzeBlue, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, SectionBorder.copy(alpha = 0.5f))) {
                Text("Explore freely", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SandboxContent(strings: ForcingSequenceStrings, onShow: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(strings.sandboxPrompt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f), fontStyle = FontStyle.Italic)
        Button(onClick = { onShow?.invoke() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Text(strings.giveUpLabel, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun IdleContent(strings: ForcingSequenceStrings, onTry: (() -> Unit)?, onShow: (() -> Unit)?, onDismiss: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(strings.activePrompt, style = MaterialTheme.typography.bodySmall, color = ChessGold.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onTry?.invoke(); onDismiss() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = SectionBorder)) {
                Text(strings.tryLabel, color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(onClick = { onShow?.invoke() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, SectionBorder.copy(alpha = 0.5f))) {
                Text("Just show me", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
