package com.acepero13.android.gamereviewer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acepero13.android.gamereviewer.domain.CoachingTrigger
import com.acepero13.android.gamereviewer.domain.InsightReconciler
import com.acepero13.android.gamereviewer.ui.screens.CoordinationQuizPhase

private val PanelBorder = Color(0xFF2D6A4F)

private fun CoachingTrigger.supportsSquareAnswer(): Boolean = when (this) {
    is CoachingTrigger.PreMoveChecklist -> true
    is CoachingTrigger.WorstPiece       -> true
    is CoachingTrigger.Safety           -> true
    is CoachingTrigger.CctCheck         -> true
    else                                -> false
}

private val forcingMoveStrings = ForcingSequenceStrings(
    animatingLabel  = "Watching the forcing sequence…",
    activePrompt    = "There's a forcing sequence here. Can you find it?",
    sandboxPrompt   = "You're in sandbox mode. Play what you think is the forcing continuation.",
    completeMessage = "Sequence complete. You can now explore variations freely.",
    tryLabel        = "Try it out",
    giveUpLabel     = "Give up — show the sequence",
)

private val punishBlunderStrings = ForcingSequenceStrings(
    animatingLabel  = "Playing the best response…",
    activePrompt    = "There's a strong response available. Can you find it?",
    sandboxPrompt   = "You're in sandbox mode. Play what you think punishes the blunder.",
    completeMessage = "Best response shown. Explore freely from here.",
    tryLabel        = "Try it yourself",
    giveUpLabel     = "Show me the best response",
)

@Composable
fun ProactiveCoachingPanel(
    trigger:                   CoachingTrigger,
    visible:                   Boolean,
    onDismiss:                 () -> Unit,
    modifier:                  Modifier = Modifier,
    onStartInteraction:        (() -> Unit)? = null,
    proactiveInteractiveMode:  Boolean = false,
    proactiveAnswerFeedback:   String? = null,
    proactiveAnswerIsCorrect:  Boolean? = null,
    proactiveFoundCount:       Int = 0,
    proactiveTotalCount:       Int = 0,
    coordinationQuizPhase:     CoordinationQuizPhase = CoordinationQuizPhase.ASKING,
    onCoordinationReveal:      (() -> Unit)? = null,
    onTryForcingSequence:      (() -> Unit)? = null,
    onShowForcingSequence:     (() -> Unit)? = null,
    onReplayForcingSequence:   (() -> Unit)? = null,
    forcingSequenceMode:       Boolean = false,
    forcingSequenceAnimating:  Boolean = false,
    forcingSequenceComplete:   Boolean = false,
    forcingSequenceCurrentStep: Int = 0,
    forcingSequenceTotalSteps:  Int = 0,
    isWeakArea:                Boolean = false,
) {
    val insight   = InsightReconciler.forTrigger(trigger)
    val hasAnswer = trigger.supportsSquareAnswer()
    val isForcing = trigger is CoachingTrigger.ForcingMove
    val isPunish  = trigger is CoachingTrigger.PunishBlunder
    val isCoord   = trigger is CoachingTrigger.CoordinatedAttack || trigger is CoachingTrigger.PieceHarmony
    val hasCoordGeometry = when (trigger) {
        is CoachingTrigger.CoordinatedAttack -> trigger.attackerSquares.isNotEmpty() && trigger.targetSquare != null
        is CoachingTrigger.PieceHarmony      -> trigger.attackerSquares.isNotEmpty() && trigger.targetSquares.isNotEmpty()
        else                                 -> false
    }

    var showQuestionsDialog  by remember { mutableStateOf(false) }
    var showReflectionDialog by remember { mutableStateOf(false) }
    var draftText            by remember { mutableStateOf("") }
    var savedReflection      by remember { mutableStateOf("") }

    if (showQuestionsDialog) {
        CoachingQuestionsDialog(
            insight          = insight,
            hasAnswer        = hasAnswer,
            isCoordination   = isCoord,
            isForcing        = isForcing || isPunish,
            savedReflection  = savedReflection,
            onEditReflection = { draftText = savedReflection; showReflectionDialog = true },
            onDismiss        = { showQuestionsDialog = false },
            onConfirm        = { showQuestionsDialog = false; onDismiss() },
        )
    }
    if (showReflectionDialog) {
        ReflectionEntryDialog(
            draftText    = draftText,
            onTextChange = { draftText = it },
            onSave       = { savedReflection = it; showReflectionDialog = false },
            onDismiss    = { showReflectionDialog = false },
        )
    }

    AnimatedVisibility(visible = visible, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically(), modifier = modifier) {
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, PanelBorder), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isWeakArea) {
                    Card(shape = RoundedCornerShape(6.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFF8F00).copy(alpha = 0.12f)), border = BorderStroke(1.dp, Color(0xFFFF8F00).copy(alpha = 0.35f)), modifier = Modifier.fillMaxWidth()) {
                        Text("⚠️ You've missed this pattern before — take your time.", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF8F00), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = insight.emoji, fontSize = 22.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(insight.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(insight.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { showQuestionsDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.ExpandMore, "Read coaching questions", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    }
                }
                HorizontalDivider(color = PanelBorder.copy(alpha = 0.4f))
                if (hasAnswer && onStartInteraction != null) {
                    BoardAnswerSection(trigger, proactiveInteractiveMode, proactiveAnswerFeedback, proactiveAnswerIsCorrect, proactiveFoundCount, proactiveTotalCount, onStartInteraction)
                }
                if (isForcing) {
                    ForcingSequenceSection(forcingMoveStrings, forcingSequenceAnimating, forcingSequenceComplete, forcingSequenceMode, forcingSequenceCurrentStep, forcingSequenceTotalSteps, onTryForcingSequence, onShowForcingSequence, onReplayForcingSequence, onDismiss)
                }
                if (isPunish) {
                    ForcingSequenceSection(punishBlunderStrings, forcingSequenceAnimating, forcingSequenceComplete, forcingSequenceMode, forcingSequenceCurrentStep, forcingSequenceTotalSteps, onTryForcingSequence, onShowForcingSequence, onReplayForcingSequence, onDismiss)
                }
                if (isCoord && hasCoordGeometry && onCoordinationReveal != null) {
                    CoordinationQuizSection(coordinationQuizPhase, onCoordinationReveal)
                }
                if (!forcingSequenceAnimating) {
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = PanelBorder)) {
                        Text("Got it — continue", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}
