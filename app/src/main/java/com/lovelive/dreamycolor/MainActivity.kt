/**
 * 主Activity类，负责应用程序的整体UI结构和导航逻辑
 * 包含主题切换、启动屏、底部导航等核心功能
 */
@file:OptIn(ExperimentalFoundationApi::class)
package com.lovelive.dreamycolor

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.lovelive.dreamycolor.data.repository.EncyclopediaRepository
import com.lovelive.dreamycolor.database.EncyclopediaDatabase
import com.lovelive.dreamycolor.ui.theme.DreamyColorTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugins.GeneratedPluginRegistrant
import io.flutter.plugin.common.MethodChannel
import android.util.Log
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.absoluteValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally


/**
 * 对话框配置数据类，用于统一管理对话框的显示内容和交互行为
 * @property title 对话框标题
 * @property message 对话框消息内容
 * @property confirmText 确认按钮文本
 * @property confirmAction 确认按钮点击回调
 * @property dismissText 取消按钮文本
 * @property dismissAction 取消按钮点击回调
 */
data class DialogConfig(
    val title: String,
    val message: String,
    val confirmText: String,
    val confirmAction: () -> Unit,
    val dismissText: String,
    val dismissAction: () -> Unit
)


/**
 * 应用程序的主Activity，继承自ComponentActivity
 * 负责初始化主题设置、资源管理和UI界面的构建
 */
class MainActivity : ComponentActivity() {
    private val channelName = "com.lovelive.dreamycolor/encyclopedia"
    private lateinit var channel: MethodChannel

    private val settingsManager by lazy { SettingsManager(this) }

    private val flutterEngineId = "dreamycolor_flutter_engine"
    private lateinit var flutterEngine: FlutterEngine

    // 处理来自Flutter的消息
    private fun handleFlutterMessage(message: String?, result: MethodChannel.Result) {
        message?.let {
            Log.d("Flutter Message", "Received message from Flutter: $it")
            result.success("Android received: $it")
        } ?: result.error("INVALID_MESSAGE", "Message was null", null)
    }
    
    private suspend fun getEncyclopediaData(): Map<String, List<Any>> {
        val database = EncyclopediaDatabase.getDatabase(this)
        val repository = EncyclopediaRepository(database.encyclopediaDao())
        
        val characters = repository.getAllCharacters().first()
        val voiceActors = repository.getAllVoiceActors().first()
        
        return mapOf(
            "characters" to characters,
            "voiceActors" to voiceActors
        )
    }


    /**
 * Activity创建时的回调方法
 * 初始化主题设置、启用边缘到边缘显示，并设置主界面内容
 */
override fun onCreate(savedInstanceState: Bundle?) {
        // 添加性能监控
        val startTime = System.currentTimeMillis()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化并缓存Flutter引擎
        initFlutterEngine()

        // 初始化MethodChannel
        channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName)

        // 设置方法调用处理器
        channel.setMethodCallHandler { call, result ->
            when (call.method) {
                "sendMessage" -> {
                    // 处理来自Flutter的消息
                    val message = call.argument<String>("message")
                    handleFlutterMessage(message, result)
                }
                
                "getEncyclopediaData" -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val data = getEncyclopediaData()
                            result.success(data)
                        } catch (e: Exception) {
                            result.error("DATA_ERROR", "Failed to get encyclopedia data", e.message)
                        }
                    }
                }

                else -> {
                    result.notImplemented()
                }
            }
        }


        setContent {
            val themeMode by settingsManager.themeModeFlow.collectAsState(
                initial = SettingsManager.ThemeMode.FOLLOW_SYSTEM
            )
            val textSize by settingsManager.textSizeFlow.collectAsState(
                initial = SettingsManager.TextSize.FOLLOW_SYSTEM
            )
            val isDarkTheme = when (themeMode) {
                SettingsManager.ThemeMode.LIGHT -> false
                SettingsManager.ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            
            // 收集主题颜色设置
            val colorTheme by settingsManager.colorThemeFlow.collectAsState(initial = SettingsManager.ColorTheme.MATERIAL_YOU)

            // 动态设置状态栏文字颜色
            LaunchedEffect(isDarkTheme) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDarkTheme
                }
            }

            DreamyColorTheme(
                themeMode = themeMode,
                textSize = textSize,
                colorTheme = colorTheme
            ) {
                // 使用rememberSaveable保持屏幕旋转后的状态
                var showSplash by rememberSaveable { mutableStateOf(true) }

                Box(modifier = Modifier.fillMaxSize()) {
                    // 主内容始终在底部
                    MainContent(settingsManager = settingsManager)

                    // 开屏动画覆盖在上面，逐渐消失
                    AnimatedVisibility(
                        visible = showSplash,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit = fadeOut(animationSpec = tween(800))
                    ) {
                        SplashScreen(
                            onTimeout = { showSplash = false },
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        )
                    }
                }
            }
        }
        // 记录启动时间
        val launchTime = System.currentTimeMillis() - startTime
        Log.d("Performance", "Activity启动耗时: $launchTime ms")

    }


    private fun initFlutterEngine() {

        // 创建FlutterEngine实例
        flutterEngine = FlutterEngine(this)

        // 启动引擎执行Dart代码
        flutterEngine.dartExecutor.executeDartEntrypoint(DartExecutor.DartEntrypoint.createDefault())

        // 注册插件
        GeneratedPluginRegistrant.registerWith(flutterEngine)

        // 缓存引擎以便重用
        FlutterEngineCache.getInstance().put(flutterEngineId, flutterEngine)
    }
}

