package com.acepero13.android.gamereviewer.ui.screens.analysis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.screens.WeaknessContext
import com.acepero13.chess.core.ui.theme.ChessGold

@Composable
internal fun CoachsBriefingCard(ctx: WeaknessContext, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, ChessGold.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(ctx.trendEmoji, style = MaterialTheme.typography.titleSmall)
                    Text("Coach's Reading", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = ChessGold)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Outlined.Close, "Dismiss", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(ctx.trendTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ChessGold)
            Spacer(Modifier.height(2.dp))
            Text("Seen across ${ctx.gamesAffected} of ${ctx.totalGamesAnalyzed} analyzed games", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (ctx.matchingMoveIndices.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text("${ctx.matchingMoveIndices.size} moment(s) in this game match this pattern — reviewed first", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF8F00))
            }
        }
    }
}
