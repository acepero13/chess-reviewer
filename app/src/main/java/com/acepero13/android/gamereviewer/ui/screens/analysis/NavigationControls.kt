package com.acepero13.android.gamereviewer.ui.screens.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState
import com.acepero13.android.gamereviewer.ui.screens.AnalysisViewModel
import com.acepero13.android.gamereviewer.ui.screens.ReviewMode
import com.acepero13.chess.core.ui.theme.ChessGold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun NavigationControls(
    state:    AnalysisUiState,
    vm:       AnalysisViewModel,
    snackbar: SnackbarHostState,
    scope:    CoroutineScope,
) {
    val enabled   = state.reviewMode != ReviewMode.MENTOR
    val frozenMsg = "Navigation is frozen in Mentor mode"
    fun navClick(action: () -> Unit): () -> Unit =
        if (enabled) action else ({ scope.launch { snackbar.showSnackbar(frozenMsg) } })
    val tint = if (enabled) ChessGold else ChessGold.copy(alpha = 0.38f)

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        IconButton(onClick = navClick(vm::goToStart))    { Icon(Icons.Outlined.FastRewind,   "Start",    tint = tint) }
        IconButton(onClick = navClick(vm::stepBackward)) { Icon(Icons.Outlined.SkipPrevious, "Previous", tint = tint) }
        Text(
            "${state.moveIndex} / ${state.totalMoves}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
        IconButton(onClick = navClick(vm::stepForward)) { Icon(Icons.Outlined.SkipNext,    "Next", tint = tint) }
        IconButton(onClick = navClick(vm::goToEnd))     { Icon(Icons.Outlined.FastForward, "End",  tint = tint) }
    }
}