/**
 * 启动屏界面组件
 * 显示应用启动时的过渡动画，支持自定义超时回调
 *
 * @param onTimeout 启动屏超时后的回调函数
 * @param modifier 可选的Modifier参数，用于自定义组件样式
 */
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
            stringResource(R.string.splash_text).forEach { char ->
                Text(
                    text = char.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
        LaunchedEffect(Unit) {
            delay(1000L)
            onTimeout()
        }
    }
}


/**
 * 主界面内容组件
 * 包含底部导航栏和多个子页面的容器，管理页面间的导航和状态
 *
 * @param settingsManager 设置管理器实例，用于管理应用配置
 */
@Composable
fun MainContent(settingsManager: SettingsManager) {
    val items = remember {
        listOf(
            Screen.Exclusive,
            Screen.Inspiration,
            Screen.Encyclopedia,
            Screen.Profile
        )
    }
    // 添加导航状态
    var currentScreen by rememberSaveable { mutableStateOf<String?>(null) }
    var characterName by rememberSaveable { mutableStateOf("") }
    var voiceActorName by rememberSaveable { mutableStateOf("") }
    // 添加Tab选择状态作为主导航机制
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    // 使用rememberSaveable保持页面状态在配置更改时不丢失
    val pagerState = rememberPagerState(pageCount = { items.size }, initialPage = selectedTabIndex)
    // 添加惰性初始化的状态标志
    var encyclopediaInitialized by remember { mutableStateOf(false) }




    // 确保pagerState和selectedTabIndex保持同步
    LaunchedEffect(selectedTabIndex) {
        if (pagerState.currentPage != selectedTabIndex) {
            pagerState.animateScrollToPage(selectedTabIndex)
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != selectedTabIndex) {
            selectedTabIndex = pagerState.currentPage
        }
        // 只有当切换到百科标签页时，才标记其为已初始化
        if (pagerState.currentPage == 2) {
            encyclopediaInitialized = true

        }
    }
    Scaffold(
        bottomBar = {
            // 只在主界面显示底部导航栏
            if (currentScreen == null) {
                NavigationBar {
                    items.forEachIndexed { index, screen ->
                        NavigationBarItem(
                            icon = {},
                            label = {
                                val textStyle =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        MaterialTheme.typography.labelMedium
                                    } else {
                                        LocalTextStyle.current
                                    }
                                Text(
                                    text = stringResource(id = screen.titleRes),
                                    style = textStyle.copy(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            },
                            selected = selectedTabIndex == index,
                            onClick = {
                                selectedTabIndex = index
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // 使用 AnimatedContent 带缓动动画
        AnimatedContent(
            targetState = currentScreen,
            modifier = Modifier.padding(innerPadding),
            transitionSpec = {
                if (targetState != null && initialState == null) {
                    // 页面从右侧滑入
                    slideInHorizontally(initialOffsetX = { it }) + fadeIn(animationSpec = tween(300)) togetherWith
                            slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(animationSpec = tween(300))
                } else if (targetState == null && initialState != null) {
                    // 页面从左侧滑出
                    slideInHorizontally(initialOffsetX = { -it }) + fadeIn(animationSpec = tween(300)) togetherWith
                            slideOutHorizontally(targetOffsetX = { it }) + fadeOut(animationSpec = tween(300))
                } else {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                }
            }
        ) { screen ->
            when (screen) {
                "character_detail" -> CharacterDetailScreen(
                    characterName = characterName,
                    onBackPressed = {
                        currentScreen = null
                    }
                )

                "voice_actor_detail" -> VoiceActorDetailScreen(
                    voiceActorName = voiceActorName,
                    onBackPressed = {
                        currentScreen = null
                    }
                )

                null -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = true, // 手动滑动
                            pageSpacing = 0.dp,
                            key = { items[it].titleRes }
                        ) { page ->
                            // 使用graphicsLayer应用淡入淡出效果
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        // 计算页面偏移量并应用透明度
                                        val pageOffset =
                                            ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                                        // 创建淡入淡出效果
                                        // 当页面完全可见时透明度为1，滑动过程中逐渐变为0
                                        alpha = 1f - (pageOffset * 0.8f).coerceIn(0f, 1f)
                                    }
                            ) {
                                when (page) {
                                    0 -> ExclusiveScreen()
                                    1 -> InspirationScreen()
                                    2 -> {
                                        if (selectedTabIndex == 2 || encyclopediaInitialized) {
                                            key(page) {
                                                LaunchedEffect(Unit) {
                                                    delay(2000)
                                                }
                                                EncyclopediaScreen(
                                                    onCharacterClick = { name ->
                                                        characterName = name
                                                        currentScreen = "character_detail"
                                                    }
                                                )
                                            }
                                        } else {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.align(Alignment.Center)
                                                )
                                            }
                                        }
                                    }
                                    3 -> ProfileScreen(settingsManager = settingsManager)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// Website数据类和相关函数已迁移到InspirationScreen.kt
// InspirationScreen已迁移到InspirationScreen.kt文件中
// 相关子屏幕已迁移到InspirationScreen.kt文件中
// EncyclopediaScreen已迁移到EncyclopediaScreen.kt文件中
