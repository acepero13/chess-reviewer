package com.acepero13.android.gamereviewer.domain

import android.util.Log

private const val TAG = "TriggerPrioritizer"

internal object TriggerPrioritizer {

    fun apply(triggers: MutableList<CoachingTrigger>, weakTriggerTypes: Set<String>, pfx: String) {
        val nonConversion = triggers.filter { it !is CoachingTrigger.ConversionStrategy }
        if (nonConversion.isEmpty()) return

        val highestTier = nonConversion.minOf { it.tier() }
        val tierRemoved = nonConversion.filter { it.tier() != highestTier }
        if (tierRemoved.isNotEmpty()) {
            Log.d(TAG, "$pfx TierFilter: removing ${tierRemoved.map { "${it.typeName()}(tier=${it.tier()})" }} keeping tier=$highestTier")
            triggers.removeAll { it !is CoachingTrigger.ConversionStrategy && it.tier() != highestTier }
        }

        val singleBest = triggers.filter { it !is CoachingTrigger.ConversionStrategy }
            .minByOrNull { NarrativeContextBuilder.effectiveSubPriority(it, weakTriggerTypes) }
            ?: return
        val voiceRemoved = triggers.filter { it !is CoachingTrigger.ConversionStrategy && it != singleBest }
        if (voiceRemoved.isNotEmpty()) {
            val note = if (singleBest.typeName() in weakTriggerTypes) " [WEAK-AREA-BOOST]" else ""
            Log.d(TAG, "$pfx SingleVoice: removing ${voiceRemoved.map { it.typeName() }} keeping=${singleBest.typeName()}$note")
            triggers.removeAll { it !is CoachingTrigger.ConversionStrategy && it != singleBest }
        }
    }
}
