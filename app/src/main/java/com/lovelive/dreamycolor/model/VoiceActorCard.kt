package com.lovelive.dreamycolor.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lovelive.dreamycolor.R

@Entity(tableName = "voice_actor_cards")
data class VoiceActorCard(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val name: String,
    val japaneseName: String,
    val birthday: String,
    val agency: String, // 事务所
    val height: Int,
    val bloodType: String,
    val hobby: String,
    val description: String,
    val coefficient: String,
    val imageRes: String
) {
    constructor() : this(0, "", "", "", "", 0, "", "", "", "", "")
}
