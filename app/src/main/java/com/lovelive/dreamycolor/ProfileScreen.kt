package com.lovelive.dreamycolor

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.window.DialogProperties


/**
 * 个人设置界面
 * 
 * 该界面包含以下功能：
 * - 主题模式设置（跟随系统/浅色/深色）
 * - 主题颜色设置（Material You/自定义颜色）
 * - 文字大小设置（跟随系统/小号/中号/大号）
 * - 拼音显示开关
 * - 版本信息显示（含隐藏功能）
 * 
 * @param settingsManager 设置管理器，用于管理和持久化用户设置
 */
@Composable
fun ProfileScreen(settingsManager: SettingsManager) {
    // 状态管理 - 使用rememberSaveable保持配置变更
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showTextSizeDialog by rememberSaveable { mutableStateOf(false) }
    var showDisclaimer by rememberSaveable { mutableStateOf(false) }
    var remainingTime by remember { mutableIntStateOf(7) }
    var showDarkRealmSnackbar by remember { mutableStateOf(false) }

    // 状态收集
    val themeMode by settingsManager.themeModeFlow.collectAsState(
        initial = SettingsManager.ThemeMode.FOLLOW_SYSTEM
    )
    val textSize by settingsManager.textSizeFlow.collectAsState(
        initial = SettingsManager.TextSize.FOLLOW_SYSTEM
    )
    val showPinyin by settingsManager.showPinyinFlow.collectAsState(initial = false)

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // 主布局：使用垂直滚动以适应小屏幕
    Box(modifier = Modifier.fillMaxSize()) {
        // 主内容区：使用垂直滚动
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // 版本号条目（点击后满足条件触发免责声明）
            VersionEntry(
                versionName = getVersionName(context),
                onSecretActivated = { showDisclaimer = true }
            )

            // 文字大小设置
            TextSizeSettingCard(
                currentSize = textSize,
                onClick = { showTextSizeDialog = true }
            )

            // 主题设置长条
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showThemeDialog = true },
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "主题模式",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when (themeMode) {
                                SettingsManager.ThemeMode.FOLLOW_SYSTEM -> "跟随系统"
                                SettingsManager.ThemeMode.LIGHT -> "浅色"
                                SettingsManager.ThemeMode.DARK -> "深色"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "箭头",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }


            // 主题颜色设置卡片
            val colorTheme by settingsManager.colorThemeFlow.collectAsState(initial = SettingsManager.ColorTheme.MATERIAL_YOU)
            var showColorThemeDialog by rememberSaveable { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showColorThemeDialog = true },
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "主题颜色",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when (colorTheme) {
                                SettingsManager.ColorTheme.MATERIAL_YOU -> "Material You"
                                SettingsManager.ColorTheme.PURPLE -> "奇迹紫"
                                SettingsManager.ColorTheme.ORANGE -> "奇迹橙"
                                SettingsManager.ColorTheme.DEEP_BLUE -> "深蓝"
                                SettingsManager.ColorTheme.LIGHT_BLUE -> "浅蓝"
                                SettingsManager.ColorTheme.ROSE -> "丹红"
                                SettingsManager.ColorTheme.YELLOW -> "亮黄"
                                SettingsManager.ColorTheme.PINK -> "轻粉"
                                SettingsManager.ColorTheme.GREEN -> "天绿"
                                SettingsManager.ColorTheme.WHITE -> "霜灰"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "箭头",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // 拼音设置卡片 - 独立卡片，不再嵌套在主题卡片内
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        coroutineScope.launch {
                            settingsManager.setShowPinyin(!showPinyin)
                        }
                    },
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "百科卡片显示拼音及罗马音",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Switch(
                        checked = showPinyin,
                        onCheckedChange = {
                            coroutineScope.launch {
                                settingsManager.setShowPinyin(it)
                            }
                        }
                    )
                }
            }

            if (showColorThemeDialog) {
                AlertDialog(
                    onDismissRequest = { showColorThemeDialog = false },
                    title = {
                        Text(
                            text = "选择主题颜色",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.selectableGroup(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 自定义顺序列表
                            val customOrder = listOf(
                                SettingsManager.ColorTheme.MATERIAL_YOU,
                                SettingsManager.ColorTheme.PURPLE,
                                SettingsManager.ColorTheme.ORANGE,
                                SettingsManager.ColorTheme.DEEP_BLUE,
                                SettingsManager.ColorTheme.LIGHT_BLUE,
                                SettingsManager.ColorTheme.ROSE,
                                SettingsManager.ColorTheme.YELLOW,
                                SettingsManager.ColorTheme.PINK,
                                SettingsManager.ColorTheme.GREEN,
                                SettingsManager.ColorTheme.WHITE,
                            )

                            customOrder.forEach { theme ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = colorTheme == theme,
                                            onClick = {
                                                coroutineScope.launch {
                                                    settingsManager.setColorTheme(theme)
                                                    showColorThemeDialog = false
                                                }
                                            },
                                            role = Role.RadioButton
                                        )
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = colorTheme == theme,
                                        onClick = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when (theme) {
                                            SettingsManager.ColorTheme.MATERIAL_YOU -> "Material You"
                                            SettingsManager.ColorTheme.PURPLE -> "奇迹紫"
                                            SettingsManager.ColorTheme.ORANGE -> "奇迹橙"
                                            SettingsManager.ColorTheme.DEEP_BLUE -> "深蓝"
                                            SettingsManager.ColorTheme.LIGHT_BLUE -> "浅蓝"
                                            SettingsManager.ColorTheme.ROSE -> "丹红"
                                            SettingsManager.ColorTheme.YELLOW -> "亮黄"
                                            SettingsManager.ColorTheme.PINK -> "轻粉"
                                            SettingsManager.ColorTheme.GREEN -> "天绿"
                                            SettingsManager.ColorTheme.WHITE -> "霜灰"
                                        }
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showColorThemeDialog = false }) {
                            Text("关闭")
                        }
                    }
                )
            }
        }

        // Snackbar主机
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // 文字大小对话框
    if (showTextSizeDialog) {
        TextSizeSelectionDialog(
            currentSize = textSize,
            onDismiss = { showTextSizeDialog = false },
            onSizeSelected = { size ->
                coroutineScope.launch {
                    settingsManager.setTextSize(size)
                }
                showTextSizeDialog = false
            }
        )
    }

    // 主题选择对话框
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentMode = themeMode,
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { mode ->
                coroutineScope.launch {
                    settingsManager.setThemeMode(mode)
                }
            }
        )
    }

    // 免责声明对话框
    if (showDisclaimer) {
        DisclaimerDialog(
            remainingTime = remainingTime,
            onConfirm = {
                coroutineScope.launch {
                    settingsManager.setShowCoefficient(true)
                }
                showDisclaimer = false
                showDarkRealmSnackbar = true
            },
            onDismiss = {
                showDisclaimer = false
                remainingTime = 7 // 重置倒计时
            }
        ) { newTime ->
            remainingTime = newTime
        }
    }

    // 显示提示"您已进入黑暗领域"
    if (showDarkRealmSnackbar) {
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar("您已进入黑暗领域")
            showDarkRealmSnackbar = false
        }
    }
}


