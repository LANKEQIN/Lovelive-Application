package com.lovelive.dreamycolor.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

// 角色详情数据模型
@Serializable
data class CharacterDetail(
    val characterName: String,
    val basicInfo: CharacterBasicInfo,
    val advancedInfo: CharacterAdvancedInfo
)

@Serializable
data class CharacterBasicInfo(
    val cvName: String,
    val group: String,
    val attribute: String,
    val themeColor: String
)

@Serializable
data class CharacterAdvancedInfo(
    val relationships: List<Relationship>,
    val signatureAttacks: List<SignatureAttack>
)

@Serializable
data class Relationship(
    val target: String,
    val type: String,
    val description: String
)

@Serializable
data class SignatureAttack(
    val name: String,
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
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color.Magenta // 默认颜色
    }
}
