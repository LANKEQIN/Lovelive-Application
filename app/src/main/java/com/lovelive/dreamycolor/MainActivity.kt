package com.lovelive.dreamycolor

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lovelive.dreamycolor.ui.theme.DreamyColorTheme
import kotlinx.coroutines.delay
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.core.content.pm.PackageInfoCompat


class MainActivity : ComponentActivity() {
    private val settingsManager by lazy { SettingsManager(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsManager.themeModeFlow.collectAsState(initial = SettingsManager.ThemeMode.FOLLOW_SYSTEM)

            DreamyColorTheme(
                themeMode = themeMode,
            ) {
                // 状态控制启动页显示
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(
                        onTimeout = { showSplash = false },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MainContent(settingsManager)
                }
            }
        }
    }
}

// 启动页组件
@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 逐字符竖排显示
            stringResource(R.string.splash_text).forEach { char ->
                Text(
                    text = char.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        // 自动关闭逻辑
        LaunchedEffect(Unit) {
            delay(1500L) // 1.5秒延迟
            onTimeout()  // 触发关闭
        }
    }
}

// 主界面内容
@Composable
fun MainContent(settingsManager: SettingsManager) {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Exclusive,
        Screen.Inspiration,
        Screen.Encyclopedia,
        Screen.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = {}, // 不要图标
                        label = {
                            Text(
                                text = stringResource(id = screen.titleRes),
                                style = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    LocalTextStyle.current.copy(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Exclusive.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Exclusive.route) {
                ExclusiveScreen()
            }
            composable(Screen.Inspiration.route) {
                InspirationScreen()
            }
            composable(Screen.Encyclopedia.route) {
                EncyclopediaScreen()
            }
            composable(Screen.Profile.route) {
                ProfileScreen(settingsManager)
            }
        }
    }
}

@Composable
fun ExclusiveScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 30.sp)
        Text(text = stringResource(R.string.navigation_exclusive), style = textStyle)
    }
}

@Composable
fun InspirationScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 30.sp)
        Text(text = stringResource(R.string.navigation_inspiration), style = textStyle)
    }
}

@Composable
fun EncyclopediaScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 30.sp)
        Text(text = stringResource(R.string.navigation_encyclopedia), style = textStyle)
    }
}

@Composable
fun ProfileScreen(settingsManager: SettingsManager) {
    var showThemeDialog by remember { mutableStateOf(false) }
    val themeMode by settingsManager.themeModeFlow.collectAsState(initial = SettingsManager.ThemeMode.FOLLOW_SYSTEM)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // 添加主题设置按钮
            Button(onClick = { showThemeDialog = true }) {
                Text(
                    text = "主题设置",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 20.sp)
                )
            }
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentMode = themeMode,
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { mode ->
                CoroutineScope(Dispatchers.IO).launch {
                    settingsManager.setThemeMode(mode)
                }
            }
        )
    }
}

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
                // 使用 Enum.entries 替换 Enum.values()
                SettingsManager.ThemeMode.entries.forEach { mode ->
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










