package com.acepero13.android.gamereviewer.ui.screens.analysis

import com.acepero13.android.gamereviewer.domain.CoachingTrigger
import com.acepero13.android.gamereviewer.domain.EndgameClassification
import com.acepero13.android.gamereviewer.domain.InsightReconciler
import com.acepero13.android.gamereviewer.domain.MiddlegamePlanClassification
import com.acepero13.android.gamereviewer.engine.highlights.GameHighlight

internal class CoachDebugFormatter(private val session: GameSession) {

    fun buildCoachEvalPrompt(): String? {
        val s = session.uiState.value
        val idx = s.moveIndex
        if (idx <= 0 || session.fenSequence.size < idx) return null
        return when {
            s.guidedDiscoveryMode && s.guidedDiscoveryInsight != null -> buildGuidedPrompt(s, idx)
            s.showProactiveCoaching && s.activeProactiveTrigger != null -> buildProactivePrompt(s, idx)
            s.showMiddlegamePlanPanel && s.middlegamePlanClassification != null ->
                buildMiddlegamePlanEvalPrompt(s.middlegamePlanClassification)
            s.showEndgameRecognitionPanel && s.endgameClassification != null ->
                buildEndgameEvalPrompt(s.endgameClassification)
            else -> null
        }
    }

    fun buildMiddlegamePlanEvalPrompt(c: MiddlegamePlanClassification): String {
        val insights = c.plans.map { InsightReconciler.forMiddlegamePlan(it) }
        return buildString {
            appendLine("## Position\n\nFEN: ${c.fen}\nMove index: ${c.moveIndex}\nGame phase: Middlegame\n\n---\n")
            appendLine("## Detected Plans (${c.plans.size})\n")
            c.plans.forEachIndexed { i, plan ->
                val ins = insights.getOrNull(i)
                appendLine("### Plan ${i + 1}: ${plan.title}")
                appendLine("**Type:** ${plan.type}\n**Priority:** ${plan.priority}")
                plan.affectedFile?.let { appendLine("**Affected file:** $it") }
                appendLine("**Advice:** ${plan.planAdvice}")
                if (ins != null) {
                    appendLine("\n**Coaching output:**\nTitle: ${ins.title}\nDescription: ${ins.description}")
                    ins.questions.forEachIndexed { qi, q -> appendLine("  ${qi + 1}. $q") }
                    appendLine("Conceptual hint: ${ins.conceptualHint}")
                }
                appendLine()
            }
        }
    }

    fun buildEndgameEvalPrompt(c: EndgameClassification): String {
        val insight = InsightReconciler.forEndgame(chapter = c.entry.chapter, name = c.entry.name)
        return buildString {
            appendLine("## Position\n\nFEN: ${c.fen}\nFirst endgame move index: ${c.firstEndgameMoveIndex}\nGame phase: Endgame\n\n---\n")
            appendLine("## Endgame Classification\n")
            appendLine("**Name:** ${c.entry.name}\n**Chapter:** ${c.entry.chapter} of *100 Endgames You Should Know*")
            appendLine("**Category:** ${c.entry.category}\n**Material signature:** ${c.entry.materialSignature}")
            appendLine("**Study advice:** ${c.entry.studyAdvice}\n\n---\n")
            appendLine("## App's coaching output\n\n**Title:** ${insight.title}\n**Description:** ${insight.description}\n")
            insight.questions.forEachIndexed { i, q -> appendLine("${i + 1}. $q") }
            appendLine("\n**Conceptual hint:** ${insight.conceptualHint}")
        }
    }

    private fun buildGuidedPrompt(s: com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState, idx: Int) = buildString {
        append(buildPositionSection(idx, s.gameHighlights))
        append(buildInsightSection(s.guidedDiscoveryCriticalMoment?.reasonCategory ?: "UNKNOWN", s.guidedDiscoveryInsight!!))
        appendLine("## Rule Debug Info\n")
        s.guidedDiscoveryCriticalMoment?.let {
            appendLine("**Panel source:** Guided Discovery\n**CriticalMoment.type:** ${it.type}\n**severity:** ${it.severity} cp\n**explanationState:** ${it.explanationState}")
        }
        append(buildHighlightInfo(s.gameHighlights, s.triggersByMove[idx] ?: emptyList(), idx))
    }

