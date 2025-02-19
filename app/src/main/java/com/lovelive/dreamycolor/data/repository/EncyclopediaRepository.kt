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

    fun getAllCharacters() = dao.getAllCharacters()

    fun getCharacter(name: String) = dao.getCharacterByName(name)

    fun getAllVoiceActors() = dao.getAllVoiceActors()

    suspend fun initializeFromAssets(context: Context) {
        withContext(Dispatchers.IO) {
            // 如果角色数据为空，则从 characters.json 初始化数据
            if (dao.getAllCharacters().first().isEmpty()) {
                try {
                    val jsonString = context.assets.open("characters.json")
                        .bufferedReader()
                        .use { it.readText() }
                    val type = object : TypeToken<List<CharacterCard>>() {}.type
                    val characters = Gson().fromJson<List<CharacterCard>>(jsonString, type)
                    characters?.forEach { dao.insertCharacter(it) }
                    Log.d("Repository", "Character data initialized successfully")
                } catch (e: Exception) {
                    Log.e("Repository", "Error reading characters JSON: ${e.message}")
                }
            }
        }
    }

    suspend fun refreshData(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                dao.deleteAllCharacters()
                val jsonString = context.assets.open("characters.json")
                    .bufferedReader()
                    .use { it.readText() }
                val type = object : TypeToken<List<CharacterCard>>() {}.type
                val characters = Gson().fromJson<List<CharacterCard>>(jsonString, type)
                characters?.forEach { dao.insertCharacter(it) }
                Log.d("Repository", "Character data refreshed successfully")
            } catch (e: Exception) {
                Log.e("Repository", "Error refreshing characters data: ${e.message}")
            }
        }
    }

    // 新增：初始化声优数据
    suspend fun initializeVoiceActorsFromAssets(context: Context) {
        withContext(Dispatchers.IO) {
            // 仅当数据库中没有任何声优数据时才初始化
            if (dao.getAllVoiceActors().first().isEmpty()) {
                try {
                    val jsonString = context.assets.open("voice_actors.json")
                        .bufferedReader()
                        .use { it.readText() }
                    val type = object : TypeToken<List<VoiceActorCard>>() {}.type
                    val voiceActors = Gson().fromJson<List<VoiceActorCard>>(jsonString, type)
                    voiceActors?.forEach { dao.insertVoiceActor(it) }
                    Log.d("Repository", "Voice actor data initialized successfully")
                } catch (e: Exception) {
                    Log.e("Repository", "Error reading voice actors JSON: ${e.message}")
                }
            }
        }
    }
}
