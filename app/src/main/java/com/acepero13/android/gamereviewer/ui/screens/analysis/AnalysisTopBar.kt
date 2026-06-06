package com.acepero13.android.gamereviewer.ui.screens.analysis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState
import com.acepero13.android.gamereviewer.ui.screens.AnalysisViewModel
import com.acepero13.android.gamereviewer.ui.screens.ReviewMode
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AnalysisTopBar(
    state:        AnalysisUiState,
    vm:           AnalysisViewModel,
    gameId:       Long,
    onBack:       () -> Unit,
    onViewReport: (Long) -> Unit,
) {
    val appColors = LocalAppColors.current
    TopAppBar(
        title = {
            Column {
                state.game?.let { g ->
                    Text("${g.whitePlayer} vs ${g.blackPlayer}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ChessGold)
                }
                if (state.openingSummary.isNotEmpty()) {
                    val deviation = state.openingDeviation
                    val summary   = if (deviation != null) "${state.openingSummary}  ·  left book: ${deviation.moveLabel}" else state.openingSummary
                    Text(
                        text     = summary,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = if (deviation != null) ChessGold.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = if (deviation != null) Modifier.clickable { vm.goToMove(deviation.moveIndex) } else Modifier,
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBackIosNew, "Back", tint = ChessGold) }
        },
        colors  = TopAppBarDefaults.topAppBarColors(containerColor = appColors.background),
        actions = {
            if (state.reviewMode == ReviewMode.MENTOR) {
                Icon(Icons.Outlined.Lock, contentDescription = "Navigation frozen", tint = ChessGold, modifier = Modifier.padding(end = 16.dp).size(20.dp))
            }
            if (state.isBackgroundAnalysisDone) {
                IconButton(onClick = { onViewReport(gameId) }) {
                    Icon(Icons.Outlined.Assessment, contentDescription = "View full report", tint = ChessGold)
                }
            }
            if (!state.isBackgroundAnalysisDone && state.backgroundAnalysisProgress > 0f) {
                CircularProgressIndicator(
                    progress    = { state.backgroundAnalysisProgress },
                    modifier    = Modifier.padding(end = 12.dp).size(18.dp),
                    strokeWidth = 2.dp,
                    color       = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
