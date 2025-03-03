package com.lovelive.dreamycolor

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.booleanPreferencesKey

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        private val THEME_MODE = intPreferencesKey("theme_mode")
        private val SHOW_COEFFICIENT = booleanPreferencesKey("show_coefficient")
        private val TEXT_SIZE = intPreferencesKey("text_size")
        // 新增拼音显示设置键
        private val SHOW_PINYIN = booleanPreferencesKey("show_pinyin")
    }

    // 新增：获取系数显示状态
    val showCoefficientFlow = context.dataStore.data.map { preferences ->
        preferences[SHOW_COEFFICIENT] ?: false
    }

    // 添加拼音显示状态流
    val showPinyinFlow = context.dataStore.data.map { preferences ->
        preferences[SHOW_PINYIN] ?: false
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

    // 添加设置拼音显示状态的方法
    suspend fun setShowPinyin(show: Boolean) {
        context.dataStore.edit { settings ->
            settings[SHOW_PINYIN] = show
        }
    }
    suspend fun setShowCoefficient(show: Boolean) {
        context.dataStore.edit { settings ->
            settings[SHOW_COEFFICIENT] = show
        }
    }


    // 文本大小枚举定义（新增）
    enum class TextSize(val value: Int) {
        FOLLOW_SYSTEM(0),
        SMALL(1),
        MEDIUM(2),
        LARGE(3);

        companion object {
            fun from(value: Int): TextSize {
                return entries.firstOrNull { it.value == value } ?: FOLLOW_SYSTEM
            }
        }
    }

    // 文本大小状态流（新增）
    val textSizeFlow = context.dataStore.data.map { preferences ->
        val sizeValue = preferences[TEXT_SIZE] ?: 0
        TextSize.from(sizeValue)
    }

    // 保存文本大小方法（新增）
    suspend fun setTextSize(size: TextSize) {
        context.dataStore.edit { settings ->
            settings[TEXT_SIZE] = size.value
        }
    }

}
