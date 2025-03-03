package com.lovelive.dreamycolor

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lovelive.dreamycolor.model.CharacterDetail
import com.lovelive.dreamycolor.model.toColor
import com.lovelive.dreamycolor.utils.DetailJsonUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailScreen(
    characterName: String,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    var characterDetail by remember { mutableStateOf<CharacterDetail?>(null) }

    // 加载角色详情数据
    LaunchedEffect(characterName) {
        // 根据角色名称确定要加载的JSON文件
        val fileName = when (characterName) {
            "高坂穗乃果" -> "honoka_detail.json"
            "南小鸟" -> "kotori_detail.json"
            // 添加更多角色
            else -> null
        }

        if (fileName != null) {
            characterDetail = DetailJsonUtils.loadCharacterDetail(context, fileName)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(characterName) },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = characterDetail?.basicInfo?.themeColor?.toColor()?.copy(alpha = 0.2f)
                    ?: MaterialTheme.colorScheme.surface
            )
        )

        characterDetail?.let { detail ->
            val themeColor = detail.basicInfo.themeColor.toColor()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 头部区域
                CharacterHeader(detail, themeColor)

                // 基本信息区域
                SectionTitle(title = "基本信息", color = themeColor)
                InfoItem(label = "所属团体", value = detail.basicInfo.group, color = themeColor)
                InfoItem(label = "属性", value = detail.basicInfo.attribute, color = themeColor)
                InfoItem(label = "声优", value = detail.basicInfo.cvName, color = themeColor)

                // 关系区域
                if (detail.advancedInfo.relationships.isNotEmpty()) {
                    SectionTitle(title = "人际关系", color = themeColor)
                    detail.advancedInfo.relationships.forEach { relationship ->
                        RelationshipItem(
                            target = relationship.target,
                            type = relationship.type,
                            description = relationship.description,
                            color = themeColor
                        )
                    }
                }

                // 底部空间
                Spacer(modifier = Modifier.height(24.dp))
            }
        } ?: run {
            // 加载中或无数据状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun CharacterHeader(
    detail: CharacterDetail,
    themeColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(themeColor.copy(alpha = 0.1f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 角色头像（占位）
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(themeColor.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = detail.characterName.take(1),
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = themeColor,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 角色名称
        DetailHeader(
            title = detail.characterName,
            themeColor = themeColor
        )

        // 角色属性标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AttributeChip(
                text = detail.basicInfo.group,
                color = themeColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            AttributeChip(
                text = detail.basicInfo.attribute,
                color = themeColor
            )
        }
    }
}














