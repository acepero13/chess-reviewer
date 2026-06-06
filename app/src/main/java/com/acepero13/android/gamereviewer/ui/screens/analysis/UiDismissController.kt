package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.data.db.GameEvaluationDao
import com.acepero13.android.gamereviewer.data.db.MoveTimeDao
import com.acepero13.android.gamereviewer.domain.PlayerStatsCalculator
import com.acepero13.android.gamereviewer.ui.screens.GamePrediction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class UiDismissController(
    private val session: GameSession,
    private val gameEvaluationDao: GameEvaluationDao,
    private val moveTimeDao: MoveTimeDao,
) {

    fun toggleStatsSheet() {
        val nowOpen = !session.uiState.value.showStatsSheet
        session.uiState.update { it.copy(showStatsSheet = nowOpen) }
        if (nowOpen && session.uiState.value.playerStats == null) {
            session.scope.launch(Dispatchers.IO) {
                val game  = session.uiState.value.game ?: return@launch
                val evals = gameEvaluationDao.getByGameId(session.gameId)
                val times = moveTimeDao.getByGameId(session.gameId)
                val stats = PlayerStatsCalculator.compute(game, evals, times)
                session.uiState.update { it.copy(playerStats = stats) }
            }
        }
    }

    fun dismissStatsSheet() = session.uiState.update { it.copy(showStatsSheet = false) }
    fun dismissMissedMomentBanner() = session.uiState.update { it.copy(showMissedMomentBanner = false, missedMomentMoveIndex = null) }
    fun dismissOpeningDeviationPanel() = session.uiState.update { it.copy(showOpeningDeviationPanel = false, openingDeviationDismissed = true) }
    fun dismissEndgameRecognitionPanel() = session.uiState.update { it.copy(showEndgameRecognitionPanel = false, endgamePanelDismissed = true) }
    fun dismissMiddlegamePlanPanel() = session.uiState.update { it.copy(showMiddlegamePlanPanel = false, middlegamePlanPanelDismissed = true) }
    fun submitPrediction(prediction: GamePrediction) = session.uiState.update { it.copy(showPredictionGate = false, gamePrediction = prediction) }
    fun skipPrediction() = session.uiState.update { it.copy(showPredictionGate = false) }
    fun dismissGameStory() = session.uiState.update { it.copy(gameStoryDismissed = true) }
    fun dismissPostGameDebrief() = session.uiState.update { it.copy(showPostGameDebrief = false) }
    fun reviewMissedMoment() = session.uiState.update { it.copy(showMissedMomentBanner = false) }
}
