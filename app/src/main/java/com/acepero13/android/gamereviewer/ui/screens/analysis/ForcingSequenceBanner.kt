package com.acepero13.android.gamereviewer.ui.screens.analysis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.chess.core.ui.theme.AnalyzeBlue
import com.acepero13.chess.core.ui.theme.ChessGold

private val ForcingPanelBg     = Color(0xFF1A2E1A)
private val ForcingPanelBorder = Color(0xFF2D6A4F)
private val ForcingPanelText   = Color(0xFFCCE8D9)

@Composable
internal fun ForcingSequenceBanner(
    animating:   Boolean,
    complete:    Boolean,
    currentStep: Int,
    totalSteps:  Int,
    onGiveUp:    () -> Unit,
    onReplay:    () -> Unit,
    onDone:      () -> Unit,
    modifier:    Modifier = Modifier,
) {
    Card(
        shape    = RoundedCornerShape(8.dp),
        colors   = CardDefaults.cardColors(containerColor = ForcingPanelBg),
        border   = BorderStroke(1.dp, ForcingPanelBorder.copy(alpha = 0.7f)),
        modifier = modifier,
    ) {
        when {
            animating -> AnimatingSequenceContent(currentStep, totalSteps)
            complete  -> CompleteSequenceContent(onReplay, onDone)
            else      -> IdleSequenceContent(onGiveUp)
        }
    }
}

@Composable
private fun AnimatingSequenceContent(currentStep: Int, totalSteps: Int) {
    val progress = if (totalSteps > 0) currentStep.toFloat() / totalSteps else 0f
    Column(
        modifier            = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text       = "Watching the forcing sequence… ($currentStep/$totalSteps)",
            style      = MaterialTheme.typography.bodySmall,
            color      = ForcingPanelText.copy(alpha = 0.9f),
            fontWeight = FontWeight.SemiBold,
        )
        LinearProgressIndicator(
            progress   = { progress },
            modifier   = Modifier.fillMaxWidth(),
            color      = AnalyzeBlue,
            trackColor = ForcingPanelBorder.copy(alpha = 0.3f),
        )
    }
}

@Composable
private fun CompleteSequenceContent(onReplay: () -> Unit, onDone: () -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text     = "Explore variations freely",
            style    = MaterialTheme.typography.bodySmall,
            color    = ForcingPanelText.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(
            onClick        = onReplay,
            shape          = RoundedCornerShape(6.dp),
            border         = BorderStroke(1.dp, AnalyzeBlue.copy(alpha = 0.7f)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        ) { Text("Replay", style = MaterialTheme.typography.labelSmall, color = AnalyzeBlue) }
        OutlinedButton(
            onClick        = onDone,
            shape          = RoundedCornerShape(6.dp),
            border         = BorderStroke(1.dp, ForcingPanelBorder.copy(alpha = 0.6f)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        ) { Text("Done", style = MaterialTheme.typography.labelSmall, color = ForcingPanelText.copy(alpha = 0.7f)) }
    }
}

@Composable
private fun IdleSequenceContent(onGiveUp: () -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text       = "Find the forcing sequence!",
            style      = MaterialTheme.typography.bodySmall,
            color      = ChessGold.copy(alpha = 0.9f),
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedButton(
            onClick        = onGiveUp,
            shape          = RoundedCornerShape(6.dp),
            border         = BorderStroke(1.dp, ForcingPanelBorder.copy(alpha = 0.6f)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        ) { Text("Give up", style = MaterialTheme.typography.labelSmall, color = ForcingPanelText.copy(alpha = 0.7f)) }
    }
}
