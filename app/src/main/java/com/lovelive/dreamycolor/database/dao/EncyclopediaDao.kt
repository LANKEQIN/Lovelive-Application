package com.lovelive.dreamycolor.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lovelive.dreamycolor.model.CharacterCard
import kotlinx.coroutines.flow.Flow

@Dao
interface EncyclopediaDao {
    // 使用 Flow 实现响应式查询
    @Query("SELECT * FROM character_cards WHERE name = :name LIMIT 1")
    fun getCharacterByName(name: String): Flow<CharacterCard?> // 修改为Flow

    @Insert(onConflict = OnConflictStrategy.REPLACE) // 添加冲突处理策略
    suspend fun insertCharacter(character: CharacterCard)

    // 可选：添加基础查询方法
    @Query("SELECT * FROM character_cards")
    fun getAllCharacters(): Flow<List<CharacterCard>>
}
