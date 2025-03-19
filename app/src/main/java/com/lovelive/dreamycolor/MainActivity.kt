/**
 * 主Activity类，负责应用程序的整体UI结构和导航逻辑
 * 包含主题切换、启动屏、底部导航等核心功能
 */
@file:OptIn(ExperimentalFoundationApi::class)
package com.lovelive.dreamycolor

import android.annotation.SuppressLint
import android.app.Application
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelive.dreamycolor.data.repository.EncyclopediaRepository
import com.lovelive.dreamycolor.database.EncyclopediaDatabase
import com.lovelive.dreamycolor.model.CharacterCard
import com.lovelive.dreamycolor.model.VoiceActorCard
import com.lovelive.dreamycolor.ui.theme.DreamyColorTheme
import com.lovelive.dreamycolor.utils.copyToClipboard
import com.lovelive.dreamycolor.viewmodel.EncyclopediaViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.grid.GridItemSpan
import com.lovelive.dreamycolor.utils.PinyinUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.SizeTransform
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugins.GeneratedPluginRegistrant
import io.flutter.plugin.common.MethodChannel
import android.util.Log

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
    private val channelName = "com.example.dreamy_color/channel"
    private lateinit var channel: MethodChannel

    private val settingsManager by lazy { SettingsManager(this) }
    // 添加ResourceManager实例
    private val resourceManager by lazy { ResourceManager.getInstance(this) }

    private val flutterEngineId = "dreamycolor_flutter_engine"
    private lateinit var flutterEngine: FlutterEngine

    // 处理来自Flutter的消息
    private fun handleFlutterMessage(message: String?, result: MethodChannel.Result) {
        message?.let {
            Log.d("Flutter Message", "Received message from Flutter: $it")
            result.success("Android received: $it")
        } ?: result.error("INVALID_MESSAGE", "Message was null", null)
    }


    // 向Flutter发送消息的方法
    fun sendMessageToFlutter(message: String) {
        runOnUiThread {
            channel.invokeMethod("messageFromNative", message, object : MethodChannel.Result {
                override fun success(result: Any?) {
                    Log.d("Native Message", "Flutter response: $result")
                }
                override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
                    Log.e("Native Message", "Error: $errorCode, $errorMessage")
                }
                override fun notImplemented() {
                    Log.w("Native Message", "Method not implemented")
                }
            })
        }
    }

    /**
 * Activity创建时的回调方法
 * 初始化主题设置、启用边缘到边缘显示，并设置主界面内容
 */
override fun onCreate(savedInstanceState: Bundle?) {
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

                else -> {
                    result.notImplemented()
                }
            }
        }

        // 预加载常用资源
        preloadResources()

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

    // 添加启动Flutter界面的方法
    fun startFlutterActivity() {
        startActivity(
            FlutterActivity
                .withCachedEngine(flutterEngineId)
                .build(this)
        )
    }



    // 添加预加载资源的方法
    private fun preloadResources() {
        // 预加载常用字符串资源
        val commonStringIds = listOf(
            R.string.app_name,
            R.string.splash_text
            // 可以根据需要添加更多常用字符串资源ID
        )
        resourceManager.preloadStrings(commonStringIds)
        
        // 预加载常用图片资源
        val commonDrawableIds = listOf(
            R.drawable.ic_launcher_background
            // 可以根据需要添加更多常用图片资源ID
        )
        resourceManager.preloadDrawables(commonDrawableIds)
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
            delay(1100L)
            onTimeout()
        }
    }
}


