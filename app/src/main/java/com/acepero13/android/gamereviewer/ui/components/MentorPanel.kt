package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState
import com.acepero13.android.gamereviewer.ui.screens.AnalysisViewModel

/**
 * Mentor mode content panel.
 *
 * A thin wrapper around [GuidedDiscoveryPanel] that wires ViewModel callbacks.
 * Rendered inside a [ModalBottomSheet] in AnalysisScreen so the board and move
 * tree remain visible behind it.
 *
 * If no guided-discovery insight is available yet (e.g. background analysis is still
 * running), the panel renders nothing to avoid a crash.
 *
 * When [isRecurringPattern] is true, an amber badge is shown above the panel to
 * signal that this mistake matches the player's top cross-game weakness.
 */
@Composable
fun MentorPanel(
    state:              AnalysisUiState,
    vm:                 AnalysisViewModel,
    modifier:           Modifier = Modifier,
    isRecurringPattern: Boolean = false,
) {
    val insight = state.guidedDiscoveryInsight ?: return

    Column(modifier = modifier) {
        if (isRecurringPattern) {
            Surface(
                shape             = RoundedCornerShape(6.dp),
                color             = Color(0xFFFF8F00).copy(alpha = 0.15f),
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    "⚠ Recurring pattern",
                    style      = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color(0xFFFF8F00),
                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        GuidedDiscoveryPanel(
            insight                   = insight,
            insightRevealed           = state.guidedDiscoveryInsightRevealed,
            thoughts                  = state.guidedDiscoveryThoughts,
            hintVisible               = state.guidedDiscoveryHintVisible,
            answerRevealed            = state.guidedDiscoveryAnswerRevealed,
            engineThinking            = state.guidedDiscoveryEngineThinking,
            revealedEvalCp            = state.guidedDiscoveryRevealedEvalCp,
            mentorMoveInputActive     = state.mentorMoveInputActive,
            mentorMoveChecking        = state.mentorMoveChecking,
            mentorMoveResult          = state.mentorMoveResult,
            mentorMoveFeedback        = state.mentorMoveFeedback,
            // Classification quiz
            showClassificationQuiz    = state.showClassificationQuiz,
            classificationOptions     = state.classificationOptions,
            classificationCorrectIndex  = state.classificationCorrectIndex,
            classificationSelectedIndex = state.classificationSelectedIndex,
            onSelectClassification    = vm::selectClassificationOption,
            // Callbacks
            onThoughtsChange          = vm::updateGuidedThoughts,
            onRevealHint              = vm::revealGuidedHint,
            onRevealAnswer            = vm::revealGuidedAnswer,
            onSubmit                  = vm::submitGuidedThoughts,
            onExit                    = vm::exitMentorMode,
            onToggleMoveInput         = vm::toggleMentorMoveInput,
            onRetryMove               = vm::retryMentorMove,
            modifier                  = Modifier.fillMaxWidth(),
        )
    }
}
