package com.lovelive.dreamycolor.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lovelive.dreamycolor.database.dao.EncyclopediaDao
import com.lovelive.dreamycolor.model.CharacterCard
import com.lovelive.dreamycolor.model.VoiceActorCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class EncyclopediaRepository(private val dao: EncyclopediaDao) {
    companion object {
        private const val DATA_VERSION = 1
        private const val PREFS_NAME = "app_data"
        private const val KEY_DATA_VERSION = "data_version"
    }

    fun getAllCharacters() = dao.getAllCharacters()
    fun getCharacter(name: String) = dao.getCharacterByName(name)
    fun getAllVoiceActors() = dao.getAllVoiceActors()

    suspend fun initializeFromAssets(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                // 检查数据版本
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val currentVersion = prefs.getInt(KEY_DATA_VERSION, 0)

                if (currentVersion < DATA_VERSION || dao.getAllCharacters().first().isEmpty()) {
                    // 初始化角色数据
                    loadCharactersFromJson(context)
                    // 初始化声优数据
                    loadVoiceActorsFromJson(context)

                    // 更新数据版本
                    prefs.edit().putInt(KEY_DATA_VERSION, DATA_VERSION).apply()

                    Log.d("Repository", "数据初始化完成")
                }
            } catch (e: Exception) {
                Log.e("Repository", "初始化数据失败: ${e.message}")
                throw e
            }
        }
    }

    private suspend fun loadCharactersFromJson(context: Context) {
        try {
            val jsonString = context.assets.open("characters.json")
                .bufferedReader()
                .use { it.readText() }
            val type = object : TypeToken<List<CharacterCard>>() {}.type
            val characters = Gson().fromJson<List<CharacterCard>>(jsonString, type)
            characters?.forEach { dao.insertCharacter(it) }
        } catch (e: Exception) {
            Log.e("Repository", "加载角色数据失败: ${e.message}")
            throw e
        }
    }

    private suspend fun loadVoiceActorsFromJson(context: Context) {
        try {
            val jsonString = context.assets.open("voice_actors.json")
                .bufferedReader()
                .use { it.readText() }
            val type = object : TypeToken<List<VoiceActorCard>>() {}.type
            val voiceActors = Gson().fromJson<List<VoiceActorCard>>(jsonString, type)
            voiceActors?.forEach { dao.insertVoiceActor(it) }
        } catch (e: Exception) {
            Log.e("Repository", "加载声优数据失败: ${e.message}")
            throw e
        }
    }

    suspend fun refreshData(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                dao.deleteAllCharacters()
                dao.deleteAllVoiceActors()
                initializeFromAssets(context)
                Log.d("Repository", "数据刷新成功")
            } catch (e: Exception) {
                Log.e("Repository", "刷新数据失败: ${e.message}")
                throw e
            }
        }
    }
}

