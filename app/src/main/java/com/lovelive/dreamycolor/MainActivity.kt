package com.lovelive.dreamycolor

import android.annotation.SuppressLint
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
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.core.view.WindowCompat
import androidx.compose.foundation.isSystemInDarkTheme
import com.lovelive.dreamycolor.database.EncyclopediaDatabase
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
import com.lovelive.dreamycolor.data.repository.EncyclopediaRepository
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import android.app.Application
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.background
import com.lovelive.dreamycolor.model.CharacterCard
import com.lovelive.dreamycolor.model.VoiceActorCard
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import android.content.pm.PackageManager
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import com.lovelive.dreamycolor.utils.copyToClipboard
import androidx.compose.foundation.combinedClickable


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
@OptIn(ExperimentalFoundationApi::class)
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
    val repository = remember { EncyclopediaRepository(database.encyclopediaDao()) }
    val settingsManager = remember { SettingsManager(context) }

    LaunchedEffect(Unit) {
        repository.initializeFromAssets(context)
    }

    val viewModel: EncyclopediaViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EncyclopediaViewModel(
                    context.applicationContext as Application,
                    repository
                ) as T
            }
        }
    )

    val groupedCharacters by viewModel.getCharactersByGroup().collectAsState(initial = emptyMap())
    val groupedVoiceActors by viewModel.getVoiceActorsByGroup().collectAsState(initial = emptyMap())

    var currentDimension by remember { mutableStateOf("角色") }
    val showCoefficient by settingsManager.showCoefficientFlow.collectAsState(initial = false)

    Column(modifier = Modifier.fillMaxSize()) {
        // 标题区（优化后的布局）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween // 改为 Center
        ) {
            Text(
                text = "百科",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f) // 添加权重
            )
            // 右半部分按钮组
            Row(
                modifier = Modifier.weight(2f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
               // 切换按钮容器（居中处理）
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.shapes.medium
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        DimensionButton(
                            text = "角色",
                            selected = currentDimension == "角色",
                            onClick = { currentDimension = "角色" }
                        )
                        DimensionButton(
                            text = "声优",
                            selected = currentDimension == "声优",
                            onClick = { currentDimension = "声优" }
                        )
                    }
                }

                Spacer(modifier = Modifier.padding(horizontal = 8.dp)) // 添加间距

                // 刷新按钮
                Button(
                    onClick = { viewModel.refreshData(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text("刷新数据")
                }
            }
        }

        // 可滚动内容区
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .weight(1f)
        ) {
            if (currentDimension == "角色") {
                groupedCharacters.forEach { (groupName, characters) ->
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = groupName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        RegularVerticalGrid(items = characters) { character ->
                            CharacterCardUI(character = character)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else {
                groupedVoiceActors.forEach { (groupName, voiceActors) ->
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = groupName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        RegularVerticalGrid(items = voiceActors) { actor ->
                            VoiceActorCardUI(
                                voiceActor = actor,
                                showCoefficient = showCoefficient
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> RegularVerticalGrid(
    items: List<T>,
    columnCount: Int = 2,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        items.chunked(columnCount).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 16.dp)
                    ) {
                        content(item)
                    }
                }
                // 补充空位
                if (rowItems.size < columnCount) {
                    repeat(columnCount - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DimensionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
                MaterialTheme.shapes.small
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VoiceActorCardUI(
    voiceActor: VoiceActorCard,
    showCoefficient: Boolean  // 新增的参数，表示是否显示 QJZ 系数
) {
    val context = LocalContext.current // 添加这行来获取 context
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(285.dp)
            .clickable { /* 点击处理 */ },
        elevation = CardDefaults.cardElevation(8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 姓名部分
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = { /* 普通点击不做任何事 */ },
                            onLongClick = {
                                context.copyToClipboard("${voiceActor.name}\n${voiceActor.japaneseName}")
                            }
                        )
                ) {
                Text(
                    text = voiceActor.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = voiceActor.japaneseName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
                if (showCoefficient) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = voiceActor.coefficient,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }


            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 信息网格
            GridLayout(
                listOf(
                    "生日" to voiceActor.birthday,
                    "事务所" to voiceActor.agency,
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 描述
            Text(
                text = voiceActor.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CharacterCardUI(character: CharacterCard) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(285.dp)
            .clickable { /* 点击进入详情 */ },
        elevation = CardDefaults.cardElevation(8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 姓名部分
            Column(
                modifier = Modifier
                    .combinedClickable(
                        onClick = { /* 普通点击不做任何事 */ },
                        onLongClick = {
                            context.copyToClipboard("${character.name}\n${character.japaneseName}")
                        }
                    )
            ) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = character.japaneseName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 信息网格
            GridLayout(
                listOf(
                    "生日" to character.birthday,
                    "年级" to character.schoolYear,
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 角色描述
            Text(
                text = character.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun GridLayout(items: List<Pair<String, String>>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for ((label, value) in items) {
            InfoItem(
                label = label,
                value = value,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .height(55.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.small
            )
            .padding(8.dp)
            // 添加长按手势
            .combinedClickable(
                onClick = { /* 普通点击不做处理 */ },
                onLongClick = {
                    context.copyToClipboard(value)
                }
            )
    ) {
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}



@Composable
fun ProfileScreen(settingsManager: SettingsManager) {
// 状态管理
    var showThemeDialog by remember { mutableStateOf(false) }
    val themeMode by settingsManager.themeModeFlow.collectAsState(
        initial = SettingsManager.ThemeMode.FOLLOW_SYSTEM
    )
    // 免责声明/隐藏功能相关状态：
    var showDisclaimer by remember { mutableStateOf(false) }
    var remainingTime by remember { mutableStateOf(7) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // 新增：用于显示底部 Snackbar 提示的状态及 HostState
    var showDarkRealmSnackbar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // 主布局：使用垂直滚动以适应小屏幕
    Box(modifier = Modifier.fillMaxSize()) {
        // 主内容区：使用垂直滚动
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            // 版本号条目（点击后满足条件触发免责声明）
            VersionEntry(
                versionName = getVersionName(context),
                onSecretActivated = { showDisclaimer = true }
            )
            Spacer(modifier = Modifier.height(16.dp))
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
                    horizontalArrangement = Arrangement.SpaceBetween
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
        }
        // 将 SnackbarHost 放在 Box 的底部中间
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
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
    // 免责声明对话框，当连续点击版本号后弹出
    if (showDisclaimer) {
        DisclaimerDialog(
            remainingTime = remainingTime,
            onConfirm = {
                coroutineScope.launch {
                    settingsManager.setShowCoefficient(true)
                }
                showDisclaimer = false
                // 点击确认后设置状态以显示提示
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
    // 显示提示“您已进入黑暗领域”
    if (showDarkRealmSnackbar) {
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar("您已进入黑暗领域")
            showDarkRealmSnackbar = false
        }
    }
}

@Composable
private fun VersionEntry(
    versionName: String,
    onSecretActivated: () -> Unit
) {
    var clickCount by remember { mutableStateOf(0) }
    var lastClickTime by remember { mutableStateOf(0L) }

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
        repeat(7) {
            delay(1000)
            updateTime(7 - it - 1)
        }
    }
}

private fun getVersionName(context: Context): String {
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
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
                // 用 for 循环替代 forEach
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
