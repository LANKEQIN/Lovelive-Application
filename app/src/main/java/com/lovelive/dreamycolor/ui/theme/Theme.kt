package com.lovelive.dreamycolor.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.lovelive.dreamycolor.SettingsManager
import androidx.compose.runtime.remember


private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun DreamyColorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: SettingsManager.ThemeMode = SettingsManager.ThemeMode.FOLLOW_SYSTEM,
    textSize: SettingsManager.TextSize,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        SettingsManager.ThemeMode.LIGHT -> false
        SettingsManager.ThemeMode.DARK -> true
        else -> darkTheme // 跟随系统
    }

    // 字体缩放计算（新增）
    val textScaleRatio = remember(textSize) {
        when (textSize) {
            SettingsManager.TextSize.FOLLOW_SYSTEM -> 1.0f
            SettingsManager.TextSize.SMALL -> 0.85f
            SettingsManager.TextSize.MEDIUM -> 1.0f
            SettingsManager.TextSize.LARGE -> 1.15f
        }
    }

    val scaledTypography = remember(textScaleRatio) {
        Typography.scaleStyle(textScaleRatio) // 这需要第三步实现
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography,
        content = content
    )
}
















