package com.acepero13.android.gamereviewer.ui.screens.analysis

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState
import com.acepero13.android.gamereviewer.ui.screens.AnalysisViewModel
import com.acepero13.android.gamereviewer.ui.screens.ReviewMode
import com.acepero13.chess.core.ui.theme.LocalAppColors
import kotlinx.coroutines.CoroutineScope
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.Modifier

@Composable
internal fun AnalysisBottomBar(
    state:      AnalysisUiState,
    vm:         AnalysisViewModel,
    snackbar:   SnackbarHostState,
    scope:      CoroutineScope,
    onBookmark: () -> Unit = {},
) {
    val appColors = LocalAppColors.current
    Surface(color = appColors.surface, tonalElevation = 0.dp) {
        Column(modifier = Modifier.animateContentSize(tween(200))) {
            HorizontalDivider(color = appColors.border, thickness = 0.5.dp)
            AnimatedContent(
                targetState    = state.reviewMode,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label          = "bottomBar",
            ) { mode ->
                when (mode) {
                    ReviewMode.NAVIGATE -> NavigateBottomBar(state, vm, snackbar, scope, onBookmark)
                    ReviewMode.ANALYSE  -> AnalyseBottomBar(state, vm)
                    ReviewMode.MENTOR   -> MentorBottomBar(state, vm)
                }
            }
        }
    }
}