/**
 * 文字大小设置卡片组件
 * 
 * 显示当前文字大小设置，点击后弹出选择对话框
 * 
 * @param currentSize 当前选择的文字大小
 * @param onClick 点击卡片时的回调
 */
@Composable
private fun TextSizeSettingCard(
    currentSize: SettingsManager.TextSize,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "文字大小",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when (currentSize) {
                        SettingsManager.TextSize.FOLLOW_SYSTEM -> "跟随系统"
                        SettingsManager.TextSize.SMALL -> "小号"
                        SettingsManager.TextSize.MEDIUM -> "中号"
                        SettingsManager.TextSize.LARGE -> "大号"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "箭头",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * 文字大小选择对话框
 * 
 * 提供文字大小的选择选项：
 * - 跟随系统（默认）
 * - 小号（更紧凑）
 * - 中号（推荐）
 * - 大号（更易读）
 * 
 * @param currentSize 当前选择的文字大小
 * @param onDismiss 关闭对话框的回调
 * @param onSizeSelected 选择新文字大小时的回调
 */
@Composable
private fun TextSizeSelectionDialog(
    currentSize: SettingsManager.TextSize,
    onDismiss: () -> Unit,
    onSizeSelected: (SettingsManager.TextSize) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择文字大小") },
        text = {
            Column {
                SettingsManager.TextSize.entries.forEach { size ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSizeSelected(size) }
                    ) {
                        RadioButton(
                            selected = size == currentSize,
                            onClick = { onSizeSelected(size) }
                        )
                        Text(
                            text = when (size) {
                                SettingsManager.TextSize.FOLLOW_SYSTEM -> "跟随系统 (默认)"
                                SettingsManager.TextSize.SMALL -> "小号 (更紧凑)"
                                SettingsManager.TextSize.MEDIUM -> "中号 (推荐)"
                                SettingsManager.TextSize.LARGE -> "大号 (更易读)"
                            },
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}



/**
 * 版本信息条目组件
 * 
 * 显示应用当前版本号，包含隐藏功能：
 * 在1秒内连续点击7次可触发免责声明对话框
 * 
 * @param versionName 应用版本号
 * @param onSecretActivated 触发隐藏功能时的回调
 */
@Composable
private fun VersionEntry(
    versionName: String,
    onSecretActivated: () -> Unit
) {
    // 使用remember记住点击状态
    var clickCount by remember { mutableIntStateOf(0) }
    var lastClickTime by remember { mutableLongStateOf(0L) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val now = System.currentTimeMillis()
                // 检测连续点击
                if (now - lastClickTime < 1000) {
                    clickCount++
                    if (clickCount >= 7) {
                        onSecretActivated()
                        clickCount = 0
                    }
                } else {
                    clickCount = 1
                }
                lastClickTime = now
            },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "版本号",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = versionName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * 免责声明对话框
 * 
 * 显示免责声明内容，包含7秒倒计时，
 * 用户必须等待倒计时结束才能确认。
 * 
 * @param remainingTime 剩余等待时间（秒）
 * @param onConfirm 用户确认时的回调
 * @param onDismiss 关闭对话框的回调
 * @param updateTime 更新倒计时的回调
 */
@Composable
private fun DisclaimerDialog(
    remainingTime: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    updateTime: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        // 禁止点击对话框外部和返回键自动 dismiss
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        ),
        title = {
            Text(
                text = "⚠️ 免责声明",
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column {
                Text("你想成为Z87吗？")
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "请仔细阅读条款（剩余 ${remainingTime}s）",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = remainingTime <= 0
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    // 倒计时处理
    LaunchedEffect(Unit) {
        for (i in 7 downTo 1) {
            updateTime(i)
            delay(1000)
        }
        updateTime(0)
    }
}

// 获取版本号的辅助函数
/**
 * 获取应用版本号
 * 
 * @param context 应用上下文
 * @return 应用版本号，获取失败时返回默认值"1.0.0"
 */
private fun getVersionName(context: Context): String {
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }
}

/**
 * 主题模式选择对话框
 * 
 * 提供主题模式的选择选项：
 * - 跟随系统
 * - 浅色模式
 * - 深色模式
 * 
 * @param currentMode 当前选择的主题模式
 * @param onDismiss 关闭对话框的回调
 * @param onThemeSelected 选择新主题模式时的回调
 */
@Composable
fun ThemeSelectionDialog(
    currentMode: SettingsManager.ThemeMode,
    onDismiss: () -> Unit,
    onThemeSelected: (SettingsManager.ThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择主题") },
        text = {
            Column {
                // 使用for循环优化性能
                for (mode in SettingsManager.ThemeMode.entries) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(mode) }
                            .padding(16.dp)
                    ) {
                        RadioButton(
                            selected = mode == currentMode,
                            onClick = { onThemeSelected(mode) }
                        )
                        Text(
                            text = when (mode) {
                                SettingsManager.ThemeMode.FOLLOW_SYSTEM -> "跟随系统"
                                SettingsManager.ThemeMode.LIGHT -> "浅色模式"
                                SettingsManager.ThemeMode.DARK -> "深色模式"
                            },
                            modifier = Modifier.padding(start = 20.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}