    private fun buildProactivePrompt(s: com.acepero13.android.gamereviewer.ui.screens.AnalysisUiState, idx: Int) = buildString {
        val trigger = s.activeProactiveTrigger!!
        append(buildPositionSection(idx, s.gameHighlights))
        append(buildInsightSection(trigger.typeName(), InsightReconciler.forTrigger(trigger)))
        appendLine("## Rule Debug Info\n")
        appendLine("**Panel source:** Proactive Coaching\n**Trigger:** CoachingTrigger.${trigger::class.simpleName}\n**Props:** ${CoachTriggerProps.format(trigger)}")
        append(buildHighlightInfo(s.gameHighlights, s.triggersByMove[idx] ?: emptyList(), idx))
    }

    private fun buildPositionSection(idx: Int, highlights: List<GameHighlight>): String {
        val fenBefore = session.fenSequence[idx - 1]
        val san = if (session.sanMoves.size >= idx) session.sanMoves[idx - 1] else "?"
        val isWhite = idx % 2 == 1
        val evalBefore = session.truthMap.getOrNull(idx - 1)?.evalCp
        val evalAfter = session.truthMap.getOrNull(idx)?.evalCp
        val cpLoss = if (evalBefore != null && evalAfter != null) {
            val d = evalAfter - evalBefore; if (isWhite) -d else d
        } else null
        val phase = highlights.firstOrNull { it.moveIndex == idx }?.phase?.name?.lowercase()
            ?.replaceFirstChar { it.uppercase() }
            ?: when { idx <= 15 -> "Opening"; idx <= 35 -> "Middlegame"; else -> "Endgame" }
        return buildString {
            appendLine("## Position\n\nFEN: $fenBefore\nMove played: $san")
            appendLine("Color: ${if (isWhite) "White" else "Black"}\nMove number: ${(idx + 1) / 2}\nGame phase: $phase")
            appendLine("Eval BEFORE: ${fmtCp(evalBefore)}\nEval AFTER: ${fmtCp(evalAfter)}\nCpLoss: ${cpLoss?.let { "$it cp" } ?: "N/A"}\n\n---\n")
        }
    }

    private fun buildInsightSection(triggerType: String, insight: InsightReconciler.Insight) = buildString {
        appendLine("## Coaching trigger type\n\n$triggerType\n---")
        appendLine("## App's coaching output\n\n**Title:** ${insight.title}\n**Description:** ${insight.description}")
        insight.questions.forEachIndexed { i, q -> appendLine("${i + 1}. $q") }
        appendLine("**Hint:** ${insight.conceptualHint}\n---\n")
    }

    private fun buildHighlightInfo(highlights: List<GameHighlight>, triggers: List<CoachingTrigger>, idx: Int) = buildString {
        if (triggers.isNotEmpty()) {
            appendLine("\n**All triggers at move $idx:**")
            triggers.forEach { appendLine("  • ${it.typeName()} (CoachingTrigger.${it::class.simpleName})") }
        }
        val here = highlights.filter { it.moveIndex == idx }
        appendLine(if (here.isEmpty()) "**Highlights at move $idx:** none" else "**Highlights at move $idx:**")
        here.forEach { h ->
            val fn = h.ruleType.split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercase() } } + "Rule.kt"
            appendLine("\n  File: $fn\n  ruleType: ${h.ruleType}\n  severity: ${h.severity}\n  title: ${h.title}")
        }
    }

    private fun fmtCp(cp: Int?) = when {
        cp == null -> "N/A"; cp > 9000 -> "Forced mate (White)"; cp < -9000 -> "Forced mate (Black)"
        else -> "%+.2f".format(cp / 100.0)
    }
}
