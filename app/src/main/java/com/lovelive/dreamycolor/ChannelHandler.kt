package com.lovelive.dreamycolor

import android.content.Context
import android.util.Log
import com.lovelive.dreamycolor.database.EncyclopediaDatabase
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

/**
 * MethodChannel处理类，负责处理Flutter与Android之间的通信
 * @property context Android上下文
 * @property channel MethodChannel实例
 */
class ChannelHandler(private val context: Context, private val channel: MethodChannel) {
    
    /**
     * 设置方法调用处理器
     */
    fun setupMethodCallHandler() {
        channel.setMethodCallHandler { call, result ->
            when (call.method) {
                "sendMessage" -> {
                    val message = call.argument<String>("message")
                    handleFlutterMessage(message, result)
                }
                
                "getEncyclopediaData" -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val data = getEncyclopediaData()
                            result.success(data)
                        } catch (e: Exception) {
                            result.error("DATA_ERROR", "Failed to get encyclopedia data", e.message)
                        }
                    }
                }

                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    /**
     * 处理来自Flutter的消息
     */
    private fun handleFlutterMessage(message: String?, result: MethodChannel.Result) {
        message?.let {
            Log.d("Flutter Message", "Received message from Flutter: $it")
            result.success("Android received: $it")
        } ?: result.error("INVALID_MESSAGE", "Message was null", null)
    }
    
    /**
     * 获取百科数据
     */
    private suspend fun getEncyclopediaData(): Map<String, List<Any>> {
        // 从Room数据库获取Encyclopedia实例
        val database = androidx.room.Room.databaseBuilder(
            context.applicationContext,
            EncyclopediaDatabase::class.java,
            "encyclopedia.db"
        ).build()
        // 需要先导入EncyclopediaRepository类
        val repository = com.lovelive.dreamycolor.data.repository.EncyclopediaRepository(database.encyclopediaDao())
        
        val characters = repository.getAllCharacters().first()
        val voiceActors = repository.getAllVoiceActors().first()
        
        return mapOf(
            "characters" to characters,
            "voiceActors" to voiceActors
        )
    }
}