// 主界面内容
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
            Screen.Profile // 添加 Profile 屏幕
        )
    }
    // 添加导航状态
    var currentScreen by rememberSaveable { mutableStateOf<String?>(null) }
    var characterName by rememberSaveable { mutableStateOf("") }
    var voiceActorName by rememberSaveable { mutableStateOf("") }
    // 添加这个变量来记录百科卡片列表的滚动位置
    var encyclopediaScrollPosition by rememberSaveable { mutableIntStateOf(0) }
    var encyclopediaDimension by rememberSaveable { mutableStateOf("角色") }
    // 添加Tab选择状态作为主导航机制
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    // 使用rememberSaveable保持页面状态在配置更改时不丢失
    val pagerState = rememberPagerState(pageCount = { items.size }, initialPage = selectedTabIndex)


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
                                val textStyle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
        // 页面切换动画
        AnimatedContent(
            targetState = currentScreen,
            modifier = Modifier.padding(innerPadding),
            transitionSpec = {
                // 替换原有的淡入淡出动画为左右平移动画
                val slideDirection = if (targetState == null)
                    slideInHorizontally { -it } togetherWith
                            slideOutHorizontally { it }
                else
                    slideInHorizontally { it } togetherWith
                            slideOutHorizontally { -it }

                slideDirection.using(SizeTransform(clip = false))
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
                        // 使用HorizontalPager替代手势检测，提供更流畅的滑动体验
                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = true, // 允许用户滑动
                            pageSpacing = 0.dp, // 页面间距
                            // 使用最新版本的HorizontalPager API
                        ) { page ->
                            when (page) {
                                0 -> ExclusiveScreen()
                                1 -> InspirationScreen()
                                2 -> EncyclopediaScreen(
                                    onCharacterClick = { name ->
                                        characterName = name
                                        currentScreen = "character_detail"
                                    },
                                    onVoiceActorClick = { name ->
                                        voiceActorName = name
                                        currentScreen = "voice_actor_detail"
                                    },
                                    initialScrollPosition = encyclopediaScrollPosition,
                                    onScrollPositionChange = { position ->
                                        encyclopediaScrollPosition = position
                                    },
                                    initialDimension = encyclopediaDimension,
                                    onDimensionChange = { dimension ->
                                        encyclopediaDimension = dimension
                                    }
                                )
                                3 -> ProfileScreen(settingsManager = settingsManager) // 添加 ProfileScreen
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

/**
 * 分组项密封类，用于百科页面的列表项类型定义
 * 包含Header（组标题）、Character（角色信息）和VoiceActor（声优信息）三种类型
 */
sealed class GroupItem {
    data class Header(val title: String) : GroupItem()
    data class Character(val data: CharacterCard) : GroupItem()
    data class VoiceActor(val data: VoiceActorCard) : GroupItem()
}


/**
 * 百科页面组件
 * 展示角色和声优信息的可折叠列表，支持分组显示和状态保持
 *
 * @param onCharacterClick 角色点击回调
 * @param onVoiceActorClick 声优点击回调
 * @param initialScrollPosition 初始滚动位置
 * @param onScrollPositionChange 滚动位置变化回调
 * @param initialDimension 初始维度（角色/声优）
 * @param onDimensionChange 维度变化回调
 */
@SuppressLint("MutableCollectionMutableState")
@Composable
fun EncyclopediaScreen(
    onCharacterClick: (String) -> Unit = {},
    onVoiceActorClick: (String) -> Unit = {},
    initialScrollPosition: Int = 0,
    onScrollPositionChange: (Int) -> Unit = {},
    initialDimension: String = "角色",
    onDimensionChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { EncyclopediaDatabase.getDatabase(context) }
    val repository = remember { EncyclopediaRepository(database.encyclopediaDao()) }
    val settingsManager = remember { SettingsManager(context) }
    val listState = rememberLazyGridState(initialFirstVisibleItemIndex = initialScrollPosition)
    var selectedCharacter by remember { mutableStateOf<CharacterCard?>(null) }
    var selectedVoiceActor by remember { mutableStateOf<VoiceActorCard?>(null) }
    val showCoefficient by settingsManager.showCoefficientFlow.collectAsState(initial = false)
    val showPinyin by settingsManager.showPinyinFlow.collectAsState(initial = false)
    var currentDimension by rememberSaveable { mutableStateOf(initialDimension) }
    var previousDimension by rememberSaveable { mutableStateOf(initialDimension) }
    // 使用rememberSaveable而不是remember来保持展开状态，避免重组时丢失
    var expandedGroups by rememberSaveable { mutableStateOf(mutableMapOf<String, Boolean>()) }

    LaunchedEffect(currentDimension) {
        onDimensionChange(currentDimension)
    }

    val currentScrollPosition = remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex }
    }
    LaunchedEffect(currentScrollPosition.value) {
        if (currentScrollPosition.value != initialScrollPosition) {
            onScrollPositionChange(currentScrollPosition.value)
        }
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

    // 使用derivedStateOf优化列表项计算，减少重组
    val characterItems = remember(groupedCharacters, expandedGroups) {
        derivedStateOf {
            groupedCharacters.flatMap { (group, list) ->
                val isExpanded = expandedGroups[group] != false
                listOf(GroupItem.Header(group)) +
                        if (isExpanded) list.map { GroupItem.Character(it) } else emptyList()
            }
        }
    }

    val voiceActorItems = remember(groupedVoiceActors, expandedGroups) {
        derivedStateOf {
            groupedVoiceActors.flatMap { (group, list) ->
                val isExpanded = expandedGroups[group] != false
                listOf(GroupItem.Header(group)) +
                        if (isExpanded) list.map { GroupItem.VoiceActor(it) } else emptyList()
            }
        }
    }

    // 移除滚动监听逻辑和isFabVisible状态变量

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.shapes.medium
                        )
                        .padding(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
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

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                state = listState
            ) {
                val items = if (currentDimension == "角色") characterItems.value else voiceActorItems.value
                items(
                    items = items,
                    span = { item ->
                        when (item) {
                            is GroupItem.Header -> GridItemSpan(2)
                            else -> GridItemSpan(1)
                        }
                    },
                    key = { item ->
                        when (item) {
                            is GroupItem.Header -> "header_${item.title}"
                            is GroupItem.Character -> "character_${item.data.name}"
                            is GroupItem.VoiceActor -> "voiceactor_${item.data.name}"
                        }
                    }
                ) { item ->
                    when (item) {
                        is GroupItem.Header -> GroupHeader(
                            title = item.title,
                            expanded = expandedGroups[item.title] != false,
                            onExpandedChange = {
                                expandedGroups = expandedGroups.toMutableMap().apply {
                                    this[item.title] = this[item.title] == false
                                }
                            }
                        )
                        is GroupItem.Character -> {
                            // 不再需要AnimatedVisibility，因为我们在构建列表时已经过滤了
                            CharacterCardUI(
                                character = item.data,
                                showPinyin = showPinyin,
                                onClick = { character ->
                                    previousDimension = currentDimension
                                    selectedCharacter = character
                                }
                            )
                        }
                        is GroupItem.VoiceActor -> {
                            // 不再需要AnimatedVisibility，因为我们在构建列表时已经过滤了
                            VoiceActorCardUI(
                                voiceActor = item.data,
                                showCoefficient = showCoefficient,
                                showPinyin = showPinyin,
                                onClick = { voiceActor ->
                                    previousDimension = currentDimension
                                    selectedVoiceActor = voiceActor
                                }
                            )
                        }
                    }
                }
            }
        }

        // 移除刷新按钮相关代码
    }

    selectedCharacter?.let { character ->
        CharacterOptionsDialog(
            character = character,
            onDismiss = {
                selectedCharacter = null
                currentDimension = previousDimension
            },
            onLocalPageClick = {
                selectedCharacter = null
                currentDimension = previousDimension
                onCharacterClick(character.name)
            },
            onExternalWikiClick = {
                selectedCharacter = null
                currentDimension = previousDimension
                val url = "https://mzh.moegirl.org.cn/${character.name}"
                context.openInBrowser(url)
            }
        )
    }

    selectedVoiceActor?.let { voiceActor ->
        VoiceActorOptionsDialog(
            voiceActor = voiceActor,
            onDismiss = {
                selectedVoiceActor = null
                currentDimension = previousDimension
            },
            onLocalPageClick = {
                selectedVoiceActor = null
                currentDimension = previousDimension
                onVoiceActorClick(voiceActor.name)
            },
            onExternalWikiClick = {
                selectedVoiceActor = null
                currentDimension = previousDimension
                val url = "https://mzh.moegirl.org.cn/${voiceActor.name}"
                context.openInBrowser(url)
            }
        )
    }
}

