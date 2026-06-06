package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.acepero13.chess.core.ui.theme.ChessGold

val ANNOTATION_COLORS: List<Color> = listOf(Color(0xCCF0A500.toInt()), Color(0xCC48BB78.toInt()), Color(0xCCF56565.toInt()), Color(0xCCFF9800.toInt()), Color(0xCC64B5F6.toInt()))
val ANNOTATION_COLOR_LABELS: List<String> = listOf("Gold", "Green", "Red", "Orange", "Blue")

@Composable
internal fun ColorPaletteRow(
    colors: List<Color>, labels: List<String> = ANNOTATION_COLOR_LABELS,
    selectedColor: Color, onColorSelected: (Color) -> Unit, modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        colors.forEachIndexed { index, color ->
            val isSelected = color == selectedColor
            val label      = labels.getOrElse(index) { "" }
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onColorSelected(color) }) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(color)
                        .then(if (isSelected) Modifier.border(2.dp, ChessGold, CircleShape)
                              else Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape))
                )
                if (label.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
                }
            }
        }
    }
}
