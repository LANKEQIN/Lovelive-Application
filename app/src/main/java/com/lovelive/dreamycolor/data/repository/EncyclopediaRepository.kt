package com.lovelive.dreamycolor.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lovelive.dreamycolor.database.dao.EncyclopediaDao
import com.lovelive.dreamycolor.model.CharacterCard
import com.lovelive.dreamycolor.model.VoiceActorCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class EncyclopediaRepository(private val dao: EncyclopediaDao) {
    companion object {
        private const val DATA_VERSION = 2
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
                    // 使用协程作用域并行加载数据
                    coroutineScope {
                        // 并行执行角色和声优数据加载
                        val charactersJob = async { loadCharactersFromJson(context) }
                        val voiceActorsJob = async { loadVoiceActorsFromJson(context) }
                        
                        // 等待两个任务完成
                        charactersJob.await()
                        voiceActorsJob.await()
                    }

                    // 更新数据版本
                    prefs.edit().putInt(KEY_DATA_VERSION, DATA_VERSION).apply()

                    Log.d("Repository", "数据初始化完成")
                } else {
                    Log.d("Repository", "数据已是最新版本，无需初始化")
                }
            } catch (e: Exception) {
                Log.e("Repository", "初始化数据失败: ${e.message}")
                throw e
            }
        }
    }

    private suspend fun loadCharactersFromJson(context: Context) {
        try {
            // 在IO线程中读取JSON文件
            val jsonString = withContext(Dispatchers.IO) {
                context.assets.open("characters.json")
                    .bufferedReader()
                    .use { it.readText() }
            }
            
            // 在Default线程中进行JSON解析（计算密集型操作）
            val type = object : TypeToken<List<CharacterCard>>() {}.type
            val characters = withContext(Dispatchers.Default) {
                Gson().fromJson<List<CharacterCard>>(jsonString, type)
            }
            
            // 批量插入数据库，提高效率
            characters?.let {
                // 分批处理，每批50条数据
                it.chunked(50).forEach { batch ->
                    withContext(Dispatchers.IO) {
                        batch.forEach { character -> 
                            dao.insertCharacter(character)
                        }
                    }
                }
            }
            
            Log.d("Repository", "成功加载${characters?.size ?: 0}个角色数据")
        } catch (e: Exception) {
            Log.e("Repository", "加载角色数据失败: ${e.message}")
            throw e
        }
    }

    private suspend fun loadVoiceActorsFromJson(context: Context) {
        try {
            // 在IO线程中读取JSON文件
            val jsonString = withContext(Dispatchers.IO) {
                context.assets.open("voice_actors.json")
                    .bufferedReader()
                    .use { it.readText() }
            }
            
            // 在Default线程中进行JSON解析（计算密集型操作）
            val type = object : TypeToken<List<VoiceActorCard>>() {}.type
            val voiceActors = withContext(Dispatchers.Default) {
                Gson().fromJson<List<VoiceActorCard>>(jsonString, type)
            }
            
            // 批量插入数据库，提高效率
            voiceActors?.let {
                // 分批处理，每批50条数据
                it.chunked(50).forEach { batch ->
                    withContext(Dispatchers.IO) {
                        batch.forEach { voiceActor -> 
                            dao.insertVoiceActor(voiceActor)
                        }
                    }
                }
            }
            
            Log.d("Repository", "成功加载${voiceActors?.size ?: 0}个声优数据")
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

