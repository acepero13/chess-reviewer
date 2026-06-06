package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiObjects
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.screens.MentorMoveResult
import com.acepero13.chess.core.ui.theme.ChessGold

@Composable
private fun GotItButton(onSubmit: () -> Unit) {
    Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = ChessGold)) {
        Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("Got it — continue review", color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
private fun ThinkingIndicator(mentorMoveChecking: Boolean) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(
            text  = if (mentorMoveChecking) "Checking your move…" else "Loading engine answer…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ShowAnswerButtons(
    hintVisible: Boolean, onRevealHint: () -> Unit, onRevealAnswer: () -> Unit, onExit: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!hintVisible) {
            OutlinedButton(onClick = onRevealHint, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.EmojiObjects, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Get a Hint")
            }
        }
        Button(onClick = onRevealAnswer, modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = ChessGold)) {
            Icon(Icons.Outlined.Visibility, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Show Answer", color = MaterialTheme.colorScheme.onPrimary)
        }
    }
    OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
        Text("Exit Guided Review")
    }
}

@Composable
internal fun DiscoveryActionButtons(
    answerRevealed: Boolean, mentorMoveResult: MentorMoveResult?,
    engineThinking: Boolean, mentorMoveChecking: Boolean, hintVisible: Boolean,
    onSubmit: () -> Unit, onRevealHint: () -> Unit, onRevealAnswer: () -> Unit, onExit: () -> Unit,
) {
    when {
        answerRevealed || mentorMoveResult == MentorMoveResult.CORRECT ||
            mentorMoveResult == MentorMoveResult.CLOSE -> GotItButton(onSubmit)
        engineThinking || mentorMoveChecking -> ThinkingIndicator(mentorMoveChecking)
        else -> ShowAnswerButtons(hintVisible, onRevealHint, onRevealAnswer, onExit)
    }
}
