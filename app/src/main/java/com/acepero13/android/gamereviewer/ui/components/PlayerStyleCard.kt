package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.PlayerProfile
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors

/** Shows the derived player-style archetype + a short description. */
@Composable
fun PlayerStyleCard(
    profile: PlayerProfile,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    Card(
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = appColors.surface),
        border   = BorderStroke(1.dp, ChessGold.copy(alpha = 0.4f)),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text  = "Your Playing Style",
                style = MaterialTheme.typography.labelMedium,
                color = appColors.textSecondary,
            )
            Text(
                text       = profile.archetype,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = ChessGold,
            )
            Text(
                text     = profile.archetypeDescription,
                style    = MaterialTheme.typography.bodySmall,
                color    = appColors.textSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text     = "Based on ${profile.gamesAnalyzed} analyzed game${if (profile.gamesAnalyzed != 1) "s" else ""}",
                style    = MaterialTheme.typography.labelSmall,
                color    = appColors.textTertiary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
