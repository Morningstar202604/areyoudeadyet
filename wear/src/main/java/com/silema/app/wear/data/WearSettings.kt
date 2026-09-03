package com.silema.app.wear.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wear_settings")

/**
 * 手表端设置持久化
 * 使用 DataStore 存储用户设置
 */
class WearSettings(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_HEALTH_TRACKING = booleanPreferencesKey("health_tracking")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_HEART_RATE_WARNING_HIGH = intPreferencesKey("hr_warning_high")
        private val KEY_HEART_RATE_WARNING_LOW = intPreferencesKey("hr_warning_low")
        private val KEY_SPO2_WARNING = intPreferencesKey("spo2_warning")
    }

    private val _notifications = MutableStateFlow(true)
    val notifications: StateFlow<Boolean> = _notifications.asStateFlow()

    private val _darkMode = MutableStateFlow(true)
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _healthTracking = MutableStateFlow(true)
    val healthTracking: StateFlow<Boolean> = _healthTracking.asStateFlow()

    private val _language = MutableStateFlow("zh")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _heartRateWarningHigh = MutableStateFlow(120)
    val heartRateWarningHigh: StateFlow<Int> = _heartRateWarningHigh.asStateFlow()

    private val _heartRateWarningLow = MutableStateFlow(50)
    val heartRateWarningLow: StateFlow<Int> = _heartRateWarningLow.asStateFlow()

    private val _spo2Warning = MutableStateFlow(90)
    val spo2Warning: StateFlow<Int> = _spo2Warning.asStateFlow()

    init {
        scope.launch {
            context.dataStore.data.map { preferences ->
                preferences[KEY_NOTIFICATIONS] ?: true
            }.collect { _notifications.value = it }
        }
        scope.launch {
            context.dataStore.data.map { preferences ->
                preferences[KEY_DARK_MODE] ?: true
            }.collect { _darkMode.value = it }
        }
        scope.launch {
            context.dataStore.data.map { preferences ->
                preferences[KEY_HEALTH_TRACKING] ?: true
            }.collect { _healthTracking.value = it }
        }
        scope.launch {
            context.dataStore.data.map { preferences ->
                preferences[KEY_LANGUAGE] ?: "zh"
            }.collect { _language.value = it }
        }
        scope.launch {
            context.dataStore.data.map { preferences ->
                preferences[KEY_HEART_RATE_WARNING_HIGH] ?: 120
            }.collect { _heartRateWarningHigh.value = it }
        }
        scope.launch {
            context.dataStore.data.map { preferences ->
                preferences[KEY_HEART_RATE_WARNING_LOW] ?: 50
            }.collect { _heartRateWarningLow.value = it }
        }
        scope.launch {
            context.dataStore.data.map { preferences ->
                preferences[KEY_SPO2_WARNING] ?: 90
            }.collect { _spo2Warning.value = it }
        }
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DARK_MODE] = enabled }
    }

    suspend fun setHealthTracking(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HEALTH_TRACKING] = enabled }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = lang }
    }

    suspend fun setHeartRateWarningHigh(value: Int) {
        context.dataStore.edit { it[KEY_HEART_RATE_WARNING_HIGH] = value }
    }

    suspend fun setHeartRateWarningLow(value: Int) {
        context.dataStore.edit { it[KEY_HEART_RATE_WARNING_LOW] = value }
    }

    suspend fun setSpo2Warning(value: Int) {
        context.dataStore.edit { it[KEY_SPO2_WARNING] = value }
    }
}
