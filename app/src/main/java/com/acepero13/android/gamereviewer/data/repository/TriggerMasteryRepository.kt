package com.acepero13.android.gamereviewer.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.acepero13.android.gamereviewer.domain.CoachingTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.masteryDataStore by preferencesDataStore(name = "trigger_mastery")

/**
 * Persists per-trigger "mastery" state using DataStore.
 *
 * ## Mastery model
 * Each trigger type has a **correct-answer streak** (consecutive correct identifications
 * in Reflection Mode). When `streak >= MASTERY_THRESHOLD` the type is considered mastered
 * and the Coach Lamp stops lighting up for it — the user is expected to notice it themselves.
 *
 * A wrong answer resets the streak to 0, immediately un-mastering the type so the lamp
 * returns until the habit is rebuilt.
 */
class TriggerMasteryRepository(private val context: Context) {

    companion object {
        const val MASTERY_THRESHOLD = 5

        private fun streakKey(typeName: String) = intPreferencesKey("streak_$typeName")
    }

    // ── Read API ───────────────────────────────────────────────────────────────

    /** Emits a map of typeName → streak count for all known trigger types. */
    val streaks: Flow<Map<String, Int>> = context.masteryDataStore.data.map { prefs ->
        CoachingTrigger.ALL_LABELS.associateWith { label ->
            val typeName = labelToTypeName(label)
            prefs[streakKey(typeName)] ?: 0
        }
    }

    /** Emits the set of trigger type names that are currently mastered. */
    val masteredTypes: Flow<Set<String>> = context.masteryDataStore.data.map { prefs ->
        CoachingTrigger.ALL_LABELS.mapNotNull { label ->
            val typeName = labelToTypeName(label)
            val streak   = prefs[streakKey(typeName)] ?: 0
            if (streak >= MASTERY_THRESHOLD) typeName else null
        }.toSet()
    }

    /** Snapshot read — used by AnalysisViewModel during trigger filtering. */
    suspend fun getMasteredTypes(): Set<String> = masteredTypes.first()

    // ── Write API ──────────────────────────────────────────────────────────────

    /** Records a correct identification. Increments the streak toward mastery. */
    suspend fun recordCorrect(typeName: String) {
        context.masteryDataStore.edit { prefs ->
            val current = prefs[streakKey(typeName)] ?: 0
            prefs[streakKey(typeName)] = current + 1
        }
    }

    /** Records a wrong identification. Resets streak to 0, removing mastery. */
    suspend fun recordIncorrect(typeName: String) {
        context.masteryDataStore.edit { prefs ->
            prefs[streakKey(typeName)] = 0
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Converts a display label (used in Reflection Mode) back to the type name key. */
    fun labelToTypeName(displayLabel: String): String = when (displayLabel) {
        "Safety Issue"     -> "SAFETY"
        "Multiple Plans"   -> "CANDIDATE_MOVES"
        "Restricted Piece" -> "WORST_PIECE"
        "Forcing Move"     -> "FORCING_MOVE"
        "Opponent's Plan"  -> "OPPONENT_PLAN"
        "Pre-Move Check"   -> "PRE_MOVE_CHECKLIST"
        "Rook Activation"  -> "ROOK_ACTIVATION"
        else               -> displayLabel.uppercase().replace(" ", "_")
    }
}
