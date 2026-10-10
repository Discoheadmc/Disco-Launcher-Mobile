package com.discocock.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "disco_settings")

class SettingsRepository(private val context: Context) {

    private val accentKey = stringPreferencesKey("accent_color")
    private val launchButtonKey = stringPreferencesKey("launch_button_color")
    private val msClientIdKey = stringPreferencesKey("ms_client_id")
    private val memoryKey = intPreferencesKey("default_memory_mb")
    private val jvmArgsKey = stringPreferencesKey("extra_jvm_args")
    private val preferredRuntimeKey = stringPreferencesKey("preferred_runtime")

    /** Key of the 8 Prism-minimal accent choices (see ui.theme.AccentChoices). */
    val accentColor: Flow<String> = context.settingsDataStore.data.map { it[accentKey] ?: "indigo" }

    /** Custom "Başlat" button color (#RRGGBB or empty), mirrors the desktop launcher. */
    val launchButtonColor: Flow<String> = context.settingsDataStore.data.map { it[launchButtonKey] ?: "" }

    /** Microsoft OAuth client id used by the device-code login flow. */
    val microsoftClientId: Flow<String> =
        context.settingsDataStore.data.map { it[msClientIdKey] ?: DEFAULT_MS_CLIENT_ID }

    /** Default Java heap (-Xmx) for newly created instances, in MB. */
    val defaultMemoryMb: Flow<Int> = context.settingsDataStore.data.map { it[memoryKey] ?: 2048 }

    /** Extra JVM args appended to every launch, whitespace separated. */
    val extraJvmArgs: Flow<String> = context.settingsDataStore.data.map { it[jvmArgsKey] ?: "" }

    /** Preferred runtime name; blank = auto (first installed / device recommended). */
    val preferredRuntime: Flow<String> = context.settingsDataStore.data.map { it[preferredRuntimeKey] ?: "" }

    suspend fun setAccentColor(value: String) {
        context.settingsDataStore.edit { it[accentKey] = value }
    }

    suspend fun setLaunchButtonColor(value: String) {
        context.settingsDataStore.edit { it[launchButtonKey] = value }
    }

    suspend fun setMicrosoftClientId(value: String) {
        context.settingsDataStore.edit { it[msClientIdKey] = value.trim() }
    }

    suspend fun setDefaultMemoryMb(value: Int) {
        context.settingsDataStore.edit { it[memoryKey] = value }
    }

    suspend fun setExtraJvmArgs(value: String) {
        context.settingsDataStore.edit { it[jvmArgsKey] = value.trim() }
    }

    suspend fun setPreferredRuntime(value: String) {
        context.settingsDataStore.edit { it[preferredRuntimeKey] = value.trim() }
    }

    companion object {
        /**
         * Baked-in default Microsoft client id; paste one in Settings (free
         * Azure app with Xbox Live) to enable Microsoft accounts.
         */
        const val DEFAULT_MS_CLIENT_ID = ""
    }
}
