package com.acepero13.android.gamereviewer.domain

import com.acepero13.android.gamereviewer.data.model.CriticalMoment
import com.acepero13.chess.core.middlegame.MiddlegamePlan

object InsightReconciler {

    data class Insight(
        val emoji: String,
        val title: String,
        val description: String,
        val questions: List<String>,
        val conceptualHint: String,
    )

    private val fallback = Insight(
        emoji         = "🤔",
        title         = "Think it through",
        description   = "Take a moment to assess the position.",
        questions     = emptyList(),
        conceptualHint = "",
    )

    fun forReason(category: CriticalMoment.ReasonCategory): Insight =
        InsightDatabase.reasonInsights[category] ?: fallback

    fun forTrigger(trigger: CoachingTrigger): Insight = when (trigger) {
        is CoachingTrigger.RookActivation     -> InsightDatabase.rookActivationInsight(trigger.rookSquare)
        is CoachingTrigger.ImpulseControl     -> InsightDatabase.impulseControlInsight(trigger.timeSpentSeconds)
        is CoachingTrigger.CalculationBlunder -> InsightDatabase.calculationBlunderInsight(trigger.timeSpentSeconds)
        is CoachingTrigger.TacticalOversight  -> InsightDatabase.tacticalOversightInsight(trigger.timeSpentSeconds)
        is CoachingTrigger.CandidateSearch    -> InsightDatabase.candidateSearchInsight(trigger.evalCp)
        is CoachingTrigger.ConversionStrategy -> InsightDatabase.conversionStrategyInsight(trigger.evaluationCp)
        is CoachingTrigger.CoordinatedAttack  -> InsightDatabase.coordinatedAttackInsight(
            trigger.isPlayerSide, trigger.isLoss, trigger.pieceCount,
        )
        is CoachingTrigger.PieceHarmony       -> InsightDatabase.pieceHarmonyInsight(
            trigger.isPlayerSide, trigger.isLoss,
        )
        is CoachingTrigger.EvalCalibration    -> InsightDatabase.evalCalibrationInsight(
            trigger.title(), trigger.coachingQuestion(),
        )
        else -> InsightDatabase.staticTriggerInsights[trigger::class.simpleName] ?: fallback
    }

    fun forBlunder(motif: String, cpLoss: Int): Insight =
        InsightDatabase.blunderInsights[motif] ?: InsightDatabase.blunderElseInsight(cpLoss)

    fun forEndgame(chapter: Int, name: String): Insight {
        val base = forReason(CriticalMoment.ReasonCategory.ENDGAME_PRINCIPLE)
        return base.copy(conceptualHint = "Chapter $chapter — $name\n\n${base.conceptualHint}")
    }

    fun forMiddlegamePlan(plan: MiddlegamePlan): Insight =
        InsightDatabase.middlegamePlanInsight(plan)
}
