package com.lovelive.dreamycolor

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.lovelive.dreamycolor.model.Website

/**
 * 灵感屏幕，包含各种Love Live相关网站和内部功能入口
 */
@Composable
fun InspirationScreen() {
    val websites = remember {
        listOf(
            Website(
                title = "缪斯时光蛋",
                url = "dialog://timecapsule",
                icon = Icons.Filled.HistoryEdu
            ),
            Website(
                title = "Aqours许愿瓶",
                url = "dialog://wishbottle",
                icon = Icons.Filled.WaterDrop
            ),
            Website(
                title = "虹之咲活动室",
                url = "dialog://activityroom",
                icon = Icons.Filled.Group
            ),
            Website(
                title = "Liella星象馆",
                url = "dialog://liella",
                icon = Icons.Filled.Star
            )
        )
    }

    // 使用单一状态管理对话框显示
    var dialogState by remember { mutableStateOf<String?>(null) }
    var currentScreen by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Crossfade(
        targetState = currentScreen,
        animationSpec = tween(300)
    ) { screen ->
        when (screen) {
            "internal://planetarium" -> PlanetariumScreen(onBack = { currentScreen = null })
            "internal://time_capsule" -> TimeCapsuleScreen(onBack = { currentScreen = null })
            "internal://wish_pool" -> WishPoolScreen(onBack = { currentScreen = null })
            "internal://activity_log" -> ActivityLogScreen(onBack = { currentScreen = null })
            null -> {
                WebsiteGrid(
                    websites = websites,
                    onWebsiteClick = { url ->
                        when (url) {
                            "dialog://liella" -> dialogState = "liella"
                            "dialog://timecapsule" -> dialogState = "timecapsule"
                            "dialog://wishbottle" -> dialogState = "wishbottle"
                            "dialog://activityroom" -> dialogState = "activityroom"
                        }
                    }
                )
            }
            else -> {
                currentScreen = null
                WebsiteGrid(
                    websites = websites,
                    onWebsiteClick = { /* ... */ }
                )
            }
        }
    }

    // 合并对话框逻辑，减少重复代码
    dialogState?.let { dialogType ->
        val dialogConfig = when(dialogType) {
            "liella" -> DialogConfig(
                title = "进入星象馆",
                message = "请选择您要进入的版本：",
                confirmText = "官方网站",
                confirmAction = {
                    context.openInBrowser("https://liella.club/")
                    dialogState = null
                },
                dismissText = "星象馆",
                dismissAction = {
                    currentScreen = "internal://planetarium"
                    dialogState = null
                }
            )
            "timecapsule" -> DialogConfig(
                title = "打开时光蛋",
                message = "请选择操作：",
                confirmText = "官方网站",
                confirmAction = {
                    context.openInBrowser("https://www.llhistoy.lionfree.net/lovelive.ws/index.html")
                    dialogState = null
                },
                dismissText = "本地存档",
                dismissAction = {
                    currentScreen = "internal://time_capsule"
                    dialogState = null
                }
            )
            "wishbottle" -> DialogConfig(
                title = "打开许愿瓶",
                message = "请选择操作：",
                confirmText = "官方网站",
                confirmAction = {
                    context.openInBrowser("https://aqours.tv/")
                    dialogState = null
                },
                dismissText = "许愿池",
                dismissAction = {
                    currentScreen = "internal://wish_pool"
                    dialogState = null
                }
            )
            "activityroom" -> DialogConfig(
                title = "进入活动室",
                message = "请选择操作：",
                confirmText = "官方网站",
                confirmAction = {
                    context.openInBrowser("https://nijigaku.club/")
                    dialogState = null
                },
                dismissText = "活动记录",
                dismissAction = {
                    currentScreen = "internal://activity_log"
                    dialogState = null
                }
            )
            else -> null
        }

        dialogConfig?.let { config ->
            AlertDialog(
                onDismissRequest = { dialogState = null },
                title = { Text(config.title) },
                text = { Text(config.message) },
                confirmButton = {
                    Button(onClick = config.confirmAction) {
                        Text(config.confirmText)
                    }
                },
                dismissButton = {
                    Button(onClick = config.dismissAction) {
                        Text(config.dismissText)
                    }
                },
                properties = DialogProperties(
                    dismissOnClickOutside = true,
                    dismissOnBackPress = true
                )
            )
        }
    }
}

/**
 * 星象馆屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanetariumScreen(
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("🌟 星象馆") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "星象馆开发中",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            )
        }
    }
}

/**
 * 时光蛋屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeCapsuleScreen(
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("📼 缪斯时光蛋") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "时光蛋本地内容待开发",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 许愿池屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishPoolScreen(
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("🏺 许愿池") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "许愿池功能筹备中",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 活动记录屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogScreen(
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("📝 活动记录") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "活动记录空页面",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 网站卡片组件
 */
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

/**
 * 网站网格组件
 */
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

/**
 * 在浏览器中打开URL的扩展函数
 */
fun Context.openInBrowser(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    startActivity(intent)
}