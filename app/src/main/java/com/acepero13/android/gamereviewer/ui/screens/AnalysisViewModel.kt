package com.acepero13.android.gamereviewer.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acepero13.android.gamereviewer.data.db.CriticalMomentDao
import com.acepero13.android.gamereviewer.data.db.GameEvaluationDao
import com.acepero13.android.gamereviewer.data.db.MoveTimeDao
import com.acepero13.android.gamereviewer.data.model.Snippet
import com.acepero13.android.gamereviewer.data.repository.GameRepository
import com.acepero13.android.gamereviewer.data.repository.SettingsRepository
import com.acepero13.android.gamereviewer.data.repository.SnippetRepository
import com.acepero13.android.gamereviewer.data.repository.TriggerMasteryRepository
import com.acepero13.android.gamereviewer.domain.EndgameRecognizer
import com.acepero13.android.gamereviewer.domain.MiddlegamePlanDetector
import com.acepero13.android.gamereviewer.domain.OpeningDeviationAnalyzer
import com.acepero13.android.gamereviewer.domain.TruthMapBuilder
import com.acepero13.android.gamereviewer.ui.screens.analysis.AnnotationController
import com.acepero13.android.gamereviewer.ui.screens.analysis.BackgroundAnalysisController
import com.acepero13.android.gamereviewer.ui.screens.analysis.BoardStateApplicator
import com.acepero13.android.gamereviewer.ui.screens.analysis.CalibrationController
import com.acepero13.android.gamereviewer.ui.screens.analysis.ClassificationQuizController
import com.acepero13.android.gamereviewer.ui.screens.analysis.CoachDebugFormatter
import com.acepero13.android.gamereviewer.ui.screens.analysis.CoachingTriggerProcessor
import com.acepero13.android.gamereviewer.ui.screens.analysis.CriticalMomentController
import com.acepero13.android.gamereviewer.ui.screens.analysis.EngineOverlayController
import com.acepero13.android.gamereviewer.ui.screens.analysis.ForcingSequenceController
import com.acepero13.android.gamereviewer.ui.screens.analysis.FreshAnalysisController
import com.acepero13.android.gamereviewer.ui.screens.analysis.GameAnalysisRunner
import com.acepero13.android.gamereviewer.ui.screens.analysis.GameDebriefBuilder
import com.acepero13.android.gamereviewer.ui.screens.analysis.GameLoaderController
import com.acepero13.android.gamereviewer.ui.screens.analysis.GameSequenceBuilder
import com.acepero13.android.gamereviewer.ui.screens.analysis.GameSession
import com.acepero13.android.gamereviewer.ui.screens.analysis.GuidedDiscoveryController
import com.acepero13.android.gamereviewer.ui.screens.analysis.MentorModeController
import com.acepero13.android.gamereviewer.ui.screens.analysis.MentorMoveController
import com.acepero13.android.gamereviewer.ui.screens.analysis.MentorSessionController
import com.acepero13.android.gamereviewer.ui.screens.analysis.MotifMapper
import com.acepero13.android.gamereviewer.ui.screens.analysis.MoveTreeBuilder
import com.acepero13.android.gamereviewer.ui.screens.analysis.NavigationController
import com.acepero13.android.gamereviewer.ui.screens.analysis.ProactiveCoachingController
import com.acepero13.android.gamereviewer.ui.screens.analysis.ReflectionController
import com.acepero13.android.gamereviewer.ui.screens.analysis.ReviewModeController
import com.acepero13.android.gamereviewer.ui.screens.analysis.SandboxController
import com.acepero13.android.gamereviewer.ui.screens.analysis.TruthMapRestorer
import com.acepero13.android.gamereviewer.ui.screens.analysis.UiDismissController
import com.acepero13.chess.core.data.db.PositionAnnotationDao
import com.acepero13.chess.core.engine.StockfishEngine
import com.acepero13.chess.core.opening.OpeningClassifier
import com.github.bhlangonijr.chesslib.Square
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnalysisViewModel(
    private val gameId: Long,
    private val repo: GameRepository,
    private val annotationDao: PositionAnnotationDao,
    private val criticalMomentDao: CriticalMomentDao,
    private val gameEvaluationDao: GameEvaluationDao,
    private val moveTimeDao: MoveTimeDao,
    private val endgameEncounterDao: com.acepero13.android.gamereviewer.data.db.EndgameEncounterDao,
    private val engine: StockfishEngine,
    private val opening: OpeningClassifier,
    private val truthMapBuilder: TruthMapBuilder,
    private val settingsRepo: SettingsRepository,
    private val masteryRepo: TriggerMasteryRepository,
    private val deviationAnalyzer: OpeningDeviationAnalyzer,
    private val endgameRecognizer: EndgameRecognizer,
    private val middlegamePlanDetector: MiddlegamePlanDetector,
    private val snippetRepo: SnippetRepository,
) : ViewModel() {

    private val session = GameSession(MutableStateFlow(AnalysisUiState()), viewModelScope, gameId)
    val uiState: StateFlow<AnalysisUiState> = session.uiState.asStateFlow()

    private val motifMapper      = MotifMapper()
    private val treeBuilder      = MoveTreeBuilder(session)
    private val engineOverlay    = EngineOverlayController(session, engine)
    private val applicator       = BoardStateApplicator(session, treeBuilder, engineOverlay)
    private val navigation       = NavigationController(session, applicator, motifMapper)
    private val annotation       = AnnotationController(session, annotationDao, treeBuilder)
    private val criticalMomentCtrl = CriticalMomentController(session, criticalMomentDao, annotationDao, treeBuilder, motifMapper)
    private val sandbox          = SandboxController(session, engine, engineOverlay)
    private val forcingSeq       = ForcingSequenceController(session, engine, sandbox)
    private val quizCtrl         = ClassificationQuizController(session, annotationDao, treeBuilder)
    private val guidedDiscovery  = GuidedDiscoveryController(session, engine, annotationDao, criticalMomentDao, navigation, treeBuilder, quizCtrl)
    private val mentorMoveCtrl   = MentorMoveController(session, engine, quizCtrl)
    private val mentorMode       = MentorModeController(session, navigation)
    private val reflection       = ReflectionController(session, masteryRepo)
    private val mentorSession    = MentorSessionController(session, criticalMomentDao, mentorMode, reflection)
    private val calibration      = CalibrationController(session)
    private val proactiveCoach   = ProactiveCoachingController(session)
    private val reviewModeCtrl   = ReviewModeController(session, engineOverlay, sandbox)
    private val dismiss          = UiDismissController(session, gameEvaluationDao, moveTimeDao)
    private val coachDebug       = CoachDebugFormatter(session)
    private val triggerProcessor = CoachingTriggerProcessor(session, masteryRepo, moveTimeDao, gameEvaluationDao)
    private val debriefBuilder   = GameDebriefBuilder(navigation)
    private val freshAnalysis    = FreshAnalysisController(session, truthMapBuilder, gameEvaluationDao, criticalMomentDao, moveTimeDao, endgameEncounterDao, triggerProcessor, debriefBuilder, motifMapper, navigation)
    private val truthRestorer    = TruthMapRestorer(session, gameEvaluationDao, criticalMomentDao, moveTimeDao, triggerProcessor, debriefBuilder, navigation)
    private val backgroundAnalysis = BackgroundAnalysisController(session, truthRestorer, freshAnalysis)
    private val analysisRunner   = GameAnalysisRunner(session, opening, deviationAnalyzer, endgameRecognizer, middlegamePlanDetector, endgameEncounterDao)
    private val sequenceBuilder  = GameSequenceBuilder(session, annotationDao)
    private val gameLoader       = GameLoaderController(session, repo, criticalMomentDao, settingsRepo, sequenceBuilder, analysisRunner, applicator, motifMapper, backgroundAnalysis)

    init {
        sandbox.applyMoveIndexCallback = { index -> navigation.applyMoveIndex(index) }
        gameLoader.loadGame()
    }

    // Navigation
    fun goToMove(index: Int) = navigation.goToMove(index)
    fun stepForward()        = navigation.stepForward()
    fun stepBackward()       = navigation.stepBackward()
    fun goToStart()          = navigation.goToStart()
    fun goToEnd()            = navigation.goToEnd()
    fun onMoveNodeClick(nodeId: Long) = navigation.onMoveNodeClick(nodeId)

    // Annotation
    fun onArrowDrawn(from: Square, to: Square) = annotation.onArrowDrawn(from, to)
    fun onSquareMarked(square: Square)          = annotation.onSquareMarked(square)
    fun undoLastArrow()                         = annotation.undoLastArrow()
    fun clearArrows()                           = annotation.clearArrows()
    fun updateMoveComment(comment: String)      = annotation.updateMoveComment(comment)

    // Critical moment
    fun markCurrentAsCritical() = criticalMomentCtrl.markCurrentAsCritical()
    fun dismissCriticalSheet()  = criticalMomentCtrl.dismissCriticalSheet()
    fun saveCriticalAnswers(plan: String, threats: String, candidates: String) =
        criticalMomentCtrl.saveCriticalAnswers(plan, threats, candidates)

    // Stats and dismissals
    fun toggleStatsSheet()               = dismiss.toggleStatsSheet()
    fun dismissStatsSheet()              = dismiss.dismissStatsSheet()
    fun dismissMissedMomentBanner()      = dismiss.dismissMissedMomentBanner()
    fun dismissOpeningDeviationPanel()   = dismiss.dismissOpeningDeviationPanel()
    fun dismissEndgameRecognitionPanel() = dismiss.dismissEndgameRecognitionPanel()
    fun dismissMiddlegamePlanPanel()     = dismiss.dismissMiddlegamePlanPanel()
    fun submitPrediction(prediction: GamePrediction) = dismiss.submitPrediction(prediction)
    fun skipPrediction()                 = dismiss.skipPrediction()
    fun dismissGameStory()               = dismiss.dismissGameStory()
    fun dismissPostGameDebrief()         = dismiss.dismissPostGameDebrief()

    fun reviewMissedMoment() {
        val idx = session.uiState.value.missedMomentMoveIndex ?: return
        dismiss.dismissMissedMomentBanner()
        viewModelScope.launch { delay(200); mentorMode.enterMentorMode(idx) }
    }

    // Review mode
    fun enterAnalyseMode()                       = reviewModeCtrl.enterAnalyseMode()
    fun setReviewMode(mode: ReviewMode)          = reviewModeCtrl.setReviewMode(mode)
    fun setAnalyseSubMode(sub: AnalyseSubMode)   = reviewModeCtrl.setAnalyseSubMode(sub)
    fun toggleEvalBar()                          = reviewModeCtrl.toggleEvalBar()
    fun toggleBestMove()                         = reviewModeCtrl.toggleBestMove()
    fun setArrowColor(color: Color)              = reviewModeCtrl.setArrowColor(color)

    // Guided discovery
    fun enterGuidedDiscovery(moveIndex: Int) = guidedDiscovery.enterGuidedDiscovery(moveIndex)
    fun exitGuidedDiscovery()                = guidedDiscovery.exitGuidedDiscovery()
    fun updateGuidedThoughts(text: String)   = guidedDiscovery.updateGuidedThoughts(text)
    fun revealGuidedHint()                   = guidedDiscovery.revealGuidedHint()
    fun revealGuidedAnswer()                 = guidedDiscovery.revealGuidedAnswer()
    fun submitGuidedThoughts() = guidedDiscovery.submitGuidedThoughts(
        advanceMentorSession = mentorSession::advanceMentorSession,
        exitMentorMode       = mentorMode::exitMentorMode,
    )

    // Mentor mode
    fun enterMentorMode(targetMoveIndex: Int = session.uiState.value.moveIndex) =
        mentorMode.enterMentorMode(targetMoveIndex)
    fun exitMentorMode() = mentorMode.exitMentorMode()

    // Mentor session
    fun enterMentorSession()                     = mentorSession.enterMentorSession()
    fun dismissPivotalMomentsPanel()             = mentorSession.dismissPivotalMomentsPanel()
    fun reviewPivotalMoment(moveIndex: Int)      = mentorSession.reviewPivotalMoment(moveIndex)
    fun dismissCoachsBriefing()                  = mentorSession.dismissCoachsBriefing()
    fun advanceMentorSession()                   = mentorSession.advanceMentorSession()

    // Mentor move input
    fun toggleMentorMoveInput()                  = mentorMoveCtrl.toggleMentorMoveInput()
    fun onMentorSquareTap(square: Square)        = mentorMoveCtrl.onMentorSquareTap(square)
    fun retryMentorMove()                        = mentorMoveCtrl.retryMentorMove()

    // Reflection
    fun enterReflectionMode()                    = reflection.enterReflectionMode(mentorMode::exitMentorMode)
    fun answerReflection(moveIndex: Int, answer: String) = reflection.answerReflection(moveIndex, answer)
    fun exitReflectionMode()                     = reflection.exitReflectionMode(mentorMode::exitMentorMode)

    // Proactive coaching
    fun enterProactiveCoaching()                 = proactiveCoach.enterProactiveCoaching()
    fun dismissProactiveCoaching()               = proactiveCoach.dismissProactiveCoaching()
    fun onCoordinationQuizReveal()               = proactiveCoach.onCoordinationQuizReveal()
    fun dismissPositionCoach()                   = proactiveCoach.dismissPositionCoach()
    fun startProactiveInteraction()              = proactiveCoach.startProactiveInteraction()
    fun answerProactiveQuestion(square: Square)  = proactiveCoach.answerProactiveQuestion(square)

    // Calibration
    fun onCalibrationValueChange(value: Int)     = calibration.onCalibrationValueChange(value)
    fun lockInCalibration()                      = calibration.lockInCalibration()
    fun dismissCalibration()                     = calibration.dismissCalibration()

    // Sandbox
    fun enterSandboxMode()                       = sandbox.enterSandboxMode()
    fun exitSandboxMode()                        = sandbox.exitSandboxMode()
    fun onSandboxSquareTap(square: Square)       = sandbox.onSandboxSquareTap(square)
    fun retryAfterBlunder()                      = sandbox.retryAfterBlunder()
    fun continueAfterBlunder()                   = sandbox.continueAfterBlunder()

    // Forcing sequence
    fun enterForcingSequenceMode()               = forcingSeq.enterForcingSequenceMode()
    fun showForcingSequence()                    = forcingSeq.showForcingSequence()
    fun replayForcingSequence()                  = forcingSeq.replayForcingSequence()
    fun exitForcingSequenceMode()                = forcingSeq.exitForcingSequenceMode()

    // Classification quiz
    fun triggerClassificationQuiz()              = quizCtrl.triggerClassificationQuiz()
    fun selectClassificationOption(index: Int)   = quizCtrl.selectClassificationOption(index)

    // Coach debug
    fun buildCoachEvalPrompt(): String?          = coachDebug.buildCoachEvalPrompt()

    // Snippet Library
    fun bookmarkPosition(title: String, tags: String, notes: String) {
        val state = session.uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            snippetRepo.insert(
                Snippet(
                    title        = title.ifBlank { "Position at move ${state.moveIndex}" },
                    fen          = state.boardState.fen,
                    sourceGameId = gameId,
                    moveIndex    = state.moveIndex,
                    tags         = tags,
                    notes        = notes,
                )
            )
        }
    }
}
