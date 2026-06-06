package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.chess.core.ui.theme.ChessGold

private val TextFieldBorder = Color(0xFF2D6A4F)

@Composable
internal fun ReflectionEntryDialog(
    draftText:    String,
    onTextChange: (String) -> Unit,
    onSave:       (String) -> Unit,
    onDismiss:    () -> Unit,
) {
    AlertDialog(
        onDismissRequest  = onDismiss,
        containerColor    = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor  = MaterialTheme.colorScheme.onSurface,
        title = {
            Text("Your Reflection", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            OutlinedTextField(
                value         = draftText,
                onValueChange = onTextChange,
                placeholder   = {
                    Text(
                        text  = "What are you thinking? Write your plan, candidate moves, or observations…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                },
                modifier  = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                minLines  = 4,
                maxLines  = 10,
                colors    = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = ChessGold,
                    unfocusedBorderColor = TextFieldBorder,
                    focusedTextColor     = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor   = MaterialTheme.colorScheme.onSurface,
                    cursorColor          = ChessGold,
                ),
                shape = RoundedCornerShape(8.dp),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(draftText.trim()) }) {
                Text("Save", color = ChessGold, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        },
    )
}
