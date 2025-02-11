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
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        SettingsManager.ThemeMode.LIGHT -> false
        SettingsManager.ThemeMode.DARK -> true
        else -> darkTheme // 跟随系统
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
        typography = Typography,
        content = content
    )
}