package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.ui.graphics.Color
import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.data.model.PlayerStats
import com.acepero13.android.gamereviewer.data.model.ReviewGame
import com.acepero13.android.gamereviewer.domain.CoachingTrigger
import com.acepero13.android.gamereviewer.domain.EndgameClassification
import com.acepero13.android.gamereviewer.domain.InsightReconciler
import com.acepero13.android.gamereviewer.domain.MiddlegamePlanClassification
import com.acepero13.android.gamereviewer.domain.OpeningDeviation
import com.acepero13.android.gamereviewer.domain.PivotalMoments
import com.acepero13.android.gamereviewer.engine.highlights.GameHighlight
import com.acepero13.chess.core.ui.board.BoardState
import com.acepero13.chess.core.ui.board.MarkedSquare
import com.acepero13.chess.core.ui.components.TreeDisplayItem

private const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

enum class ReviewMode { NAVIGATE, ANALYSE, MENTOR }
enum class AnalyseSubMode { VIEW, EDIT, EXPLORE, OPENING_EXPLORER }
enum class CoordinationQuizPhase { ASKING, REVEALING }
enum class MentorMoveResult { CORRECT, CLOSE, INCORRECT }

data class WeaknessContext(
    val trendTitle: String,
    val trendEmoji: String,
    val trendDescription: String,
    val gamesAffected: Int,
    val totalGamesAnalyzed: Int,
    val matchingMoveIndices: List<Int>,
)

data class ReflectionItem(
    val moveIndex: Int,
    val fen: String,
    val triggerTypeName: String,
    val correctLabel: String,
    val userAnswer: String? = null,
)

data class ClassificationOption(
    val label:       String,
    val description: String,
    val category:    CriticalMoment.ReasonCategory,
)

enum class GamePrediction(
    val emoji:       String,
    val label:       String,
    val description: String,
) {
    SPECIFIC_BLUNDER(
        emoji       = "🎯",
        label       = "I blundered somewhere specific",
        description = "A tactical oversight or material loss",
    ),
    TIME_PRESSURE(
        emoji       = "⏱️",
        label       = "I lost on time / rushed",
        description = "Clock pressure forced inaccurate moves",
    ),
    OUTPLAYED_POSITIONALLY(
        emoji       = "🧭",
        label       = "I got outplayed positionally",
        description = "Gradual squeeze or strategic errors",
    ),
    NOT_SURE(
        emoji       = "🔍",
        label       = "Not sure — let the engine decide",
        description = "Let the analysis reveal what went wrong",
    ),
}

data class PredictionMatchResult(
    val isAccurate: Boolean,
    val headline:   String,
    val detail:     String? = null,
)

