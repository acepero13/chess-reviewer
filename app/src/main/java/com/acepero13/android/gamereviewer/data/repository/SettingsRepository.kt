package com.acepero13.android.gamereviewer.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_BOARD_THEME = stringPreferencesKey("board_theme")
        private val KEY_PIECE_STYLE = stringPreferencesKey("piece_style")
        private val KEY_THEME_MODE  = stringPreferencesKey("theme_mode")
        /** The player's own username — used to auto-orient the board. */
        private val KEY_USERNAME    = stringPreferencesKey("username")
        /** When true, a per-position analysis prompt is shown at flagged positions in Navigate mode. */
        private val KEY_POSITION_COACH = booleanPreferencesKey("position_coach_enabled")
        /** When true, a "Copy LLM Prompt" button appears next to active coaching panels. */
        private val KEY_DEVELOPER_MODE = booleanPreferencesKey("developer_mode_enabled")
    }

    val boardTheme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_BOARD_THEME] ?: "Classic"
    }

    val pieceStyle: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PIECE_STYLE] ?: "Classic"
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "dark"
    }

    /** Player's own username — empty means auto-orientation is disabled (always White at bottom). */
    val username: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USERNAME] ?: ""
    }

    /** When true, structured analysis prompts appear at flagged positions in Navigate mode. */
    val positionCoachEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_POSITION_COACH] ?: false
    }

    /** When true, a "Copy LLM Prompt" button appears next to active coaching panels. */
    val developerModeEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEVELOPER_MODE] ?: false
    }

    suspend fun setBoardTheme(theme: String) {
        context.dataStore.edit { prefs -> prefs[KEY_BOARD_THEME] = theme }
    }

    suspend fun setPieceStyle(style: String) {
        context.dataStore.edit { prefs -> prefs[KEY_PIECE_STYLE] = style }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[KEY_THEME_MODE] = mode }
    }

    suspend fun setUsername(name: String) {
        context.dataStore.edit { prefs -> prefs[KEY_USERNAME] = name.trim() }
    }

    suspend fun setPositionCoachEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_POSITION_COACH] = enabled }
    }

    suspend fun setDeveloperModeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_DEVELOPER_MODE] = enabled }
    }
}
