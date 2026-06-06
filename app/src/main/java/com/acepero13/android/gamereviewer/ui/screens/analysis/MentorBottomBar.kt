package com.acepero13.android.gamereviewer.ui.screens.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState
import com.acepero13.android.gamereviewer.ui.screens.AnalysisViewModel
import com.acepero13.android.gamereviewer.ui.screens.MentorMoveResult
import com.acepero13.chess.core.ui.theme.ChessGold

@Composable
internal fun MentorBottomBar(state: AnalysisUiState, vm: AnalysisViewModel) {
    val sessionActive = state.mentorSessionQueue.isNotEmpty()
    val canAdvance    = sessionActive &&
        state.mentorSessionIdx + 1 < state.mentorSessionQueue.size &&
        (state.guidedDiscoveryAnswerRevealed ||
         state.mentorMoveResult == MentorMoveResult.CORRECT ||
         state.mentorMoveResult == MentorMoveResult.CLOSE)

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = vm::exitMentorMode, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
            Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = ChessGold)
            Spacer(Modifier.width(6.dp))
            Text(
                text       = if (sessionActive) "Exit review" else "Return",
                color      = ChessGold,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (canAdvance) {
            Button(
                onClick  = vm::advanceMentorSession,
                modifier = Modifier.weight(1f),
                shape    = RoundedCornerShape(8.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = ChessGold),
            ) {
                Text("Next mistake", color = Color.Black, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
            }
        }
    }
}
