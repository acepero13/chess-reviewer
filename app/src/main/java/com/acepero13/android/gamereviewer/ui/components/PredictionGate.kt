package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.screens.GamePrediction
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors

@Composable
fun PredictionGate(
    visible:     Boolean,
    whitePlayer: String,
    blackPlayer: String,
    result:      String,
    onSubmit:    (GamePrediction) -> Unit,
    onSkip:      () -> Unit,
    modifier:    Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn() + slideInVertically { it / 3 },
        exit    = fadeOut() + slideOutVertically { it / 3 },
        modifier = modifier,
    ) {
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000)),
            contentAlignment = Alignment.Center,
        ) {
            var selected by remember { mutableStateOf<GamePrediction?>(null) }

            val appColors = LocalAppColors.current
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = appColors.surface),
                border = BorderStroke(1.dp, ChessGold.copy(alpha = 0.35f)),
            ) {
                Column(
                    modifier            = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text       = "Before you start reviewing…",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = ChessGold,
                    )
                    Text(
                        text  = "$whitePlayer vs $blackPlayer  ·  $result",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(color = ChessGold.copy(alpha = 0.2f))
                    Text(
                        text  = "What do you think was your key challenge?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    GamePrediction.entries.forEach { option ->
                        PredictionOption(
                            option     = option,
                            isSelected = selected == option,
                            onClick    = { selected = option },
                        )
                    }
                    HorizontalDivider(color = ChessGold.copy(alpha = 0.2f))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            onClick  = onSkip,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick  = { selected?.let(onSubmit) },
                            enabled  = selected != null,
                            modifier = Modifier.weight(2f),
                            shape    = RoundedCornerShape(8.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = ChessGold),
                        ) {
                            Text(
                                text       = "Start Review",
                                fontWeight = FontWeight.SemiBold,
                                color      = appColors.background,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PredictionOption(
    option:     GamePrediction,
    isSelected: Boolean,
    onClick:    () -> Unit,
    modifier:   Modifier = Modifier,
) {
    val appColors   = LocalAppColors.current
    val borderColor = if (isSelected) ChessGold else appColors.border
    val bgColor     = if (isSelected) Color(0x1FC9A84C) else Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .background(bgColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text  = option.emoji,
            style = MaterialTheme.typography.titleMedium,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = option.label,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = if (isSelected) ChessGold else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = option.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
