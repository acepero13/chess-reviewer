package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.screens.ClassificationOption
import com.acepero13.chess.core.ui.theme.ChessGold

internal val CorrectGreen   = Color(0xFF2E7D32)
internal val CorrectOnGreen = Color(0xFFC8E6C9)
internal val CloseAmber     = Color(0xFF1E1600)
internal val CloseOnAmber   = Color(0xFFE8C882)
internal val WrongRed       = Color(0xFF4E0808)
internal val WrongOnRed     = Color(0xFFFFCDD2)

@Composable
private fun ClassificationOptionRow(
    option: ClassificationOption, idx: Int, correctIndex: Int,
    selectedIndex: Int, answered: Boolean, onSelect: () -> Unit,
) {
    val isCorrect  = idx == correctIndex
    val isSelected = idx == selectedIndex
    val bg = when { !answered -> MaterialTheme.colorScheme.surface; isCorrect -> CorrectGreen; isSelected -> WrongRed; else -> MaterialTheme.colorScheme.surface }
    val bd = when { !answered -> MaterialTheme.colorScheme.outline; isCorrect -> CorrectGreen; isSelected -> WrongRed; else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) }
    val fg = when { !answered -> MaterialTheme.colorScheme.onSurface; isCorrect -> CorrectOnGreen; isSelected -> WrongOnRed; else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(bg)
            .border(1.dp, bd, RoundedCornerShape(8.dp))
            .then(if (!answered) Modifier.clickable(onClick = onSelect) else Modifier).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(option.label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = fg)
            if (option.description.isNotBlank())
                Text(option.description, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.8f))
        }
        if (answered && isCorrect) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Outlined.CheckCircle, "Correct", tint = CorrectOnGreen, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun ClassificationQuizSection(
    options: List<ClassificationOption>, correctIndex: Int, selectedIndex: Int,
    onSelect: (Int) -> Unit, modifier: Modifier = Modifier,
) {
    val answered = selectedIndex >= 0
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Why was this moment critical?", style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold, color = ChessGold)
        options.forEachIndexed { idx, option ->
            ClassificationOptionRow(option, idx, correctIndex, selectedIndex, answered, onSelect = { onSelect(idx) })
        }
    }
}
