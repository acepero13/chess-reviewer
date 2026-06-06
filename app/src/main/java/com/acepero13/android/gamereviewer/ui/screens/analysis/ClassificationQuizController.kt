package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.android.gamereviewer.ui.screens.ClassificationOption
import com.acepero13.chess.core.data.db.PositionAnnotationDao
import com.acepero13.chess.core.data.model.PositionAnnotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ClassificationQuizController(
    private val session: GameSession,
    private val annotationDao: PositionAnnotationDao,
    private val treeBuilder: MoveTreeBuilder,
) {

    fun triggerClassificationQuiz() {
        val moment = session.uiState.value.guidedDiscoveryCriticalMoment ?: return
        val correct = moment.toReason()
        val options = buildOptions(correct)
        val correctIdx = options.indexOfFirst { it.category == correct }
        session.uiState.update {
            it.copy(
                showClassificationQuiz = true, classificationOptions = options,
                classificationCorrectIndex = correctIdx, classificationSelectedIndex = -1,
            )
        }
    }

    fun selectClassificationOption(index: Int) {
        val options = session.uiState.value.classificationOptions
        if (index !in options.indices) return
        val selected = options[index]
        session.uiState.update { it.copy(classificationSelectedIndex = index) }
        val fen = session.uiState.value.boardState.fen
        session.scope.launch(Dispatchers.IO) {
            persistQuizAnnotation(fen, selected.label)
        }
    }

    private suspend fun persistQuizAnnotation(fen: String, label: String) {
        val existing = session.annotationCache[fen] ?: annotationDao.getByFen(fen)
        val quizNote = "My assessment: $label"
        val newComment = buildString {
            val prev = existing?.moveComment?.takeIf { it.isNotBlank() }
            if (prev != null) { appendLine(prev); appendLine() }
            appendLine(quizNote)
        }.trim()
        val upd = (existing ?: PositionAnnotation(fen = fen)).copy(moveComment = newComment)
        annotationDao.upsert(upd)
        session.annotationCache[fen] = upd
        session.uiState.update { it.copy(currentComment = newComment) }
        treeBuilder.refreshTreeItems()
    }

    private fun buildOptions(correct: CriticalMoment.ReasonCategory): List<ClassificationOption> {
        val distractors = CriticalMoment.ReasonCategory.entries
            .filter { it != correct }.shuffled().take(3)
        return (listOf(correct) + distractors).shuffled().map { cat ->
            ClassificationOption(
                label = CategoryLabels.label(cat),
                description = CategoryLabels.description(cat),
                category = cat,
            )
        }
    }
}

