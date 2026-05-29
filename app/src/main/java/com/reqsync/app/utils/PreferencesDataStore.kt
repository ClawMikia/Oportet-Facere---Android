package com.reqsync.app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Top-level DataStore extension on Context (singleton). */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reqsync_prefs")

/**
 * Typed wrapper around the app's [DataStore] preferences.
 * Provides flows for each setting and suspend writers.
 */
class PreferencesDataStore(private val context: Context) {

    private object Keys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ONBOARDING_DONE       = booleanPreferencesKey("onboarding_done")
        val LAST_PARSE_SESSION_ID = longPreferencesKey("last_parse_session_id")
        val VIBRATION_ENABLED     = booleanPreferencesKey("vibration_enabled")
    }

    // ── Flows ─────────────────────────────────────────────────────────────────

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    val onboardingDone: Flow<Boolean> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[Keys.ONBOARDING_DONE] ?: false }

    val lastParseSessionId: Flow<Long> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[Keys.LAST_PARSE_SESSION_ID] ?: -1L }

    val vibrationEnabled: Flow<Boolean> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[Keys.VIBRATION_ENABLED] ?: true }

    // ── Writers ───────────────────────────────────────────────────────────────

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setLastParseSessionId(id: Long) {
        context.dataStore.edit { it[Keys.LAST_PARSE_SESSION_ID] = id }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }
}
