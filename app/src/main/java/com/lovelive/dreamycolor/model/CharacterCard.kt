package com.lovelive.dreamycolor.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "character_cards")
data class CharacterCard(
    @PrimaryKey(autoGenerate = true) // 设置自动生成主键
    val id: Int = 0,
    val name: String,
    val japaneseName: String,
    val birthday: String,
    val schoolYear: String,
    val height: Int,
    val bloodType: String = "",
    val hobby: String = "",
    val description: String,
    val imageRes: String = ""
) {
    // 建议添加无参构造函数以兼容某些序列化场景
    constructor() : this(0, "", "", "", "", 0, "", "", "", "")
}
