package com.lovelive.dreamycolor

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.booleanPreferencesKey

/**
 * 应用设置数据存储实例
 * 
 * 使用DataStore存储应用的设置信息，确保设置的持久化存储
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 应用设置管理器
 *
 * 负责管理和持久化应用的各项设置，包括：
 * - 主题模式（跟随系统/浅色/深色）
 * - 主题颜色（Material You/自定义颜色）
 * - 文本大小（跟随系统/小/中/大）
 * - 拼音显示开关
 * - 系数显示开关
 *
 * @property context 应用上下文，用于访问DataStore
 */
class SettingsManager(private val context: Context) {
    companion object {
        /** 主题模式设置键 */
        private val THEME_MODE = intPreferencesKey("theme_mode")
        /** 系数显示设置键 */
        private val SHOW_COEFFICIENT = booleanPreferencesKey("show_coefficient")
        /** 文本大小设置键 */
        private val TEXT_SIZE = intPreferencesKey("text_size")
        /** 拼音显示设置键 */
        private val SHOW_PINYIN = booleanPreferencesKey("show_pinyin")
        /** 主题颜色设置键 */
        private val COLOR_THEME = intPreferencesKey("color_theme")
    }

    /**
     * 系数显示状态流
     *
     * @return Flow<Boolean> 系数显示状态的数据流，true表示显示，false表示隐藏
     */
    val showCoefficientFlow = context.dataStore.data.map { preferences ->
        preferences[SHOW_COEFFICIENT] == true
    }

    /**
     * 拼音显示状态流
     *
     * @return Flow<Boolean> 拼音显示状态的数据流，true表示显示，false表示隐藏
     */
    val showPinyinFlow = context.dataStore.data.map { preferences ->
        preferences[SHOW_PINYIN] == true
    }
    
    /**
     * 颜色主题枚举
     *
     * 定义应用支持的所有主题颜色选项
     *
     * @property value 主题颜色的整数值，用于持久化存储
     */
    enum class ColorTheme(val value: Int) {
        MATERIAL_YOU(0),
        PURPLE(1),
        ROSE(2),
        LIGHT_BLUE(3),
        ORANGE(4),
        DEEP_BLUE(5),
        YELLOW(6),
        PINK(7),
        GREEN(8),
        WHITE(9);

        companion object {
            fun from(value: Int): ColorTheme {
                return entries.firstOrNull { it.value == value } ?: MATERIAL_YOU
            }
        }
    }

    /**
     * 颜色主题状态流
     *
     * @return Flow<ColorTheme> 当前选择的主题颜色的数据流
     */
    val colorThemeFlow = context.dataStore.data.map { preferences ->
        val themeValue = preferences[COLOR_THEME] ?: 0
        ColorTheme.from(themeValue)
    }

    /**
     * 主题模式枚举
     *
     * 定义应用支持的显示模式
     *
     * @property value 主题模式的整数值，用于持久化存储
     */
    enum class ThemeMode(val value: Int) {
        FOLLOW_SYSTEM(0),
        LIGHT(1),
        DARK(2)
    }

    /**
     * 主题模式状态流
     *
     * @return Flow<ThemeMode> 当前主题模式的数据流
     */
    val themeModeFlow = context.dataStore.data.map { preferences ->
        val modeValue = preferences[THEME_MODE] ?: 0
        ThemeMode.entries.getOrNull(modeValue) ?: ThemeMode.FOLLOW_SYSTEM
    }

    /**
     * 设置主题模式
     *
     * @param mode 要设置的主题模式
     */
    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { settings ->
            settings[THEME_MODE] = mode.value
        }
    }

    /**
     * 设置拼音显示状态
     *
     * @param show true表示显示拼音，false表示隐藏拼音
     */
    suspend fun setShowPinyin(show: Boolean) {
        context.dataStore.edit { settings ->
            settings[SHOW_PINYIN] = show
        }
    }

    /**
     * 设置系数显示状态
     *
     * @param show true表示显示系数，false表示隐藏系数
     */
    suspend fun setShowCoefficient(show: Boolean) {
        context.dataStore.edit { settings ->
            settings[SHOW_COEFFICIENT] = show
        }
    }


    /**
     * 文本大小枚举
     *
     * 定义应用支持的文本大小选项
     *
     * @property value 文本大小的整数值，用于持久化存储
     */
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

    /**
     * 文本大小状态流
     *
     * @return Flow<TextSize> 当前文本大小的数据流
     */
    val textSizeFlow = context.dataStore.data.map { preferences ->
        val sizeValue = preferences[TEXT_SIZE] ?: 0
        TextSize.from(sizeValue)
    }

    /**
     * 设置文本大小
     *
     * @param size 要设置的文本大小
     */
    suspend fun setTextSize(size: TextSize) {
        context.dataStore.edit { settings ->
            settings[TEXT_SIZE] = size.value
        }
    }
    
    /**
     * 设置颜色主题
     *
     * @param theme 要设置的颜色主题
     */
    suspend fun setColorTheme(theme: ColorTheme) {
        context.dataStore.edit { settings ->
            settings[COLOR_THEME] = theme.value
        }
    }

}
