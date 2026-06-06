package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.chess.core.ui.theme.ChessGold

@Composable
private fun ThoughtsDialog(
    draft: String, onDraftChange: (String) -> Unit, onDismiss: () -> Unit, onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Analysis notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
        text  = {
            OutlinedTextField(value = draft, onValueChange = onDraftChange,
                placeholder = { Text("Write what you see in this position…", style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 8)
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save", color = ChessGold, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
    )
}

@Composable
internal fun ThoughtsInput(
    thoughts: String, onThoughtsChange: (String) -> Unit,
    visible: Boolean, modifier: Modifier = Modifier,
) {
    var showDialog    by remember { mutableStateOf(false) }
    var draftThoughts by remember { mutableStateOf("") }
    if (showDialog) {
        ThoughtsDialog(
            draft         = draftThoughts,
            onDraftChange = { draftThoughts = it },
            onDismiss     = { showDialog = false },
            onSave        = { onThoughtsChange(draftThoughts); showDialog = false },
        )
    }
    AnimatedVisibility(visible, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Row(
            modifier = modifier.fillMaxWidth()
                .clickable { draftThoughts = thoughts; showDialog = true }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.ExpandMore, null,
                tint = if (thoughts.isNotBlank()) ChessGold else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text  = if (thoughts.isNotBlank()) "Edit analysis notes" else "Add analysis notes (optional)",
                style = MaterialTheme.typography.labelSmall,
                color = if (thoughts.isNotBlank()) ChessGold else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
