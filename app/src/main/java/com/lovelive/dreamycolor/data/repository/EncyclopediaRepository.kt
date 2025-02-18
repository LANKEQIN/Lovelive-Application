package com.lovelive.dreamycolor.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lovelive.dreamycolor.database.dao.EncyclopediaDao
import com.lovelive.dreamycolor.model.CharacterCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class EncyclopediaRepository(private val dao: EncyclopediaDao) {
    // 获取所有角色流
    fun getAllCharacters() = dao.getAllCharacters()

    // 添加角色
    suspend fun addCharacter(character: CharacterCard) {
        dao.insertCharacter(character)
    }

    fun getCharacter(name: String) = dao.getCharacterByName(name)

    suspend fun initializeFromAssets(context: Context) {
        withContext(Dispatchers.IO) {
            // 仅当数据库为空时才初始化
            if (dao.getAllCharacters().first().isEmpty()) {
                try {
                    val jsonString = context.assets.open("characters.json")
                        .bufferedReader()
                        .use { it.readText() }

                    val type = object : TypeToken<List<CharacterCard>>() {}.type
                    val characters = Gson().fromJson<List<CharacterCard>>(jsonString, type)

                    characters?.forEach { dao.insertCharacter(it) }
                } catch (e: Exception) {
                    Log.e("Repository", "Error reading JSON: ${e.message}")
                }
            }
        }
    }

    // 新增：手动刷新数据，不受数据库是否为空的限制
    suspend fun refreshData(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                // 清空原有数据
                dao.deleteAllCharacters()

                // 从 assets 中读取 JSON 文件
                val jsonString = context.assets.open("characters.json")
                    .bufferedReader()
                    .use { it.readText() }

                val type = object : TypeToken<List<CharacterCard>>() {}.type
                val characters = Gson().fromJson<List<CharacterCard>>(jsonString, type)

                // 将 JSON 数据插入数据库
                characters?.forEach { dao.insertCharacter(it) }
                Log.d("Repository", "Data refreshed successfully")
            } catch (e: Exception) {
                Log.e("Repository", "Error refreshing data: ${e.message}")
            }
        }
    }
}