// 新增：分组标题组件
@Composable
fun GroupHeader(
    title: String,
    expanded: Boolean = true,
    onExpandedChange: () -> Unit = {},
    verticalPadding: Dp = 12.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.shapes.medium
            )
            .padding(horizontal = 12.dp, vertical = verticalPadding)
            .clickable { onExpandedChange() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = fadeOut(animationSpec = tween(durationMillis = 200))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "折叠" else "展开",
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
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
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun CharacterOptionsDialog(
    character: CharacterCard,
    onDismiss: () -> Unit,
    onLocalPageClick: () -> Unit,
    onExternalWikiClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("查看${character.name}的详细信息") },
        text = { Text("请选择查看方式：") },
        confirmButton = {
            Button(onClick = onLocalPageClick) {
                Text("本地页面")
            }
        },
        dismissButton = {
            Button(onClick = onExternalWikiClick) {
                Text("萌娘百科")
            }
        }
    )
}

@Composable
fun VoiceActorOptionsDialog(
    voiceActor: VoiceActorCard,
    onDismiss: () -> Unit,
    onLocalPageClick: () -> Unit,
    onExternalWikiClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("查看${voiceActor.name}的详细信息") },
        text = { Text("请选择查看方式：") },
        confirmButton = {
            Button(onClick = onLocalPageClick) {
                Text("本地页面")
            }
        },
        dismissButton = {
            Button(onClick = onExternalWikiClick) {
                Text("萌娘百科")
            }
        }
    )
}


