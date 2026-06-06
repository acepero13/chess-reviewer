package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.acepero13.android.gamereviewer.domain.InsightReconciler
import com.acepero13.android.gamereviewer.ui.screens.ClassificationOption
import com.acepero13.android.gamereviewer.ui.screens.MentorMoveResult

@Composable
fun GuidedDiscoveryPanel(
    insight: InsightReconciler.Insight,
    insightRevealed: Boolean,
    thoughts: String,
    hintVisible: Boolean,
    answerRevealed: Boolean,
    engineThinking: Boolean,
    revealedEvalCp: Int?,
    mentorMoveInputActive: Boolean,
    mentorMoveChecking: Boolean,
    mentorMoveResult: MentorMoveResult?,
    mentorMoveFeedback: String,
    showClassificationQuiz: Boolean,
    classificationOptions: List<ClassificationOption>,
    classificationCorrectIndex: Int,
    classificationSelectedIndex: Int,
    onSelectClassification: (Int) -> Unit,
    onThoughtsChange: (String) -> Unit,
    onRevealHint: () -> Unit,
    onRevealAnswer: () -> Unit,
    onSubmit: () -> Unit,
    onExit: () -> Unit,
    onToggleMoveInput: () -> Unit,
    onRetryMove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InsightHeaderCard(insight, insightRevealed, expanded, onToggleExpanded = { expanded = !expanded })
        MoveInputSection(mentorMoveInputActive, mentorMoveChecking, mentorMoveResult,
            mentorMoveFeedback, answerRevealed, onToggleMoveInput, onRetryMove)
        AnimatedVisibility(showClassificationQuiz,
            enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            ClassificationQuizSection(classificationOptions, classificationCorrectIndex,
                classificationSelectedIndex, onSelectClassification)
        }
        ConceptualHintSection(hint = insight.conceptualHint, visible = hintVisible)
        EngineRevealSection(visible = answerRevealed, evalCp = revealedEvalCp)
        DiscoveryActionButtons(answerRevealed, mentorMoveResult, engineThinking,
            mentorMoveChecking, hintVisible, onSubmit, onRevealHint, onRevealAnswer, onExit)
        ThoughtsInput(thoughts, onThoughtsChange, visible = !mentorMoveInputActive)
    }
}
