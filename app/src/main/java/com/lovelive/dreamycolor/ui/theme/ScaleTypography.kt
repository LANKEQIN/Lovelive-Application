package com.lovelive.dreamycolor.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

fun TextStyle.scale(factor: Float): TextStyle {
    return this.copy(
        fontSize = (this.fontSize.value * factor).sp
    )
}

fun Typography.scaleStyle(factor: Float): Typography {
    return this.copy(
        displayLarge = displayLarge.scale(factor),
        displayMedium = displayMedium.scale(factor),
        displaySmall = displaySmall.scale(factor),

        headlineLarge = headlineLarge.scale(factor),
        headlineMedium = headlineMedium.scale(factor),
        headlineSmall = headlineSmall.scale(factor),

        titleLarge = titleLarge.scale(factor),
        titleMedium = titleMedium.scale(factor),
        titleSmall = titleSmall.scale(factor),

        bodyLarge = bodyLarge.scale(factor),
        bodyMedium = bodyMedium.scale(factor),
        bodySmall = bodySmall.scale(factor),

        labelLarge = labelLarge.scale(factor),
        labelMedium = labelMedium.scale(factor),
        labelSmall = labelSmall.scale(factor)
    )
}
