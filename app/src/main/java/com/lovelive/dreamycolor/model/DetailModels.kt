package com.lovelive.dreamycolor.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import androidx.core.graphics.toColorInt

// 角色详情数据模型
@Serializable
data class CharacterDetail(
    val characterName: String,
    val japaneseName: String,  // 添加日文名
    val romanizedName: String, // 添加罗马音
    val basicInfo: CharacterBasicInfo,
    val advancedInfo: CharacterAdvancedInfo,
    val footnotes: List<String> // 添加脚注/参考资料
)

@Serializable
data class CharacterBasicInfo(
    val age: Int,             // 添加年龄
    val school: String,       // 添加所属学校
    val height: Int,          // 添加身高（cm）
    val cvName: String,       // 声优
    val group: String,        // 团体
    val attribute: String,    // 属性
    val themeColor: String,   // 主题色
    val bio: String           // 添加简介
)

@Serializable
data class CharacterAdvancedInfo(
    val relationships: List<Relationship>
)

@Serializable
data class Relationship(
    val target: String,
    val type: String,
    val description: String
)

// 声优详情数据模型
@Serializable
data class VoiceActorDetail(
    val voiceActorName: String,
    val career: Career,
    val discography: Discography
)

@Serializable
data class Career(
    val agency: String,
    val debutYear: Int,
    val achievements: List<String>
)

@Serializable
data class Discography(
    val bestAlbum: String,
    val characterSongs: List<CharacterSong>
)

@Serializable
data class CharacterSong(
    val title: String,
    val character: String
)

// 辅助函数 - 将十六进制颜色字符串转换为 Color 对象
fun String.toColor(): Color {
    return try {
        Color(this.toColorInt())
    } catch (_: Exception) {
        Color.Magenta // 默认颜色
    }
}
