package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState
import com.acepero13.android.gamereviewer.ui.screens.AnalysisViewModel

@Composable
internal fun ExploreSubMode(state: AnalysisUiState, vm: AnalysisViewModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AnimatedVisibility(state.blunderReflectionMode && state.blunderReflectionInsight != null,
            enter = fadeIn(tween(250)) + expandVertically(), exit = fadeOut(tween(150)) + shrinkVertically()) {
            if (state.blunderReflectionInsight != null) {
                BlunderReflectionPanel(
                    insight    = state.blunderReflectionInsight,
                    cpLoss     = state.blunderCpLoss,
                    onRetry    = vm::retryAfterBlunder,
                    onContinue = vm::continueAfterBlunder,
                    modifier   = Modifier.fillMaxWidth(),
                )
            }
        }
        if (state.sandboxEngineThinking) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)))
        }
        FilledTonalButton(onClick = vm::exitSandboxMode, modifier = Modifier.fillMaxWidth()) {
            Text("Exit Explore Mode")
        }
    }
}
