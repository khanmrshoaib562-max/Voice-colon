package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "voice_finder_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val CLAP_ENABLED = booleanPreferencesKey("clap_enabled")
        val VOICE_PHRASE = stringPreferencesKey("voice_phrase")
        val CLAP_SENSITIVITY = stringPreferencesKey("clap_sensitivity")
        val CLAP_COUNT = intPreferencesKey("clap_count")
        val CLAP_COOLDOWN = intPreferencesKey("clap_cooldown")
        val ALARM_VOLUME = floatPreferencesKey("alarm_volume")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val FLASHLIGHT_ENABLED = booleanPreferencesKey("flashlight_enabled")
        val ALARM_SOUND_TYPE = stringPreferencesKey("alarm_sound_type")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_PIN = stringPreferencesKey("app_pin")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val AUTO_START_ON_BOOT = booleanPreferencesKey("auto_start_on_boot")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val voiceEnabled = preferences[PreferencesKeys.VOICE_ENABLED] ?: false
        val clapEnabled = preferences[PreferencesKeys.CLAP_ENABLED] ?: true
        val voicePhrase = preferences[PreferencesKeys.VOICE_PHRASE] ?: "find my phone"
        val clapSensitivityStr = preferences[PreferencesKeys.CLAP_SENSITIVITY] ?: ClapSensitivity.MEDIUM.name
        val clapSensitivity = try {
            ClapSensitivity.valueOf(clapSensitivityStr)
        } catch (_: Exception) {
            ClapSensitivity.MEDIUM
        }
        val clapCount = preferences[PreferencesKeys.CLAP_COUNT] ?: 2
        val clapCooldown = preferences[PreferencesKeys.CLAP_COOLDOWN] ?: 4
        val alarmVolume = preferences[PreferencesKeys.ALARM_VOLUME] ?: 0.95f
        val vibrationEnabled = preferences[PreferencesKeys.VIBRATION_ENABLED] ?: true
        val flashlightEnabled = preferences[PreferencesKeys.FLASHLIGHT_ENABLED] ?: true
        val soundTypeStr = preferences[PreferencesKeys.ALARM_SOUND_TYPE] ?: AlarmSoundType.SIREN.name
        val soundType = try {
            AlarmSoundType.valueOf(soundTypeStr)
        } catch (_: Exception) {
            AlarmSoundType.SIREN
        }
        val appLockEnabled = preferences[PreferencesKeys.APP_LOCK_ENABLED] ?: false
        val appPin = preferences[PreferencesKeys.APP_PIN] ?: ""
        val biometricEnabled = preferences[PreferencesKeys.BIOMETRIC_ENABLED] ?: false
        val autoStartBoot = preferences[PreferencesKeys.AUTO_START_ON_BOOT] ?: false

        AppSettings(
            voiceEnabled = voiceEnabled,
            clapEnabled = clapEnabled,
            voicePhrase = voicePhrase,
            clapSensitivity = clapSensitivity,
            clapCount = clapCount,
            clapCooldownSeconds = clapCooldown,
            alarmVolume = alarmVolume,
            vibrationEnabled = vibrationEnabled,
            flashlightEnabled = flashlightEnabled,
            alarmSoundType = soundType,
            appLockEnabled = appLockEnabled,
            appPin = appPin,
            biometricEnabled = biometricEnabled,
            autoStartOnBoot = autoStartBoot
        )
    }

    suspend fun setVoiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.VOICE_ENABLED] = enabled }
    }

    suspend fun setClapEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.CLAP_ENABLED] = enabled }
    }

    suspend fun setVoicePhrase(phrase: String) {
        context.dataStore.edit { it[PreferencesKeys.VOICE_PHRASE] = phrase.trim().lowercase() }
    }

    suspend fun setClapSensitivity(sensitivity: ClapSensitivity) {
        context.dataStore.edit { it[PreferencesKeys.CLAP_SENSITIVITY] = sensitivity.name }
    }

    suspend fun setClapCount(count: Int) {
        context.dataStore.edit { it[PreferencesKeys.CLAP_COUNT] = count }
    }

    suspend fun setClapCooldown(seconds: Int) {
        context.dataStore.edit { it[PreferencesKeys.CLAP_COOLDOWN] = seconds }
    }

    suspend fun setAlarmVolume(volume: Float) {
        context.dataStore.edit { it[PreferencesKeys.ALARM_VOLUME] = volume }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setFlashlightEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.FLASHLIGHT_ENABLED] = enabled }
    }

    suspend fun setAlarmSoundType(type: AlarmSoundType) {
        context.dataStore.edit { it[PreferencesKeys.ALARM_SOUND_TYPE] = type.name }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setAppPin(pin: String) {
        context.dataStore.edit { it[PreferencesKeys.APP_PIN] = pin }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setAutoStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_START_ON_BOOT] = enabled }
    }
}
