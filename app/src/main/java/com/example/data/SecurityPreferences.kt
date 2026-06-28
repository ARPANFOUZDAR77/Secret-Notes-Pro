package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import kotlinx.coroutines.flow.catch
import java.io.IOException
import androidx.datastore.preferences.core.emptyPreferences

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SecurityPreferences(private val context: Context) {
    
    companion object {
        val HASHED_PIN = stringPreferencesKey("hashed_pin")
        val HASHED_FAKE_PIN = stringPreferencesKey("hashed_fake_pin")
        val IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
        val AUTO_LOCK_TIMEOUT = stringPreferencesKey("auto_lock_timeout") // in milliseconds
        val APP_THEME = stringPreferencesKey("app_theme")
        val SECURITY_QUESTION = stringPreferencesKey("security_question")
        val HASHED_SECURITY_ANSWER = stringPreferencesKey("hashed_security_answer")
        val IS_AUTO_SAVE_ENABLED = booleanPreferencesKey("is_auto_save_enabled")
        val AUTO_SAVE_INTERVAL_SECS = intPreferencesKey("auto_save_interval_secs")
        val IS_BIOMETRIC_SIMULATION_ENABLED = booleanPreferencesKey("is_biometric_simulation_enabled")
    }

    private val safeData = context.dataStore.data.catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }

    val hashedPinFlow: Flow<String?> = safeData.map { it[HASHED_PIN] }
    val hashedFakePinFlow: Flow<String?> = safeData.map { it[HASHED_FAKE_PIN] }
    val isBiometricEnabledFlow: Flow<Boolean> = safeData.map { it[IS_BIOMETRIC_ENABLED] ?: false }
    val isBiometricSimulationEnabledFlow: Flow<Boolean> = safeData.map { it[IS_BIOMETRIC_SIMULATION_ENABLED] ?: false }
    val appThemeFlow: Flow<String> = safeData.map { it[APP_THEME] ?: "Cyber" }
    val securityQuestionFlow: Flow<String?> = safeData.map { it[SECURITY_QUESTION] }
    val hashedSecurityAnswerFlow: Flow<String?> = safeData.map { it[HASHED_SECURITY_ANSWER] }
    val isAutoSaveEnabledFlow: Flow<Boolean> = safeData.map { it[IS_AUTO_SAVE_ENABLED] ?: true }
    val autoSaveIntervalFlow: Flow<Int> = safeData.map { it[AUTO_SAVE_INTERVAL_SECS] ?: 5 }
    
    suspend fun savePin(hashedPin: String) {
        context.dataStore.edit { it[HASHED_PIN] = hashedPin }
    }
    
    suspend fun saveFakePin(hashedPin: String) {
        context.dataStore.edit { it[HASHED_FAKE_PIN] = hashedPin }
    }
    
    suspend fun saveSecurityQuestion(question: String, hashedAnswer: String) {
        context.dataStore.edit { 
            it[SECURITY_QUESTION] = question
            it[HASHED_SECURITY_ANSWER] = hashedAnswer
        }
    }
    
    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setBiometricSimulationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_BIOMETRIC_SIMULATION_ENABLED] = enabled }
    }

    suspend fun setAppTheme(theme: String) {
        context.dataStore.edit { it[APP_THEME] = theme }
    }

    suspend fun setAutoSaveEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_AUTO_SAVE_ENABLED] = enabled }
    }

    suspend fun setAutoSaveInterval(seconds: Int) {
        context.dataStore.edit { it[AUTO_SAVE_INTERVAL_SECS] = seconds }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
