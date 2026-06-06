package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.data.db.CriticalMomentDao
import com.acepero13.android.gamereviewer.data.db.GameEvaluationDao
import com.acepero13.android.gamereviewer.data.db.MoveTimeDao
import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.data.model.MoveTimeData
import com.acepero13.android.gamereviewer.domain.TimeAnalyzer
import com.acepero13.android.gamereviewer.domain.TruthMapEntry
import com.acepero13.android.gamereviewer.engine.highlights.GameHighlightEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

internal class TruthMapRestorer(
    private val session: GameSession,
    private val gameEvaluationDao: GameEvaluationDao,
    private val criticalMomentDao: CriticalMomentDao,
    private val moveTimeDao: MoveTimeDao,
    private val triggerProcessor: CoachingTriggerProcessor,
    private val debriefBuilder: GameDebriefBuilder,
    private val navigation: NavigationController,
) {

    suspend fun restore() {
        val dbEvals   = gameEvaluationDao.getByGameId(session.gameId)
        restoreTruthMap(dbEvals)
        val moveTimes = withContext(Dispatchers.IO) {
            moveTimeDao.getByGameId(session.gameId).associateBy { it.moveIndex }
        }
        val allMoments  = withContext(Dispatchers.IO) { criticalMomentDao.getAll() }
        val weakTypes   = WeaknessAnalyzer.buildWeakTriggerTypes(allMoments)
        val triggers    = triggerProcessor.evaluate(dbEvals, weakTypes)
        val highlights  = GameHighlightEngine.run(session.truthMap, session.sanMoves, session.fenSequence, moveTimes)
        val overthought = computeOverthought(dbEvals, moveTimes)
        updateState(triggers, weakTypes, highlights, overthought)
        val current = session.uiState.value.moveIndex
        if (current > 0) navigation.checkMissedMoments(0, current)
    }

    private fun restoreTruthMap(dbEvals: List<GameEvaluation>) {
        if (dbEvals.isEmpty()) return
        session.truthMap = dbEvals.map { ev ->
            TruthMapEntry(moveIndex = ev.moveIndex,
                fen = session.fenSequence.getOrElse(ev.moveIndex) { "" },
                evalCp = ev.evalCp, evalDelta = ev.evalDelta,
                motif = ev.motif, pvLine = ev.pvLine)
        }
    }

    private fun computeOverthought(
        evals: List<GameEvaluation>,
        moveTimes: Map<Int, MoveTimeData>,
    ): Set<Int> = TimeAnalyzer.overthougtMoveIndices(
        TimeAnalyzer.analyze(evals, moveTimes.values.toList())
    )

    private fun updateState(
        triggers: Map<Int, List<com.acepero13.android.gamereviewer.domain.CoachingTrigger>>,
        weakTypes: Set<String>,
        highlights: List<com.acepero13.android.gamereviewer.engine.highlights.GameHighlight>,
        overthought: Set<Int>,
    ) {
        session.uiState.update { st ->
            val story  = debriefBuilder.buildGameStory(st.criticalMoments, session.uciMoves.size)
            val atEnd  = st.moveIndex == session.uciMoves.size && !st.showPostGameDebrief
            val result = if (atEnd) debriefBuilder.buildDebrief(st.gamePrediction, st.criticalMoments) else st.predictionMatchResult
            st.copy(
                isBackgroundAnalysisDone = true, backgroundAnalysisProgress = 1f,
                gameHighlights = highlights, triggersByMove = triggers,
                weakTriggerTypes = weakTypes, gameStory = story,
                showPostGameDebrief = st.showPostGameDebrief || atEnd,
                predictionMatchResult = result, gameStoryUnlocked = st.gameStoryUnlocked || atEnd,
                overthougtMoveIndices = overthought,
            )
        }
    }
}
