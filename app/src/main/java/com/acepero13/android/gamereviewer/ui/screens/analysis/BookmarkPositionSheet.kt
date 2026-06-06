package com.acepero13.android.gamereviewer.ui.screens.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkPositionSheet(
    sheetState: SheetState,
    onSave:     (title: String, tags: String, notes: String) -> Unit,
    onDismiss:  () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var tags  by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val appColors = LocalAppColors.current

    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        sheetState        = sheetState,
        containerColor    = appColors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Outlined.Bookmark,
                    contentDescription = null,
                    tint               = ChessGold,
                    modifier           = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = "Bookmark Position",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = appColors.textPrimary,
                )
            }
            Spacer(Modifier.height(16.dp))
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = ChessGold,
                unfocusedBorderColor = appColors.border,
                focusedTextColor     = appColors.textPrimary,
                unfocusedTextColor   = appColors.textPrimary,
                cursorColor          = ChessGold,
            )
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text("Title", color = appColors.textSecondary) },
                placeholder   = { Text("e.g. Missed fork in the endgame", color = appColors.textTertiary) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                colors        = fieldColors,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value         = tags,
                onValueChange = { tags = it },
                label         = { Text("Tags (comma-separated)", color = appColors.textSecondary) },
                placeholder   = { Text("e.g. endgame, tactics", color = appColors.textTertiary) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                colors        = fieldColors,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value         = notes,
                onValueChange = { notes = it },
                label         = { Text("Notes", color = appColors.textSecondary) },
                placeholder   = { Text("What were you thinking here?", color = appColors.textTertiary) },
                minLines      = 2,
                maxLines      = 4,
                modifier      = Modifier.fillMaxWidth(),
                colors        = fieldColors,
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    onClick  = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel", color = appColors.textSecondary)
                }
                Button(
                    onClick  = { onSave(title, tags, notes) },
                    colors   = ButtonDefaults.buttonColors(containerColor = ChessGold),
                    modifier = Modifier.weight(2f),
                ) {
                    Text("Save Bookmark", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

