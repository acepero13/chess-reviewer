package com.acepero13.android.gamereviewer.ui.screens.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.EmojiObjects
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.CoachingTrigger
import com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState
import com.acepero13.android.gamereviewer.ui.screens.AnalysisViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun NavigateBottomBar(
    state:      AnalysisUiState,
    vm:         AnalysisViewModel,
    snackbar:   SnackbarHostState,
    scope:      CoroutineScope,
    onBookmark: () -> Unit = {},
) {
    val analysisReady = state.isBackgroundAnalysisDone
    val onNotReady: () -> Unit = {
        scope.launch { snackbar.showSnackbar("Analysis is still running — results will be ready shortly") }
    }

    val currentTrigger     = state.triggersByMove[state.moveIndex]?.firstOrNull()
    val currentTriggerType = currentTrigger?.typeName()
    val recentTriggerTypes = (1..3).mapNotNull { step ->
        state.triggersByMove[state.moveIndex - step * 2]?.firstOrNull()?.typeName()
    }.toSet()
    val hasActiveTrigger = currentTrigger != null &&
        (currentTrigger.tier() == 1 ||
         currentTrigger is CoachingTrigger.EvalCalibration ||
         currentTriggerType !in recentTriggerTypes)

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        BottomBarButton(icon = Icons.Outlined.Flag,         label = "Critical", onClick = vm::markCurrentAsCritical, enabled = state.moveIndex > 0)
        BottomBarButton(icon = Icons.Outlined.Search,       label = "Analyse",  onClick = vm::enterAnalyseMode)
        BottomBarButton(icon = Icons.Outlined.Bookmark,     label = "Bookmark", onClick = { if (state.moveIndex >= 0) onBookmark() }, enabled = state.moveIndex >= 0)
        BottomBarButton(icon = Icons.Outlined.BarChart,     label = "Stats",    onClick = vm::toggleStatsSheet,       enabled = analysisReady, onDisabledClick = if (!analysisReady) onNotReady else null)
        BottomBarButton(icon = Icons.Outlined.EmojiObjects, label = "Mentor",   onClick = vm::enterMentorSession,     enabled = analysisReady && state.criticalMoments.any { it.type == "ENGINE_MARKED" }, onDisabledClick = if (!analysisReady) onNotReady else null)
    }
}
