package com.acepero13.android.gamereviewer.ui.screens.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.acepero13.chess.core.ui.theme.ChessGold

@Composable
internal fun MentorSessionProgressHeader(
    current:              Int,
    total:                Int,
    modifier:             Modifier = Modifier,
    weaknessDotPositions: Set<Int> = emptySet(),
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 1..total) {
                val done      = i < current
                val active    = i == current
                val isWeak    = i in weaknessDotPositions
                Text(
                    text  = when { done -> "●"; active -> "◉"; else -> "○" },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isWeak && (done || active) -> Color(0xFFFF8F00)
                        done || active             -> ChessGold
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        Text("Mistake $current of $total", style = MaterialTheme.typography.labelSmall, color = ChessGold)
    }
}