data class AnalysisUiState(
    val game: ReviewGame? = null,
    val moveIndex: Int = 0,
    val totalMoves: Int = 0,
    val boardState: BoardState = BoardState(fen = START_FEN),
    val treeItems: List<TreeDisplayItem> = emptyList(),
    val openingSummary: String = "",
    val phaseSummary: String = "",
    val backgroundAnalysisProgress: Float = 0f,
    val isBackgroundAnalysisDone: Boolean = false,
    val currentComment: String = "",
    val hasAnnotationAtCurrent: Boolean = false,
    val showMissedMomentBanner: Boolean = false,
    val missedMomentMoveIndex: Int? = null,
    val shownCategoryAtMove: Map<String, Int> = emptyMap(),
    val guidedDiscoveryMode: Boolean = false,
    val guidedDiscoveryInsight: InsightReconciler.Insight? = null,
    val guidedDiscoveryCriticalMoment: CriticalMoment? = null,
    val guidedDiscoveryThoughts: String = "",
    val guidedDiscoveryHintVisible: Boolean = false,
    val guidedDiscoveryAnswerRevealed: Boolean = false,
    val guidedDiscoveryRevealedEvalCp: Int? = null,
    val guidedDiscoveryEngineThinking: Boolean = false,
    val showCriticalSheet: Boolean = false,
    val sandboxMode: Boolean = false,
    val sandboxEngineThinking: Boolean = false,
    val blunderGuardActive: Boolean = false,
    val blunderReflectionMode: Boolean = false,
    val blunderReflectionInsight: InsightReconciler.Insight? = null,
    val blunderPreMoveFen: String = "",
    val blunderCpLoss: Int = 0,
    val forcingSequenceMode: Boolean = false,
    val forcingSequenceAnimating: Boolean = false,
    val forcingSequenceComplete: Boolean = false,
    val forcingSequencePvMoves: List<String> = emptyList(),
    val forcingSequenceStartFen: String = "",
    val forcingSequenceCurrentStep: Int = 0,
    val criticalMoments: List<CriticalMoment> = emptyList(),
    val reviewMode: ReviewMode = ReviewMode.NAVIGATE,
    val analyseSubMode: AnalyseSubMode = AnalyseSubMode.VIEW,
    val previousReviewMode: ReviewMode = ReviewMode.NAVIGATE,
    val mentorAvailable: Boolean = false,
    val mentorMoveInputActive: Boolean = false,
    val mentorMoveChecking: Boolean = false,
    val mentorMoveResult: MentorMoveResult? = null,
    val mentorMoveFeedback: String = "",
    val evalBarVisible: Boolean = false,
    val bestMoveVisible: Boolean = false,
    val currentEvalCp: Int? = null,
    val currentArrowColor: Color = Color(0xCCF0A500.toInt()),
    val gameHighlights: List<GameHighlight> = emptyList(),
    val showStatsSheet: Boolean = false,
    val playerStats: Pair<PlayerStats, PlayerStats>? = null,
    val mentorContextLabel: String = "",
    val mentorSessionQueue: List<Int> = emptyList(),
    val mentorSessionIdx: Int = 0,
    val showClassificationQuiz: Boolean = false,
    val classificationOptions: List<ClassificationOption> = emptyList(),
    val classificationCorrectIndex: Int = -1,
    val classificationSelectedIndex: Int = -1,
    val guidedDiscoveryInsightRevealed: Boolean = false,
    val weaknessContext: WeaknessContext? = null,
    val showCoachsBriefing: Boolean = false,
    val triggersByMove: Map<Int, List<CoachingTrigger>> = emptyMap(),
    val activeProactiveTrigger: CoachingTrigger? = null,
    val showProactiveCoaching: Boolean = false,
    val triggersEngaged: Set<Int> = emptySet(),
    val coordinationQuizPhase: CoordinationQuizPhase = CoordinationQuizPhase.ASKING,
    val proactiveInteractiveMode: Boolean = false,
    val proactiveAnswerFeedback: String? = null,
    val proactiveAnswerIsCorrect: Boolean? = null,
    val coachHighlightSquares: List<MarkedSquare> = emptyList(),
    val proactiveHangingSquares: List<String> = emptyList(),
    val proactiveHangingOwnSquares: Set<String> = emptySet(),
    val proactiveFoundSquares: Set<String> = emptySet(),
    val showReflectionMode: Boolean = false,
    val reflectionItems: List<ReflectionItem> = emptyList(),
    val positionCoachEnabled: Boolean = false,
    val developerModeEnabled: Boolean = false,
    val positionCoachDismissedMoves: Set<Int> = emptySet(),
    val openingDeviation: OpeningDeviation? = null,
    val showOpeningDeviationPanel: Boolean = false,
    val openingDeviationDismissed: Boolean = false,
    val endgameClassification: EndgameClassification? = null,
    val showEndgameRecognitionPanel: Boolean = false,
    val endgamePanelDismissed: Boolean = false,
    val middlegamePlanClassification: MiddlegamePlanClassification? = null,
    val showMiddlegamePlanPanel: Boolean = false,
    val middlegamePlanPanelDismissed: Boolean = false,
    val showPredictionGate: Boolean = false,
    val gamePrediction: GamePrediction? = null,
    val gameStory: String = "",
    val gameStoryDismissed: Boolean = false,
    val gameStoryUnlocked: Boolean = false,
    val showPostGameDebrief: Boolean = false,
    val predictionMatchResult: PredictionMatchResult? = null,
    val weakTriggerTypes: Set<String> = emptySet(),
    val pivotalMoments: PivotalMoments? = null,
    val showPivotalMomentsPanel: Boolean = false,
    val overthougtMoveIndices: Set<Int> = emptySet(),
    val showCalibrationPanel: Boolean = false,
    val calibrationTrigger: CoachingTrigger.EvalCalibration? = null,
    val calibrationUserValue: Int = 0,
    val calibrationLocked: Boolean = false,
    val calibrationFeedback: String = "",
    val calibrationFeedbackPositive: Boolean = false,
)
