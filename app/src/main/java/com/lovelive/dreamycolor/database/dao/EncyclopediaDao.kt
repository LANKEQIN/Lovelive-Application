package com.lovelive.dreamycolor.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lovelive.dreamycolor.model.CharacterCard
import com.lovelive.dreamycolor.model.VoiceActorCard
import kotlinx.coroutines.flow.Flow

@Dao
interface EncyclopediaDao {
    // 使用 Flow 实现响应式查询
    @Query("SELECT COUNT(*) FROM character_cards")
    suspend fun getCharacterCount(): Int

    @Query("SELECT * FROM character_cards WHERE name = :name LIMIT 1")
    fun getCharacterByName(name: String): Flow<CharacterCard?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterCard)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceActor(voiceActor: VoiceActorCard)

    // 获取所有角色数据
    @Query("SELECT * FROM character_cards")
    fun getAllCharacters(): Flow<List<CharacterCard>>

    // 新增：删除所有角色记录，用于刷新数据时先清空表中的数据
    @Query("DELETE FROM character_cards")
    suspend fun deleteAllCharacters()

    @Query("SELECT * FROM voice_actor_cards")
    fun getAllVoiceActors(): Flow<List<VoiceActorCard>>

}
