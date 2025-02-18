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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.core.view.WindowCompat
import androidx.compose.foundation.isSystemInDarkTheme
import com.lovelive.dreamycolor.database.EncyclopediaDatabase
import com.lovelive.dreamycolor.model.CharacterCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelive.dreamycolor.viewmodel.EncyclopediaViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope


class MainActivity : ComponentActivity() {
    private val settingsManager by lazy { SettingsManager(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by settingsManager.themeModeFlow.collectAsState(initial = SettingsManager.ThemeMode.FOLLOW_SYSTEM)
            val isDarkTheme = when (themeMode) {
                SettingsManager.ThemeMode.LIGHT -> false
                SettingsManager.ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }

            // 动态设置状态栏文字颜色
            LaunchedEffect(isDarkTheme) {
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme
            }

            DreamyColorTheme(
                themeMode = themeMode,
            ) {
                // 状态控制启动页显示
                var showSplash by remember { mutableStateOf(true) }

                // 实现淡入淡出动画：
                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(800)
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen(
                            onTimeout = { showSplash = false },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        MainContent(
                            settingsManager = settingsManager
                        )
                    }
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
// 修改后的 MainContent 函数
@Composable
fun MainContent(settingsManager: SettingsManager) {
    //val navController = rememberNavController() // 注释掉
    val items = listOf(
        Screen.Exclusive,
        Screen.Inspiration,
        Screen.Encyclopedia,
        Screen.Profile
    )
    // 使用 rememberPagerState 来记住页面状态
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) {
        // provide pageCount
        items.size
    }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar {
                //val navBackStackEntry by navController.currentBackStackEntryAsState() // 注释
                //val currentRoute = navBackStackEntry?.destination?.route //注释

                items.forEachIndexed { index, screen ->
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
                        selected = pagerState.currentPage == index,
                        onClick = {

                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // 使用 HorizontalPager 替换 NavHost
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding)
        ) { page ->
            // 根据页面索引显示不同的内容
            when (page) {
                0 -> ExclusiveScreen()
                1 -> InspirationScreen()
                2 -> EncyclopediaScreen()
                3 -> ProfileScreen(settingsManager)
            }
        }
    }
    //    LaunchedEffect(pagerState.currentPage) { //注释掉
    //        // 根据 pagerState.currentPage 更新导航的选中状态
    //        when (pagerState.currentPage) {
    //            0 -> navController.navigate(Screen.Exclusive.route)
    //            1 -> navController.navigate(Screen.Inspiration.route)
    //            2 -> navController.navigate(Screen.Encyclopedia.route)
    //            3 -> navController.navigate(Screen.Profile.route)
    //        }
    //    }
}

@Composable
fun ExclusiveScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
    val context = LocalContext.current
    val database = remember { EncyclopediaDatabase.getDatabase(context) }

    // 使用 viewModel() 函数并传递 factory 参数
    val viewModel: EncyclopediaViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(EncyclopediaViewModel::class.java)) {
                    return EncyclopediaViewModel(database.encyclopediaDao()) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    )

    // 使用 Flow 接收数据
    val character by viewModel.getCharacter("高坂穗乃果").collectAsState(initial = null)

    LaunchedEffect(Unit) {
        viewModel.addTestData() // 初始化测试数据
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        character?.let {
            CharacterCardUI(character = it)
        } ?: Text("加载中...")
    }
}

@Composable
fun CharacterCardUI(character: CharacterCard) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* 点击效果 */ },
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = character.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = character.japaneseName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            androidx.compose.material3.HorizontalDivider(
                color = MaterialTheme.colorScheme.outline,
                thickness = 1.dp
            )
            Text("生日：${character.birthday}")
            Text(character.description)
        }
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