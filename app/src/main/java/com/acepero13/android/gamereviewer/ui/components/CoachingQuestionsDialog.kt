package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiObjects
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acepero13.android.gamereviewer.domain.InsightReconciler
import com.acepero13.chess.core.ui.theme.ChessGold

private val DialogBorder = Color(0xFF2D6A4F)

@Composable
internal fun CoachingQuestionsDialog(
    insight:          InsightReconciler.Insight,
    hasAnswer:        Boolean,
    isCoordination:   Boolean,
    isForcing:        Boolean,
    savedReflection:  String,
    onEditReflection: () -> Unit,
    onDismiss:        () -> Unit,
    onConfirm:        () -> Unit,
) {
    AlertDialog(
        onDismissRequest  = onDismiss,
        containerColor    = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor  = MaterialTheme.colorScheme.onSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = insight.emoji, fontSize = 20.sp)
                Text(text = insight.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = { CoachingDialogContent(insight, hasAnswer, isCoordination, isForcing, savedReflection, onEditReflection) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape   = RoundedCornerShape(8.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = DialogBorder),
            ) { Text("Got it — continue", color = Color.White, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Back", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        },
    )
}

@Composable
private fun CoachingDialogContent(
    insight:          InsightReconciler.Insight,
    hasAnswer:        Boolean,
    isCoordination:   Boolean,
    isForcing:        Boolean,
    savedReflection:  String,
    onEditReflection: () -> Unit,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = insight.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
        HorizontalDivider(color = DialogBorder.copy(alpha = 0.4f))
        Text("Think about these:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.SemiBold)
        insight.questions.forEachIndexed { i, question ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${i + 1}.", style = MaterialTheme.typography.bodySmall, color = ChessGold, fontWeight = FontWeight.Bold)
                Text(question, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        HorizontalDivider(color = DialogBorder.copy(alpha = 0.25f))
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.EmojiObjects, null, modifier = Modifier.size(14.dp).padding(top = 2.dp), tint = ChessGold.copy(alpha = 0.7f))
            Text(text = insight.conceptualHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), fontStyle = FontStyle.Italic)
        }
        if (!hasAnswer && !isCoordination && !isForcing) {
            ReflectionSection(savedReflection, onEditReflection)
        }
    }
}

@Composable
private fun ReflectionSection(savedReflection: String, onEdit: () -> Unit) {
    HorizontalDivider(color = DialogBorder.copy(alpha = 0.25f))
    if (savedReflection.isNotEmpty()) {
        Card(
            shape  = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = ChessGold.copy(alpha = 0.10f)),
            border = BorderStroke(1.dp, DialogBorder.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("Your reflection", style = MaterialTheme.typography.labelSmall, color = ChessGold.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(savedReflection, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        OutlinedButton(
            onClick  = onEdit,
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(8.dp),
            border   = BorderStroke(1.dp, DialogBorder.copy(alpha = 0.5f)),
        ) { Text("Edit reflection", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall) }
    } else {
        OutlinedButton(
            onClick  = onEdit,
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(8.dp),
            border   = BorderStroke(1.dp, ChessGold.copy(alpha = 0.5f)),
        ) { Text("Write your reflection…", color = ChessGold.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold) }
    }
}