@Composable
fun VoiceActorCardUI(
    voiceActor: VoiceActorCard,
    showCoefficient: Boolean,
    showPinyin: Boolean = false,
    onClick: (VoiceActorCard) -> Unit = {}
) {
    // 使用remember缓存计算结果，避免重组时重复计算
    val cardHeight = remember(showCoefficient, showPinyin) {
        when {
            showCoefficient && showPinyin -> 340.dp
            showCoefficient -> 280.dp
            showPinyin -> 270.dp
            else -> 240.dp
        }
    }

    // 预先计算信息项列表，避免重组时重复创建
    val infoItems = remember(voiceActor, showCoefficient) {
        listOfNotNull(
            "生日" to voiceActor.birthday,
            "事务所" to voiceActor.agency,
            if (showCoefficient) "系数" to voiceActor.coefficient else null
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            // 使用clickable替代pointerInput以提高性能
            .clickable { onClick(voiceActor) },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 标题区域
            NameSection(
                name = voiceActor.name,
                japaneseName = voiceActor.japaneseName,
                showPinyin = showPinyin
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            // 信息区域
            GridLayout(infoItems)
        }
    }
}

@Composable
fun CharacterCardUI(
    character: CharacterCard,
    onClick: (CharacterCard) -> Unit = {},
    showPinyin: Boolean = false
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                when {
                    showPinyin -> 270.dp
                    else -> 240.dp
                }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick(character) },
                    onLongPress = {
                        context.copyToClipboard("${character.name}\n${character.japaneseName}")
                    }
                )
            },
        elevation = CardDefaults.cardElevation(4.dp),
        shape = MaterialTheme.shapes.large,
        // 使用与VoiceActorCardUI相同的默认颜色
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 名称区域
            NameSection(
                name = character.name,
                japaneseName = character.japaneseName,
                showPinyin = showPinyin
            )
            // 分割线
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            // 信息网格
            GridLayout(
                listOfNotNull(
                    "生日" to character.birthday,
                    "年级" to character.schoolYear,
                )
            )
        }
    }
}

@Composable
private fun NameSection(name: String, japaneseName: String, showPinyin: Boolean = false) {
    // 使用remember缓存计算结果
    val height = remember(showPinyin) {
        if (showPinyin) 70.dp else 60.dp
    }

    // 预先计算拼音，避免在渲染时计算
    val pinyin = remember(name, showPinyin) {
        if (showPinyin) PinyinUtils.chinesePinyinMap[name] else null
    }

    // 预先计算罗马音，避免在渲染时计算
    val displayJapaneseName = remember(japaneseName, showPinyin) {
        if (showPinyin) PinyinUtils.convertJapaneseToRomaji(japaneseName) else japaneseName
    }

    // 根据拼音长度动态计算字体大小
    val pinyinFontSize = remember(pinyin) {
        when (pinyin?.length) {
            null, in 0..14 -> 13.sp
            in 19..23 -> 9.sp
            18 -> 10.sp
            16, 17 -> 11.sp
            else -> 13.sp
        }
    }


    // 根据日文名长度动态计算字体大小
    val japaneseNameFontSize = remember(japaneseName) {
        when (displayJapaneseName.length) {
            in 0..14 -> 13.sp
            in 15..17 -> 11.sp
            18 -> 10.sp
            else -> 13.sp
        }
    }


    Column(modifier = Modifier.height(height)) {
        // 中文名称
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.primary,
                fontSize = 19 .sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 如果开启拼音显示，且能从映射表找到对应拼音，则显示拼音
        if (showPinyin && pinyin != null) {
            Text(
                text = pinyin,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    fontSize = pinyinFontSize,
                    fontWeight = FontWeight.Normal
                ),
                lineHeight = 17.sp  // 行间距
            )
        }

        // 日文名显示
        Text(
            text = displayJapaneseName,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = japaneseNameFontSize
            ),
            lineHeight = 17.sp    // 行间距
        )
    }
}

@Composable
private fun GridLayout(items: List<Pair<String, String>>) {
    // 改为单列布局
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // 直接遍历所有项目，不需要行列计算
        items.forEach { item ->
            InfoItem(
                label = item.first,
                value = item.second,
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

    // 使用remember缓存背景颜色和文本样式，避免重组时重复创建
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
    val valueStyle = MaterialTheme.typography.bodyMedium
    val valueColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.small
            )
            .padding(8.dp)
            .combinedClickable(
                onClick = { /* 普通点击不做处理 */ },
                onLongClick = {
                    context.copyToClipboard(value)
                }
            )
    ) {
        Text(
            text = label,
            style = labelStyle,
            color = labelColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = valueStyle,
            color = valueColor
        )
    }
}
