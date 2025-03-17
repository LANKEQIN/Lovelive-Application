package com.lovelive.dreamycolor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay

/**
 * 资源管理器类，负责预加载和缓存应用中经常使用的资源
 * 使用单例模式确保全局只有一个资源缓存实例
 */
class ResourceManager private constructor(private val context: Context) {
    // 资源加载协程作用域
    private val resourceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    companion object {
        private const val TAG = "ResourceManager"

        // 图片缓存大小 - 使用可用内存的1/8作为缓存
        private val MAX_MEMORY = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        private val CACHE_SIZE = MAX_MEMORY / 8

        @Volatile
        private var INSTANCE: ResourceManager? = null

        fun getInstance(context: Context): ResourceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ResourceManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        // 在Composable函数中获取ResourceManager实例的便捷方法
        @Composable
        fun current(): ResourceManager {
            val context = LocalContext.current
            return getInstance(context)
        }
    }

    // 资源加载状态
    sealed class ResourceState {
        data object Initial : ResourceState()
        data object Loading : ResourceState()
        data object Success : ResourceState()
        data class Error(val message: String) : ResourceState()
    }

    // 资源加载状态流
    private val _resourceState = MutableStateFlow<ResourceState>(ResourceState.Initial)
    val resourceState: StateFlow<ResourceState> = _resourceState.asStateFlow()

    // 预加载任务的Job引用，用于取消
    private var preloadJob: Job? = null

    // 图片资源缓存
    private val imageCache = LruCache<Int, Drawable>(CACHE_SIZE)

    // 字符串资源缓存
    private val stringCache = LruCache<Int, String>(100)

    // 位图缓存
    private val bitmapCache = LruCache<String, Bitmap>(CACHE_SIZE)

    /**
     * 资源加载优先级
     */
    enum class LoadPriority {
        HIGH,   // 高优先级，立即加载
        MEDIUM, // 中优先级，稍后加载
        LOW     // 低优先级，延迟加载
    }

