package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.chess.core.ui.theme.ChessGold
import com.acepero13.chess.core.ui.theme.LocalAppColors

/**
 * Header card for the Insights tab: shows how many games still need a stats pass and lets the
 * user kick off the [com.acepero13.android.gamereviewer.work.ShallowAnalysisWorker], or shows
 * live progress while it runs.
 */
@Composable
fun AnalysisProgressCard(
    gamesAnalyzed: Int,
    gamesPending: Int,
    inProgress: Boolean,
    progressDone: Int,
    progressTotal: Int,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current
    Card(
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = appColors.surface),
        border   = BorderStroke(1.dp, appColors.border),
        modifier = modifier,
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text       = "$gamesAnalyzed analyzed · $gamesPending pending",
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = appColors.textPrimary,
            )

            if (inProgress) {
                val frac = if (progressTotal > 0) progressDone.toFloat() / progressTotal else 0f
                Text(
                    text  = "Analyzing $progressDone / $progressTotal games…",
                    style = MaterialTheme.typography.bodySmall,
                    color = appColors.textSecondary,
                )
                LinearProgressIndicator(
                    progress   = { frac },
                    modifier   = Modifier.fillMaxWidth().height(8.dp),
                    color      = ChessGold,
                    trackColor = ChessGold.copy(alpha = 0.2f),
                    strokeCap  = StrokeCap.Round,
                )
            } else if (gamesPending > 0) {
                Text(
                    text  = "Run a fast engine pass to compute accuracy and your style profile. " +
                        "Already-reviewed games are reused instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = appColors.textSecondary,
                )
                Button(
                    onClick = onAnalyze,
                    colors  = ButtonDefaults.buttonColors(containerColor = ChessGold),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Analyze $gamesPending game${if (gamesPending != 1) "s" else ""}")
                }
            } else {
                Text(
                    text  = "All imported games are analyzed. ✅",
                    style = MaterialTheme.typography.bodySmall,
                    color = appColors.textSecondary,
                )
            }
        }
    }
}
