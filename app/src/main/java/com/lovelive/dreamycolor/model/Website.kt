package com.lovelive.dreamycolor.model

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 表示网站或内部功能入口的数据类
 */
data class Website(
    val title: String,
    val url: String,
    val icon: ImageVector
)