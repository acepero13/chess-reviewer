package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.data.db.CriticalMomentDao
import com.acepero13.android.gamereviewer.data.db.EndgameEncounterDao
import com.acepero13.android.gamereviewer.data.db.GameEvaluationDao
import com.acepero13.android.gamereviewer.data.db.MoveTimeDao
import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.data.model.GameEvaluation
import com.acepero13.android.gamereviewer.data.model.MoveTimeData
import com.acepero13.android.gamereviewer.domain.TimeAnalyzer
import com.acepero13.android.gamereviewer.domain.TruthMapBuilder
import com.acepero13.android.gamereviewer.engine.highlights.GameHighlightEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class FreshAnalysisController(
    private val session: GameSession,
    private val truthMapBuilder: TruthMapBuilder,
    private val gameEvaluationDao: GameEvaluationDao,
    private val criticalMomentDao: CriticalMomentDao,
    private val moveTimeDao: MoveTimeDao,
    private val endgameEncounterDao: EndgameEncounterDao,
    private val triggerProcessor: CoachingTriggerProcessor,
    private val debriefBuilder: GameDebriefBuilder,
    private val motifMapper: MotifMapper,
    private val navigation: NavigationController,
) {

    suspend fun run() {
        val map  = truthMapBuilder.build(session.uciMoves) { processed, total ->
            session.uiState.update { it.copy(backgroundAnalysisProgress = processed.toFloat() / total) }
        }
        session.truthMap = map
        val evaluations = map.toEvaluations(session.gameId)
        persistEvaluations(evaluations)
        val moveTimes  = withContext(Dispatchers.IO) { moveTimeDao.getByGameId(session.gameId).associateBy { it.moveIndex } }
        val allMoments = withContext(Dispatchers.IO) { criticalMomentDao.getAll() }
        val weakTypes  = WeaknessAnalyzer.buildWeakTriggerTypes(allMoments)
        val triggers   = triggerProcessor.evaluate(evaluations, weakTypes)
        session.uiState.update { it.copy(triggersByMove = triggers, weakTriggerTypes = weakTypes) }
        persistCriticalMoments(evaluations)
        val highlights  = GameHighlightEngine.run(map, session.sanMoves, session.fenSequence, moveTimes)
        val freshMoments = withContext(Dispatchers.IO) { criticalMomentDao.getByGameId(session.gameId) }
        val overthought  = TimeAnalyzer.overthougtMoveIndices(TimeAnalyzer.analyze(evaluations, moveTimes.values.toList()))
        updateFinalState(freshMoments, highlights, weakTypes, overthought)
        val current = session.uiState.value.moveIndex
        if (current > 0) navigation.checkMissedMoments(0, current)
    }

    private fun persistEvaluations(evaluations: List<GameEvaluation>) {
        if (evaluations.isEmpty()) return
        session.scope.launch(Dispatchers.IO) { gameEvaluationDao.insertAll(evaluations) }
    }

    private fun persistCriticalMoments(evaluations: List<GameEvaluation>) {
        val moments = session.truthMap
            .filter { it.isCritical || it.isSignificantTacticalMiss }
            .map { entry ->
                CriticalMoment(gameId = session.gameId, moveIndex = entry.moveIndex,
                    type = CriticalMoment.Type.ENGINE_MARKED.name,
                    severity = kotlin.math.abs(entry.playerEvalDelta),
                    reasonCategory = motifMapper.motifToReason(entry),
                    explanationState = CriticalMoment.ExplanationState.HIDDEN.name, fen = entry.fen)
            }
        if (moments.isEmpty()) return
        session.scope.launch(Dispatchers.IO) {
            criticalMomentDao.insertAll(moments)
            if (moments.any { it.reasonCategory == CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE.name })
                endgameEncounterDao.markMistake(session.gameId)
        }
    }

    private fun updateFinalState(
        freshMoments: List<CriticalMoment>,
        highlights: List<com.acepero13.android.gamereviewer.engine.highlights.GameHighlight>,
        weakTypes: Set<String>,
        overthought: Set<Int>,
    ) {
        session.uiState.update { st ->
            val story  = debriefBuilder.buildGameStory(freshMoments, session.uciMoves.size)
            val atEnd  = st.moveIndex == session.uciMoves.size && !st.showPostGameDebrief
            val result = if (atEnd) debriefBuilder.buildDebrief(st.gamePrediction, freshMoments) else st.predictionMatchResult
            st.copy(
                isBackgroundAnalysisDone = true, backgroundAnalysisProgress = 1f,
                criticalMoments = freshMoments, gameHighlights = highlights,
                weakTriggerTypes = weakTypes, gameStory = story,
                showPostGameDebrief = st.showPostGameDebrief || atEnd,
                predictionMatchResult = result, gameStoryUnlocked = st.gameStoryUnlocked || atEnd,
                overthougtMoveIndices = overthought,
            )
        }
    }
}

private fun List<com.acepero13.android.gamereviewer.domain.TruthMapEntry>.toEvaluations(gameId: Long) =
    map { entry ->
        GameEvaluation(gameId = gameId, moveIndex = entry.moveIndex,
            evalCp = entry.evalCp, evalDelta = entry.evalDelta,
            motif = entry.motif, pvLine = entry.pvLine)
    }
