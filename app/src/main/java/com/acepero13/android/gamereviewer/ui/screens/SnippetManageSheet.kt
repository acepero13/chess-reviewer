package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.data.model.Snippet
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetManageSheet(
    snippet: Snippet,
    onDismiss: () -> Unit,
    onSave: (title: String, tags: String, notes: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val appColors  = LocalAppColors.current

    var title by remember { mutableStateOf(snippet.title) }
    var tags  by remember { mutableStateOf(snippet.tags) }
    var notes by remember { mutableStateOf(snippet.notes) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = ChessGold,
        unfocusedBorderColor = appColors.border,
        focusedLabelColor    = ChessGold,
        unfocusedLabelColor  = appColors.textSecondary,
        cursorColor          = ChessGold,
        focusedTextColor     = appColors.textPrimary,
        unfocusedTextColor   = appColors.textPrimary,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = appColors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text       = "Manage Snippet",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = ChessGold,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text("Title") },
                singleLine    = true,
                colors        = fieldColors,
                modifier      = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value         = tags,
                onValueChange = { tags = it },
                label         = { Text("Tags") },
                placeholder   = { Text("tactic, endgame, fork", color = appColors.textTertiary) },
                singleLine    = true,
                colors        = fieldColors,
                modifier      = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value         = notes,
                onValueChange = { notes = it },
                label         = { Text("Notes") },
                minLines      = 3,
                maxLines      = 5,
                colors        = fieldColors,
                modifier      = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = appColors.textSecondary)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { onSave(title, tags, notes); onDismiss() },
                    enabled = title.isNotBlank(),
                    colors  = ButtonDefaults.buttonColors(containerColor = ChessGold),
                ) {
                    Text("Save", color = androidx.compose.ui.graphics.Color.Black)
                }
            }
        }
    }
}
