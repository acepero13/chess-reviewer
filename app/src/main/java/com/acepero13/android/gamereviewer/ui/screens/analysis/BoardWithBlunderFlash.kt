package com.acepero13.android.gamereviewer.ui.screens.analysis

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.screens.AnalyseSubMode
import com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState
import com.acepero13.android.gamereviewer.ui.screens.ReviewMode
import com.acepero13.chess.core.ui.board.ChessBoard
import com.github.bhlangonijr.chesslib.Square

private const val TAG = "MentorTap"

@Composable
internal fun BoardWithBlunderFlash(
    state:          AnalysisUiState,
    onArrow:        (Square, Square) -> Unit,
    onMark:         (Square) -> Unit,
    onTap:          (Square) -> Unit,
    onMentorTap:    (Square) -> Unit,
    onProactiveTap: (Square) -> Unit,
    modifier:       Modifier = Modifier.fillMaxWidth(),
) {
    val inEditMode = state.reviewMode == ReviewMode.ANALYSE && state.analyseSubMode == AnalyseSubMode.EDIT
    LaunchedEffect(state.reviewMode, state.mentorMoveInputActive) {
        Log.d(TAG, "BoardWithBlunderFlash recomposed: reviewMode=${state.reviewMode}")
    }
    val latestState          by rememberUpdatedState(state)
    val latestOnTap          by rememberUpdatedState(onTap)
    val latestOnMentorTap    by rememberUpdatedState(onMentorTap)
    val latestOnProactiveTap by rememberUpdatedState(onProactiveTap)
    val stableSquareTap = remember {
        { sq: Square ->
            val s         = latestState
            val isMentor  = s.reviewMode == ReviewMode.MENTOR
            val isExplore = s.reviewMode == ReviewMode.ANALYSE && s.analyseSubMode == AnalyseSubMode.EXPLORE
            Log.d(TAG, "onSquareTap: sq=$sq inMentor=$isMentor inExplore=$isExplore proactive=${s.proactiveInteractiveMode}")
            when {
                s.proactiveInteractiveMode -> latestOnProactiveTap(sq)
                isExplore                  -> latestOnTap(sq)
                isMentor                   -> latestOnMentorTap(sq)
                else                       -> Unit
            }
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "blunderFlash")
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue  = if (state.blunderGuardActive) 0.2f else 0f,
        targetValue   = if (state.blunderGuardActive) 0.9f else 0f,
        animationSpec = infiniteRepeatable(animation = tween(350, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "blunderAlpha",
    )
    Box(modifier = modifier.clip(RoundedCornerShape(4.dp)).border(
        width = if (state.blunderGuardActive) 4.dp else 0.dp,
        color = MaterialTheme.colorScheme.error.copy(alpha = flashAlpha),
        shape = RoundedCornerShape(4.dp),
    )) {
        ChessBoard(
            boardState     = state.boardState.copy(markedSquares = state.boardState.markedSquares + state.coachHighlightSquares),
            onSquareTap    = stableSquareTap,
            onArrowDrawn   = if (inEditMode) onArrow else null,
            onSquareMarked = if (inEditMode) onMark  else null,
            modifier       = Modifier.fillMaxWidth(),
        )
    }
}
