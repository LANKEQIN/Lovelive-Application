package com.lovelive.dreamycolor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontStyle
import androidx.activity.compose.BackHandler



// 角色头像映射
private val characterImageResources = mapOf(
    "高坂穗乃果" to R.drawable.ic_honoka_head,
    // 其他角色映射...
)

// 获取角色头像资源 ID
private fun getCharacterImageResource(characterName: String): Int {
    return characterImageResources[characterName] ?: 0
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailScreen(
    characterName: String,
    onBackPressed: () -> Unit,
) {
    BackHandler(onBack = onBackPressed)

    val context = LocalContext.current
    var characterDetail by remember { mutableStateOf<CharacterDetail?>(null) }

    // 加载角色详情数据
    LaunchedEffect(characterName) {
        // 根据角色名称确定要加载的JSON文件
        val fileName = when (characterName) {
            "高坂穗乃果" -> "1_honoka_detail.json"
            "南小鸟" -> "1_kotori_detail.json"
            "园田海未" -> "1_umi_detail.json"
            "星空凛" -> "1_rin_detail.json"
            "小泉花阳" -> "1_hanayo_detail.json"
            "西木野真姬" -> "1_maki_detail.json"
            "矢泽妮可" -> "1_nico_detail.json"
            "绚濑绘里" -> "1_eli_detail.json"
            "东条希" -> "1_nozomi_detail.json"

            "樱内梨子" -> "2_riko_detail.json"
            "高海千歌"  -> "2_chika_detail.json"
            "渡边曜"   -> "2_you_detail.json"
            "松浦果南"  -> "2_kanan_detail.json"
            "黑泽黛雅"  -> "2_dia_detail.json"
            "小原鞠莉" -> "2_mari_detail.json"
            "津岛善子" -> "2_yoshiko_detail.json"
            "国木田花丸" -> "2_hanamaru_detail.json"
            "黑泽露比"  -> "2_rubi_detail.json"

            "高咲侑" -> "3_takasaki_yuu_detail.json"
            "上原步梦"  -> "3_uehara_ayumu_detail.json"
            "中须霞"   -> "3_nakasu_kasumi_detail.json"
            "樱坂雫" -> "3_osaka_shizuku_detail.json"
            "朝香果林" -> "3_asaka_karin_detail.json"
            "宫下爱" -> "3_miyashita_ai_detail.json"
            "近江彼方" -> "3_oumi_kanat_detail.json"
            "艾玛·维尔德" -> "3_emma_verde_detail.json"
            "天王寺璃奈" -> "3_tennouji_rina_detail.json"
            "三船栞子" -> "3_mifune_shioriko_detail.json"
            "钟岚珠" -> "3_zhong_lanzhu_detail.json"
            "米雅·泰勒" -> "3_mia_taylor_detail.json"
            "优木雪菜" -> "3_yuuki_setsuna_detail.json"

            "涩谷香音"  -> "4_shibuya_kanon_detail.json"
            "唐可可"   -> "4_tang_keke_detail.json"
            "岚千砂都" -> "4_arashi_chisato_detail.json"
            "平安名堇" -> "4_heanna_sumire_detail.json"
            "叶月恋" -> "4_hazuki_ren_detail.json"
            "樱小路希奈子" -> "4_sakurakoji_kinako_detail.json"
            "米女芽衣" -> "4_yoneme_mei_detail.json"
            "若菜四季" -> "4_wakana_shiki_detail.json"
            "鬼塚夏美" -> "4_onitsuka_natsumi_detail.json"
            "鬼塚冬毬" -> "4_onitsuka_tomari_detail.json"
            "薇恩·玛格丽特" -> "4_wien_margarete_detail.json"


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
                    .padding(horizontal = 0.dp)
            ) {
                // 头部区域
                CharacterHeader(detail, themeColor)

                // 基本信息区域
                SectionTitle(title = "基本信息", color = themeColor)
                Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)) { // 外层 Column 添加间距
                    InfoItem(label = "年龄", value = "${detail.basicInfo.age}岁", color = themeColor)
                    InfoItem(label = "学校", value = detail.basicInfo.school, color = themeColor)
                    InfoItem(label = "身高", value = "${detail.basicInfo.height}cm", color = themeColor)
                    InfoItem(label = "声优", value = detail.basicInfo.cvName, color = themeColor)
                    InfoItem(label = "所属团体", value = detail.basicInfo.group, color = themeColor)
                    InfoItem(label = "属性", value = detail.basicInfo.attribute, color = themeColor)
                }

                // 角色简介
                SectionTitle(title = "角色简介", color = themeColor)
                Text(
                    text = "    " + detail.basicInfo.bio,  // 直接在文本前面添加两个空格实现首行缩进
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3f // 行间距
                    ),
                    modifier = Modifier
                        .padding(vertical = 16.dp, horizontal = 20.dp)
                        .fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                )


                // 关系区域
                if (detail.advancedInfo.relationships.isNotEmpty()) {
                    SectionTitle(title = "人际关系（部分）", color = themeColor)
                    detail.advancedInfo.relationships.take(3).forEach { relationship ->
                        RelationshipItem(
                            target = relationship.target,
                            type = relationship.type,
                            description = relationship.description,
                            color = themeColor
                        )
                    }
                }

                // 脚注/参考资料
                if (detail.footnotes.isNotEmpty()) {
                    SectionTitle(title = "参考资料", color = themeColor)
                    detail.footnotes.forEachIndexed { index, footnote ->
                        Text(
                            text = "${index + 1}. $footnote",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 20.dp)
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
        // 角色头像
        val imageRes = getCharacterImageResource(detail.characterName)
        if (imageRes != 0) {
            // 有对应图片资源时显示图片
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = imageRes),
                contentDescription = detail.characterName,
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            // 没有对应图片资源时显示占位符
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
        }


        Spacer(modifier = Modifier.height(16.dp))

        // 角色中文名称
        Text(
            text = detail.characterName,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )

        // 角色日文名称
        Text(
            text = detail.japaneseName,
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // 罗马音
        Text(
            text = detail.romanizedName,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )


        // 角色属性标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AttributeChip(text = detail.basicInfo.group, color = themeColor)
            Spacer(modifier = Modifier.width(8.dp))
            AttributeChip(text = detail.basicInfo.attribute, color = themeColor)
        }
    }
}


@Composable
fun AttributeChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
        )
    }
}

















