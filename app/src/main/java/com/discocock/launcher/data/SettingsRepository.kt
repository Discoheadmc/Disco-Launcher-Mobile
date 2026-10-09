package com.discocock.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "disco_settings")

class SettingsRepository(private val context: Context) {

    private val accentKey = stringPreferencesKey("accent_color")
    private val launchButtonKey = stringPreferencesKey("launch_button_color")
    private val msClientIdKey = stringPreferencesKey("ms_client_id")

    /** Key of the 8 Prism-minimal accent choices (see ui.theme.AccentChoices). */
    val accentColor: Flow<String> = context.settingsDataStore.data.map { it[accentKey] ?: "indigo" }

    /**
     * Custom "Başlat" button color (#RRGGBB or empty), mode-independent —
     * mirrors the desktop launcher's dedicated launch-button color.
     */
    val launchButtonColor: Flow<String> = context.settingsDataStore.data.map { it[launchButtonKey] ?: "" }

    /** Microsoft OAuth client id used by the device-code login flow. */
    val microsoftClientId: Flow<String> =
        context.settingsDataStore.data.map { it[msClientIdKey] ?: DEFAULT_MS_CLIENT_ID }

    suspend fun setAccentColor(value: String) {
        context.settingsDataStore.edit { it[accentKey] = value }
    }

    suspend fun setLaunchButtonColor(value: String) {
        context.settingsDataStore.edit { it[launchButtonKey] = value }
    }

    suspend fun setMicrosoftClientId(value: String) {
        context.settingsDataStore.edit { it[msClientIdKey] = value.trim() }
    }

    companion object {
        /**
         * Baked-in default Microsoft client id. The desktop Disco Launcher gets
         * its id from its own infra; here it can be pasted in Settings (free
         * Azure app with Xbox Live enabled) or set at build time.
         */
        const val DEFAULT_MS_CLIENT_ID = ""
    }
}
