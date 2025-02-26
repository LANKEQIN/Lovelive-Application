package com.lovelive.dreamycolor.utils

import android.content.Context
import com.lovelive.dreamycolor.model.CharacterDetail
import com.lovelive.dreamycolor.model.VoiceActorDetail
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.IOException
import android.util.Log

object DetailJsonUtils {
    private val json = Json { ignoreUnknownKeys = true }
    private const val TAG = "DetailJsonUtils"

    fun loadCharacterDetail(context: Context, fileName: String): CharacterDetail? {
        return try {
            val path = "encyclopedia_details/characters/$fileName"
            Log.d(TAG, "尝试加载角色文件: $path")
            val jsonString = context.assets.open(path).bufferedReader().use { it.readText() }
            Log.d(TAG, "成功读取JSON: ${jsonString.take(100)}...")
            json.decodeFromString<CharacterDetail>(jsonString) // 显式指定类型参数
        } catch (e: IOException) {
            Log.e(TAG, "文件读取错误: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "JSON解析错误: ${e.message}", e) // 添加异常对象以获取完整堆栈跟踪
            null
        }
    }

    fun loadVoiceActorDetail(context: Context, fileName: String): VoiceActorDetail? {
        return try {
            val path = "encyclopedia_details/voice_actors/$fileName"
            Log.d(TAG, "尝试加载声优文件: $path")
            val jsonString = context.assets.open(path).bufferedReader().use { it.readText() }
            Log.d(TAG, "成功读取JSON: ${jsonString.take(100)}...")
            json.decodeFromString<VoiceActorDetail>(jsonString) // 显式指定类型参数
        } catch (e: IOException) {
            Log.e(TAG, "文件读取错误: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "JSON解析错误: ${e.message}", e) // 添加异常对象以获取完整堆栈跟踪
            null
        }
    }
}
