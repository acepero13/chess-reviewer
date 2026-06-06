package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.acepero13.chess.core.ui.theme.ChessGold

@Composable
internal fun CommentChip(comment: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val hasComment = comment.isNotBlank()
    Row(
        modifier = modifier.clip(RoundedCornerShape(8.dp))
            .border(1.dp, if (hasComment) ChessGold.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.Comment, "Add comment",
            tint = if (hasComment) ChessGold else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp))
        Text(
            text      = if (hasComment) comment else "Add a comment…",
            style     = MaterialTheme.typography.bodySmall,
            color     = if (hasComment) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = if (hasComment) FontStyle.Normal else FontStyle.Italic,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            modifier  = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun CommentDialog(initial: String, moveLabel: String = "", onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Move comment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (moveLabel.isNotBlank())
                    Text(moveLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            OutlinedTextField(text, { text = it }, modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 8,
                placeholder = { Text("What were you thinking here?", style = MaterialTheme.typography.bodySmall) })
        },
        confirmButton = { TextButton({ onSave(text) }) { Text("Save", color = ChessGold, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
    )
}
