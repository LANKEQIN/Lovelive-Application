package com.lovelive.dreamycolor

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        private val THEME_MODE = intPreferencesKey("theme_mode")
    }

    // 模式定义
    enum class ThemeMode(val value: Int) {
        FOLLOW_SYSTEM(0),
        LIGHT(1),
        DARK(2)
    }

    // 获取当前主题模式
    val themeModeFlow = context.dataStore.data.map { preferences ->
        val modeValue = preferences[THEME_MODE] ?: 0
        ThemeMode.entries.getOrNull(modeValue) ?: ThemeMode.FOLLOW_SYSTEM
    }

    // 保存主题模式
    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { settings ->
            settings[THEME_MODE] = mode.value
        }
    }
}
