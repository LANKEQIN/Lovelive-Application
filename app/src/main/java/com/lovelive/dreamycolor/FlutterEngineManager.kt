package com.lovelive.dreamycolor

import android.content.Context
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugins.GeneratedPluginRegistrant

/**
 * Flutter引擎管理类，负责Flutter引擎的初始化和缓存
 * @property context Android上下文
 * @property engineId 引擎ID，用于缓存标识
 */
class FlutterEngineManager(private val context: Context, private val engineId: String) {
    private lateinit var flutterEngine: FlutterEngine

    /**
     * 初始化并缓存Flutter引擎
     */
    fun initAndCacheEngine() {
        flutterEngine = FlutterEngine(context)
        flutterEngine.dartExecutor.executeDartEntrypoint(DartExecutor.DartEntrypoint.createDefault())
        GeneratedPluginRegistrant.registerWith(flutterEngine)
        FlutterEngineCache.getInstance().put(engineId, flutterEngine)
    }

    /**
     * 获取Flutter引擎实例
     */
    fun getEngine(): FlutterEngine = flutterEngine
}