    /**
     * 预加载Drawable资源，支持优先级控制
     * @param resourceIds 要预加载的资源ID列表
     * @param priority 加载优先级，默认为MEDIUM
     * @param batchSize 批量处理的大小，默认为10
     */
    fun preloadDrawables(
        resourceIds: List<Int>,
        priority: LoadPriority = LoadPriority.MEDIUM,
        batchSize: Int = 10
    ) {
        // 取消之前的预加载任务（如果有）
        preloadJob?.cancel()

        preloadJob = resourceScope.launch {
            try {
                _resourceState.value = ResourceState.Loading

                // 根据优先级延迟加载
                when (priority) {
                    LoadPriority.HIGH -> { /* 立即加载，不延迟 */ }
                    LoadPriority.MEDIUM -> delay(100) // 短暂延迟
                    LoadPriority.LOW -> delay(500)    // 较长延迟
                }

                // 批量处理资源加载，避免一次性加载过多资源
                resourceIds.chunked(batchSize).forEach { batch ->
                    // 使用async并行加载每个批次中的资源
                    val deferreds = batch.map { resId ->
                        async {
                            if (imageCache.get(resId) == null && isActive) {
                                try {
                                    val drawable = withContext(Dispatchers.IO) {
                                        ContextCompat.getDrawable(context, resId)
                                    }
                                    drawable?.let {
                                        imageCache.put(resId, it)
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "加载资源 $resId 失败: ${e.message}")
                                }
                            }
                        }
                    }

                    // 等待当前批次完成
                    deferreds.forEach { it.await() }

                    // 检查是否被取消
                    if (!isActive) {
                        _resourceState.value = ResourceState.Initial
                        return@launch
                    }
                }

                if (isActive) {
                    _resourceState.value = ResourceState.Success
                    Log.d(TAG, "预加载${resourceIds.size}个Drawable资源完成")
                }
            } catch (e: Exception) {
                if (isActive) {
                    _resourceState.value = ResourceState.Error(e.message ?: "预加载资源失败")
                    Log.e(TAG, "预加载资源失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 预加载字符串资源，支持优先级控制
     * @param stringIds 要预加载的字符串资源ID列表
     * @param priority 加载优先级，默认为MEDIUM
     * @param batchSize 批量处理的大小，默认为20
     */
    fun preloadStrings(
        stringIds: List<Int>,
        priority: LoadPriority = LoadPriority.MEDIUM,
        batchSize: Int = 20
    ) {
        resourceScope.launch {
            try {
                // 根据优先级延迟加载
                when (priority) {
                    LoadPriority.HIGH -> { /* 立即加载，不延迟 */ }
                    LoadPriority.MEDIUM -> delay(200) // 短暂延迟
                    LoadPriority.LOW -> delay(800)    // 较长延迟
                }

                // 批量处理资源加载
                stringIds.chunked(batchSize).forEach { batch ->
                    val deferreds = batch.map { resId ->
                        async {
                            if (stringCache.get(resId) == null && isActive) {
                                try {
                                    val string = withContext(Dispatchers.IO) {
                                        context.getString(resId)
                                    }
                                    stringCache.put(resId, string)
                                } catch (e: Exception) {
                                    Log.w(TAG, "加载字符串资源 $resId 失败: ${e.message}")
                                }
                            }
                        }
                    }

                    // 等待当前批次完成
                    deferreds.forEach { it.await() }

                    // 检查是否被取消
                    if (!isActive) return@launch
                }

                if (isActive) {
                    Log.d(TAG, "预加载${stringIds.size}个字符串资源完成")
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.e(TAG, "预加载字符串资源失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 取消所有正在进行的资源加载任务
     */
    fun cancelLoading() {
        preloadJob?.cancel()
        preloadJob = null
        _resourceState.value = ResourceState.Initial
        Log.d(TAG, "已取消所有资源加载任务")
    }

    /**
     * 获取Drawable资源，优先从缓存获取
     * @param resId 资源ID
     * @param loadAsync 是否异步加载（缓存未命中时）
     * @return Drawable对象，如果资源不存在则返回null
     */
    fun getDrawable(resId: Int, loadAsync: Boolean = false): Drawable? {
        // 先从缓存获取
        val drawable = imageCache.get(resId)

        // 缓存未命中则从资源加载并缓存
        if (drawable == null) {
            if (loadAsync) {
                // 异步加载并缓存，但当前返回null
                resourceScope.launch {
                    try {
                        val asyncDrawable = withContext(Dispatchers.IO) {
                            ContextCompat.getDrawable(context, resId)
                        }
                        asyncDrawable?.let {
                            imageCache.put(resId, it)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "异步加载Drawable资源 $resId 失败: ${e.message}")
                    }
                }
                return null
            } else {
                // 同步加载并缓存
                return try {
                    val syncDrawable = ContextCompat.getDrawable(context, resId)
                    syncDrawable?.let {
                        imageCache.put(resId, it)
                    }
                    syncDrawable
                } catch (e: Exception) {
                    Log.w(TAG, "同步加载Drawable资源 $resId 失败: ${e.message}")
                    null
                }
            }
        }

        return drawable
    }

    /**
     * 异步获取Drawable资源，返回结果通过回调函数
     * @param resId 资源ID
     * @param callback 加载完成后的回调函数
     */
    fun getDrawableAsync(resId: Int, callback: (Drawable?) -> Unit) {
        // 先从缓存获取
        val drawable = imageCache.get(resId)

        if (drawable != null) {
            // 缓存命中，直接回调
            callback(drawable)
        } else {
            // 缓存未命中，异步加载
            resourceScope.launch {
                try {
                    val asyncDrawable = withContext(Dispatchers.IO) {
                        ContextCompat.getDrawable(context, resId)
                    }
                    asyncDrawable?.let {
                        imageCache.put(resId, it)
                    }
                    // 在主线程中回调结果
                    withContext(Dispatchers.Main) {
                        callback(asyncDrawable)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "异步加载Drawable资源 $resId 失败: ${e.message}")
                    withContext(Dispatchers.Main) {
                        callback(null)
                    }
                }
            }
        }
    }

    /**
     * 获取字符串资源，优先从缓存获取
     * @param resId 资源ID
     * @param loadAsync 是否异步加载（缓存未命中时）
     * @return 字符串内容，如果异步加载则返回空字符串
     */
    fun getString(resId: Int, loadAsync: Boolean = false): String {
        // 先从缓存获取
        val string = stringCache.get(resId)

        // 缓存未命中则从资源加载并缓存
        if (string == null) {
            if (loadAsync) {
                // 异步加载并缓存，但当前返回空字符串
                resourceScope.launch {
                    try {
                        val asyncString = withContext(Dispatchers.IO) {
                            context.getString(resId)
                        }
                        stringCache.put(resId, asyncString)
                    } catch (e: Exception) {
                        Log.w(TAG, "异步加载字符串资源 $resId 失败: ${e.message}")
                    }
                }
                return ""
            } else {
                // 同步加载并缓存
                return try {
                    val syncString = context.getString(resId)
                    stringCache.put(resId, syncString)
                    syncString
                } catch (e: Exception) {
                    Log.w(TAG, "同步加载字符串资源 $resId 失败: ${e.message}")
                    ""
                }
            }
        }

        return string
    }

    /**
     * 异步获取字符串资源，返回结果通过回调函数
     * @param resId 资源ID
     * @param callback 加载完成后的回调函数
     */
    fun getStringAsync(resId: Int, callback: (String) -> Unit) {
        // 先从缓存获取
        val string = stringCache.get(resId)

        if (string != null) {
            // 缓存命中，直接回调
            callback(string)
        } else {
            // 缓存未命中，异步加载
            resourceScope.launch {
                try {
                    val asyncString = withContext(Dispatchers.IO) {
                        context.getString(resId)
                    }
                    stringCache.put(resId, asyncString)
                    // 在主线程中回调结果
                    withContext(Dispatchers.Main) {
                        callback(asyncString)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "异步加载字符串资源 $resId 失败: ${e.message}")
                    withContext(Dispatchers.Main) {
                        callback("")
                    }
                }
            }
        }
    }

    /**
     * 缓存位图
     * @param key 缓存键
     * @param bitmap 要缓存的位图
     * @param async 是否异步执行缓存操作
     */
    fun cacheBitmap(key: String, bitmap: Bitmap, async: Boolean = false) {
        if (async) {
            resourceScope.launch {
                bitmapCache.put(key, bitmap)
            }
        } else {
            bitmapCache.put(key, bitmap)
        }
    }

    /**
     * 获取缓存的位图
     * @param key 缓存键
     * @param loadCallback 如果缓存未命中，可以通过此回调提供位图加载逻辑
     * @return 缓存的位图，如果不存在则返回null
     */
    fun getCachedBitmap(key: String, loadCallback: (() -> Bitmap?)? = null): Bitmap? {
        val bitmap = bitmapCache.get(key)

        if (bitmap == null && loadCallback != null) {
            // 缓存未命中且提供了加载回调，则尝试加载
            resourceScope.launch {
                try {
                    val loadedBitmap = withContext(Dispatchers.IO) {
                        loadCallback.invoke()
                    }
                    loadedBitmap?.let {
                        bitmapCache.put(key, it)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "加载位图 $key 失败: ${e.message}")
                }
            }
        }

        return bitmap
    }

    /**
     * 异步获取缓存的位图，返回结果通过回调函数
     * @param key 缓存键
     * @param loadCallback 如果缓存未命中，通过此回调提供位图加载逻辑
     * @param resultCallback 加载完成后的回调函数
     */
    fun getCachedBitmapAsync(
        key: String,
        loadCallback: (() -> Bitmap?)? = null,
        resultCallback: (Bitmap?) -> Unit
    ) {
        val bitmap = bitmapCache.get(key)

        if (bitmap != null) {
            // 缓存命中，直接回调
            resultCallback(bitmap)
        } else if (loadCallback != null) {
            // 缓存未命中且提供了加载回调，异步加载
            resourceScope.launch {
                try {
                    val loadedBitmap = withContext(Dispatchers.IO) {
                        loadCallback.invoke()
                    }
                    loadedBitmap?.let {
                        bitmapCache.put(key, it)
                    }
                    // 在主线程中回调结果
                    withContext(Dispatchers.Main) {
                        resultCallback(loadedBitmap)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "异步加载位图 $key 失败: ${e.message}")
                    withContext(Dispatchers.Main) {
                        resultCallback(null)
                    }
                }
            }
        } else {
            // 缓存未命中且没有提供加载回调
            resultCallback(null)
        }
    }

    /**
     * 预热资源 - 在应用启动时调用，预加载关键资源
     * @param drawableIds 要预加载的Drawable资源ID列表
     * @param stringIds 要预加载的字符串资源ID列表
     */
    fun warmUpResources(
        drawableIds: List<Int> = emptyList(),
        stringIds: List<Int> = emptyList()
    ) {
        if (drawableIds.isNotEmpty()) {
            // 高优先级加载关键Drawable资源
            preloadDrawables(drawableIds.take(5), LoadPriority.HIGH, 5)

            // 如果有更多资源，则以中等优先级加载
            if (drawableIds.size > 5) {
                preloadDrawables(drawableIds.drop(5), LoadPriority.MEDIUM)
            }
        }

        if (stringIds.isNotEmpty()) {
            // 高优先级加载关键字符串资源
            preloadStrings(stringIds.take(10), LoadPriority.HIGH, 10)

            // 如果有更多资源，则以中等优先级加载
            if (stringIds.size > 10) {
                preloadStrings(stringIds.drop(10), LoadPriority.MEDIUM)
            }
        }
    }

    /**
     * 清除所有缓存
     * @param async 是否异步执行清除操作
     */
    fun clearAllCaches(async: Boolean = false) {
        if (async) {
            resourceScope.launch {
                imageCache.evictAll()
                stringCache.evictAll()
                bitmapCache.evictAll()
                Log.d(TAG, "已异步清除所有资源缓存")
            }
        } else {
            imageCache.evictAll()
            stringCache.evictAll()
            bitmapCache.evictAll()
            Log.d(TAG, "已清除所有资源缓存")
        }
    }

    /**
     * 清除特定类型的缓存
     * @param clearDrawables 是否清除Drawable缓存
     * @param clearStrings 是否清除字符串缓存
     * @param clearBitmaps 是否清除位图缓存
     * @param async 是否异步执行清除操作
     */
    fun clearCaches(
        clearDrawables: Boolean = true,
        clearStrings: Boolean = true,
        clearBitmaps: Boolean = true,
        async: Boolean = false
    ) {
        val clearAction = {
            if (clearDrawables) imageCache.evictAll()
            if (clearStrings) stringCache.evictAll()
            if (clearBitmaps) bitmapCache.evictAll()

            Log.d(TAG, "已清除选定的资源缓存")
        }

        if (async) {
            resourceScope.launch {
                clearAction()
            }
        } else {
            clearAction()
        }
    }

    /**
     * 获取当前缓存使用情况
     * @return 缓存使用信息字符串
     */
    fun getCacheStats(): String {
        return "图片缓存: ${imageCache.size()}/${imageCache.maxSize()}, " +
                "字符串缓存: ${stringCache.size()}/${stringCache.maxSize()}, " +
                "位图缓存: ${bitmapCache.size()}/${bitmapCache.maxSize()}"
    }

    /**
     * 释放资源管理器资源
     * 在不再需要ResourceManager时调用，例如在Activity或Fragment的onDestroy中
     */
    fun release() {
        cancelLoading()
        resourceScope.cancel()
        Log.d(TAG, "ResourceManager已释放")
    }
}