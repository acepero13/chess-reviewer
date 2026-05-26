package com.acepero13.android.gamereviewer.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiObjects
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.chess.core.ui.theme.ChessGold

/**
 * Modal bottom-sheet questionnaire that appears when the user marks a position as critical.
 *
 * Captures qualitative data about the user's thinking process before any engine feedback
 * is revealed — the core of the "Human-First, Engine-Second" philosophy.
 *
 * @param onSubmit   Called with (plan, threats, candidates) when the user confirms.
 * @param onDismiss  Called when the user cancels without submitting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CriticalMomentSheet(
    sheetState: SheetState,
    onSubmit: (plan: String, threats: String, candidates: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var plan       by remember { mutableStateOf("") }
    var threats    by remember { mutableStateOf("") }
    var candidates by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Header ─────────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.EmojiObjects,
                    contentDescription = null,
                    tint = ChessGold,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Critical Position — Your Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ChessGold,
                )
            }

            Text(
                text = "Before checking the engine, articulate your thinking. " +
                        "This is where improvement happens.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ── Q1: Plan ───────────────────────────────────────────────────────
            QuestionField(
                label    = "What is your plan here?",
                hint     = "e.g. Push the passed pawn, activate the rook on d-file…",
                value    = plan,
                onValueChange = { plan = it },
            )

            // ── Q2: Threats ────────────────────────────────────────────────────
            QuestionField(
                label    = "What threats do you see?",
                hint     = "e.g. Opponent's knight can jump to f5, back-rank mate threat…",
                value    = threats,
                onValueChange = { threats = it },
            )

            // ── Q3: Candidates ─────────────────────────────────────────────────
            QuestionField(
                label    = "What candidate moves did you consider?",
                hint     = "e.g. 1. Rxd7, 2. Nd5, 3. h4 — and why you chose one",
                value    = candidates,
                onValueChange = { candidates = it },
            )

            Spacer(Modifier.height(8.dp))

            // ── Actions ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick  = { onSubmit(plan, threats, candidates) },
                    enabled  = plan.isNotBlank() || threats.isNotBlank() || candidates.isNotBlank(),
                    colors   = ButtonDefaults.buttonColors(containerColor = ChessGold),
                ) {
                    Text("Save Analysis", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun QuestionField(
    label: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text(hint, style = MaterialTheme.typography.bodySmall) },
            modifier      = Modifier.fillMaxWidth(),
            minLines      = 2,
            maxLines      = 4,
        )
    }
}
