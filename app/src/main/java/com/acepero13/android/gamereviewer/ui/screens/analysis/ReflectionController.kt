package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.data.repository.TriggerMasteryRepository
import com.acepero13.android.gamereviewer.ui.screens.ReflectionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ReflectionController(
    private val session: GameSession,
    private val masteryRepo: TriggerMasteryRepository,
) {

    fun enterReflectionMode(exitMentorMode: () -> Unit) {
        val engaged = session.uiState.value.triggersEngaged
        val items = buildReflectionItems(engaged)
        if (items.isEmpty()) { exitMentorMode(); return }
        session.uiState.update { it.copy(showReflectionMode = true, reflectionItems = items) }
    }

    fun answerReflection(moveIndex: Int, answer: String) {
        val updated = session.uiState.value.reflectionItems.map { item ->
            if (item.moveIndex == moveIndex) item.copy(userAnswer = answer) else item
        }
        session.uiState.update { it.copy(reflectionItems = updated) }
        val item = updated.firstOrNull { it.moveIndex == moveIndex } ?: return
        session.scope.launch(Dispatchers.IO) {
            if (answer == item.correctLabel) masteryRepo.recordCorrect(item.triggerTypeName)
            else masteryRepo.recordIncorrect(item.triggerTypeName)
        }
    }

    fun exitReflectionMode(exitMentorMode: () -> Unit) {
        session.uiState.update { it.copy(showReflectionMode = false, reflectionItems = emptyList()) }
        exitMentorMode()
    }

    private fun buildReflectionItems(engaged: Set<Int>): List<ReflectionItem> =
        session.uiState.value.triggersByMove.entries
            .filter { (idx, _) -> idx !in engaged }
            .sortedBy { (idx, _) -> idx }
            .mapNotNull { (idx, triggers) ->
                val trigger = triggers.firstOrNull() ?: return@mapNotNull null
                val fen = session.fenSequence.getOrElse(idx) { "" }
                ReflectionItem(
                    moveIndex = idx, fen = fen,
                    triggerTypeName = trigger.typeName(),
                    correctLabel = trigger.displayLabel(),
                )
            }
}
