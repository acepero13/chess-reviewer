package com.acepero13.android.gamereviewer.ui.screens.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.screens.AnalyseSubMode
import com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState
import com.acepero13.android.gamereviewer.ui.screens.AnalysisViewModel
import com.acepero13.android.gamereviewer.ui.screens.ReviewMode

@Composable
internal fun AnalyseBottomBar(state: AnalysisUiState, vm: AnalysisViewModel) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        BottomBarButton(
            icon    = Icons.Outlined.ArrowBackIosNew,
            label   = "Navigate",
            onClick = { vm.setReviewMode(ReviewMode.NAVIGATE) },
        )
        BottomBarButton(
            icon     = Icons.Outlined.Edit,
            label    = "Edit",
            selected = state.analyseSubMode == AnalyseSubMode.EDIT,
            onClick  = {
                if (state.analyseSubMode == AnalyseSubMode.EDIT)
                    vm.setAnalyseSubMode(AnalyseSubMode.VIEW)
                else
                    vm.setAnalyseSubMode(AnalyseSubMode.EDIT)
            },
        )
        BottomBarButton(
            icon     = Icons.Outlined.PlayArrow,
            label    = "Explore",
            selected = state.analyseSubMode == AnalyseSubMode.EXPLORE,
            onClick  = {
                if (state.analyseSubMode == AnalyseSubMode.EXPLORE)
                    vm.exitSandboxMode()
                else
                    vm.enterSandboxMode()
            },
        )
    }
}
