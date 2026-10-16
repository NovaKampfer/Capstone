package com.MatthewBaerg.multitasked.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.MatthewBaerg.multitasked.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val SHOW_CELEBRATION_KEY = booleanPreferencesKey("show_celebration")
        val APP_THEME_KEY = stringPreferencesKey("app_theme")
    }

    val showCelebration: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[SHOW_CELEBRATION_KEY] ?: true // Default to true
        }

    suspend fun setShowCelebration(show: Boolean) {
        dataStore.edit {
            it[SHOW_CELEBRATION_KEY] = show
        }
    }

    val theme: Flow<AppTheme> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                // Default to system theme on error
                emit(emptyPreferences()) // an empty preferences is fine, the map below will convert it to AppTheme.SYSTEM
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeName = preferences[APP_THEME_KEY] ?: AppTheme.SYSTEM.name
            try {
                AppTheme.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                // Default to system theme if the saved value is invalid
                AppTheme.SYSTEM
            }
        }

    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit {
            it[APP_THEME_KEY] = theme.name
        }
    }
}
