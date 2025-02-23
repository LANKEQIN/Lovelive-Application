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
import android.content.Context
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.foundation.layout.PaddingValues
import com.lovelive.dreamycolor.utils.copyToClipboard
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.FloatingActionButton



class MainActivity : ComponentActivity() {
    private val settingsManager by lazy { SettingsManager(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by settingsManager.themeModeFlow.collectAsState(initial = SettingsManager.ThemeMode.FOLLOW_SYSTEM)
            val textSize by settingsManager.textSizeFlow.collectAsState(
                initial = SettingsManager.TextSize.FOLLOW_SYSTEM // 添加初始值
            )
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
                textSize = textSize
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


data class Website(
    val title: String,
    val url: String,
    val icon: ImageVector
)
// 新增音乐MV数据类
data class MusicVideo(
    val id: String,
    val title: String,
    val description: String,
    val videoUrl: String = "",       // 后期替换实际链接
    val coverPlaceholder: ImageVector = Icons.Default.MusicNote // 占位图标
)
// 配置数据
private val musicMagazineData = listOf(
    MusicVideo(
        id = "mv1",
        title = "始まりは君の空",
        description = """
            |这是Liella!的第一首单曲的MV
        """.trimMargin()
    )
)


@Composable
private fun WebsiteCard(
    website: Website,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = website.icon,
                contentDescription = website.title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = website.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
@Composable
private fun WebsiteGrid(
    websites: List<Website>,
    onWebsiteClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(websites) { website ->
            WebsiteCard(
                website = website,
                onClick = { onWebsiteClick(website.url) }
            )
        }
    }
}

@Composable
fun InspirationScreen() {

    var showPlanetariumDialog by remember { mutableStateOf(false) }

    val websites = listOf(
        Website(
            title = "缪斯时光蛋",
            url = "https://www.llhistoy.lionfree.net/lovelive.ws/index.html",
            icon = Icons.Filled.HistoryEdu
        ),
        Website(
            title = "Aqours许愿瓶",
            url = "https://aqours.tv/",
            icon = Icons.Filled.WaterDrop
        ),
        Website(
            title = "虹之咲活动室",
            url = "https://nijigaku.club/",
            icon = Icons.Filled.Group
        ),
        Website(
            title = "Liella星象馆",
            url = "dialog://liella",
            icon = Icons.Filled.Star
        )
    )

    var selectedMV by remember { mutableStateOf<MusicVideo?>(null) }
    var currentScreen by remember { mutableStateOf<String?>(null) }

    Crossfade(
        targetState = currentScreen,
        animationSpec = tween(300)
    ) { screen ->
        when (screen) {
            // 默认网格界面
            null -> {
                WebsiteGrid(
                    websites = websites,
                    onWebsiteClick = { url ->
                        if (url == "dialog://liella") {
                            showPlanetariumDialog = true
                        } else if (url.startsWith("internal://")) {
                            currentScreen = url
                        } else {
                            currentScreen = "webview:$url"
                        }
                    }
                )
            }
            // 音乐杂志栏
            "internal://music_magazine" -> {
                MusicMagazineScreen(
                    onBack = { currentScreen = null },
                )
            }
            // 网页浏览（保留原有功能）
            else -> {
                WebViewScreen(
                    url = screen.removePrefix("webview:"),
                    onClose = { currentScreen = null }
                )
            }
        }
    }
    if (showPlanetariumDialog) {
        AlertDialog(
            onDismissRequest = { showPlanetariumDialog = false },
            title = { Text("进入星象馆") },
            text = { Text("请选择您要进入的版本：") },
            confirmButton = {
                Button(onClick = {
                    currentScreen = "webview:https://liella.club/"
                    showPlanetariumDialog = false
                }) {
                    Text("官方网站")
                }
            },
            dismissButton = {
                Button(onClick = {
                    currentScreen = "internal://music_magazine"
                    showPlanetariumDialog = false
                }) {
                    Text("本地内容")
                }
            }
        )
    }

    // MV详情页叠加层
    selectedMV?.let { mv ->
        MusicVideoDetailScreen(
            mv = mv,
            onBack = { selectedMV = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var loadingProgress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    loadingProgress = newProgress
                }
            }
            // 在这里启用 JavaScript，并可以加入其他安全设置，例如禁用文件访问
            settings.javaScriptEnabled = true
            // 如果不需要文件/内容访问，可以禁用它们：
            settings.allowFileAccess = false
            settings.allowContentAccess = false

            settings.domStorageEnabled = true
            settings.setSupportZoom(true)
        }
    }

    BackHandler(onBack = {
        if (canGoBack) {
            webView.goBack()
        } else {
            onClose()
        }
    })

    DisposableEffect(Unit) {
        webView.loadUrl(url)
        onDispose { webView.destroy() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部导航栏
        TopAppBar(
            title = { Text("浏览网页") },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { webView.goBack() },
                    enabled = canGoBack
                ) {
                    Icon(Icons.Filled.ArrowBackIosNew, "上一页")
                }
                IconButton(
                    onClick = { webView.goForward() },
                    enabled = webView.canGoForward()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "下一页",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(onClick = { webView.reload() }) {
                    Icon(Icons.Filled.Refresh, "刷新")
                }
            }
        )

        // 加载进度条
        if (loadingProgress < 100) {
            LinearProgressIndicator(
                progress = { loadingProgress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // WebView容器
        AndroidView(
            factory = { webView },
            modifier = Modifier.weight(1f),
            update = { view ->
                canGoBack = view.canGoBack()
            }
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicMagazineScreen(
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("🎵 音乐与杂志") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            }
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(items = musicMagazineData) { mv ->
                MusicVideoCard(
                    mv = mv,
                    onClick = { /* ★★★ 此处已清空点击响应 ★★★ */ }
                )
            }
        }
    }
}

@Composable
private fun MusicVideoCard(
    mv: MusicVideo,
    onClick: () -> Unit = {} // 默认空实现
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .clickable { onClick() }, // 调用 onClick
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = mv.coverPlaceholder,
                contentDescription = "封面",
                modifier = Modifier.size(120.dp)
            )
            Text(mv.title)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicVideoDetailScreen(
    mv: MusicVideo,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // 顶部导航栏
        TopAppBar(
            title = { Text(mv.title) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        )

        // 视频占位区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // 播放图标占位
            Icon(
                imageVector = Icons.Default.PlayCircleOutline,
                contentDescription = "播放",
                modifier = Modifier.size(64.dp),
                tint = Color.White
            )
        }

        // 简介区域
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = mv.description,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp
            )
        }
    }

    // 处理返回键
    BackHandler {
        onBack()
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

    // 滚动状态和悬浮按钮可见性
    val scrollState = rememberScrollState()
    var isFabVisible by remember { mutableStateOf(true) }

    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            isFabVisible = false
        } else {
            delay(500) // 停止滚动后延迟显示
            isFabVisible = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 固定标题区域 - 新增的固定部分
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(2f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
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
                    }
                }
            }

            // 可滚动内容区域 - 与原结构分离
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                if (currentDimension == "角色") {
                    groupedCharacters.forEach { (groupName, characters) ->
                        Column {
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
                        Column {
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

        // 悬浮刷新按钮
        AnimatedVisibility(
            visible = isFabVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { viewModel.refreshData(context) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "刷新数据",
                    modifier = Modifier.size(24.dp)
                )
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
            }


            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 显示信息项，包含生日、事务所及条件下的系数
            val infoList = mutableListOf<Pair<String, String>>().apply {
                add("生日" to voiceActor.birthday)
                add("事务所" to voiceActor.agency)
                if (showCoefficient) add("系数" to voiceActor.coefficient)
            }
            GridLayout(infoList)

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
    var remainingTime by remember { mutableIntStateOf(7) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // 新增：用于显示底部 Snackbar 提示的状态及 HostState
    var showDarkRealmSnackbar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    var showTextSizeDialog by remember { mutableStateOf(false) } // 新增对话框状态
    val textSize by settingsManager.textSizeFlow.collectAsState(
        initial = SettingsManager.TextSize.FOLLOW_SYSTEM
    )

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
        }
        // 将 SnackbarHost 放在 Box 的底部中间
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // 新增文字大小对话框
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


@Composable
private fun VersionEntry(
    versionName: String,
    onSecretActivated: () -> Unit
) {
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
