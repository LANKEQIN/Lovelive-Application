package com.lovelive.dreamycolor

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lovelive.dreamycolor.data.repository.EncyclopediaRepository
import com.lovelive.dreamycolor.database.EncyclopediaDatabase
import com.lovelive.dreamycolor.model.CharacterCard
import com.lovelive.dreamycolor.model.VoiceActorCard
import com.lovelive.dreamycolor.utils.copyToClipboard
import com.lovelive.dreamycolor.viewmodel.EncyclopediaViewModel
import androidx.compose.foundation.lazy.grid.GridItemSpan
import com.lovelive.dreamycolor.utils.PinyinUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.ripple

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 使用AnimatedContent来平滑切换维度按钮
            AnimatedContent(
                targetState = currentDimension,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                }
            ) { dimension ->
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
                                selected = dimension == "角色",
                                onClick = { currentDimension = "角色" }
                            )
                            DimensionButton(
                                text = "声优",
                                selected = dimension == "声优",
                                onClick = { currentDimension = "声优" }
                            )
                        }
                    }
                }
            }
            // 使用key来确保切换维度时列表完全重建
            key(currentDimension) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    state = listState
                ) {
                    val items = if (currentDimension == "角色")
                        characterItems.value
                    else
                        voiceActorItems.value

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
        }
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

@Composable
fun GroupHeader(
    title: String,
    expanded: Boolean = true,
    onExpandedChange: () -> Unit = {},
    verticalPadding: Dp = 12.dp
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, // 移除手动指定的 Ripple，使用默认主题 Ripple
                    onClick = onExpandedChange
                )
                .padding(horizontal = 12.dp, vertical = verticalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            )

            val rotation by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "rotation"
            )

            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (expanded) "折叠" else "展开",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.graphicsLayer {
                    rotationZ = rotation // 使用 graphicsLayer 设置旋转角度
                }
            )
        }
    }
}


@Composable
private fun DimensionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 300),
        label = "backgroundColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300),
        label = "textColor"
    )

    Box(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .background(
                backgroundColor,
                MaterialTheme.shapes.small
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            color = textColor,
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
            else -> 250.dp
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
                    else -> 250.dp
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
    val height = remember(showPinyin) {
        if (showPinyin) 70.dp else 50.dp
    }

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
            in 0..10 -> 13.sp
            11 -> 10.sp
            in 12..14 -> 13.sp
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