package com.acepero13.android.gamereviewer.ui.screens.analysis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors

private val BannerBorder = Color(0xFFC9A84C)

@Composable
internal fun MissedMomentBanner(visible: Boolean, onReview: () -> Unit, onDismiss: () -> Unit) {
    val appColors = LocalAppColors.current
    AnimatedVisibility(visible, enter = fadeIn() + slideInVertically { -it }, exit = fadeOut() + slideOutVertically { -it }) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(containerColor = ChessGold.copy(alpha = 0.10f)),
            shape    = RoundedCornerShape(8.dp),
            border   = BorderStroke(1.dp, BannerBorder.copy(alpha = 0.5f)),
        ) {
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Lightbulb, null, tint = ChessGold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Review suggestion available", style = MaterialTheme.typography.bodySmall, color = appColors.textPrimary, modifier = Modifier.weight(1f))
                TextButton(onClick = onReview) {
                    Text("Review", color = ChessGold, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Close, "Dismiss", tint = appColors.iconSubtle, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
internal fun MentorContextBanner(label: String) {
    val appColors = LocalAppColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = ChessGold.copy(alpha = 0.10f)),
        shape    = RoundedCornerShape(8.dp),
        border   = BorderStroke(1.dp, BannerBorder.copy(alpha = 0.5f)),
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, null, tint = ChessGold, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = appColors.textPrimary)
        }
    }
}
