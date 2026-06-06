package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class BackgroundAnalysisController(
    private val session: GameSession,
    private val restorer: TruthMapRestorer,
    private val freshAnalysis: FreshAnalysisController,
) {

    fun launch(stored: List<CriticalMoment>) {
        if (stored.any { it.type == CriticalMoment.Type.ENGINE_MARKED.name }) {
            session.scope.launch(Dispatchers.IO) { restorer.restore() }
        } else {
            session.scope.launch(Dispatchers.Default) { freshAnalysis.run() }
        }
    }
}
