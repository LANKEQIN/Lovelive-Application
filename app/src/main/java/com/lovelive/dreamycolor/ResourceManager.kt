package com.lovelive.dreamycolor

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.LruCache
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow


/**
 * 资源管理器类，负责预加载和缓存应用中经常使用的资源
 * 使用单例模式确保全局只有一个资源缓存实例
 */
class ResourceManager private constructor(context: Context) {
    // 将context字段改为ApplicationContext，避免内存泄漏
    private val applicationContext = context.applicationContext

    // 资源加载协程作用域
    private val resourceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 资源加载完成状态
    private val _isResourceLoadingComplete = MutableStateFlow(false)
    val isResourceLoadingComplete = _isResourceLoadingComplete.asStateFlow()

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

    // 预加载任务的Job引用，用于取消
    private var preloadJob: Job? = null

    // 图片资源缓存
    private val imageCache = LruCache<Int, Drawable>(CACHE_SIZE)

    // 字符串资源缓存
    private val stringCache = LruCache<Int, String>(100)



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
                    LoadPriority.HIGH -> { /* 立即加载，不延迟 */
                    }

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
                                        ContextCompat.getDrawable(applicationContext, resId)
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
                    LoadPriority.HIGH -> { /* 立即加载，不延迟 */
                    }

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
                                        applicationContext.getString(resId)
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
     * 预加载所有页面资源
     * 该方法会预加载应用中所有页面所需的资源，包括图片和字符串资源
     * 建议在应用启动时调用，以减少页面切换时的加载延迟
     */
    fun preloadAllPageResources() {
        resourceScope.launch {
            try {
                _resourceState.value = ResourceState.Loading
                Log.d(TAG, "开始预加载所有页面资源")
                
                // 预加载导航栏相关资源（高优先级）
                val navigationStringIds = listOf(
                    R.string.navigation_exclusive,
                    R.string.navigation_inspiration,
                    R.string.navigation_encyclopedia,
                    R.string.navigation_profile,
                    R.string.app_name,
                    R.string.splash_text
                )
                preloadStrings(navigationStringIds, LoadPriority.HIGH)
                
                // 预加载专属页面资源（中优先级）
                val exclusiveDrawables = listOf(
                    R.drawable.ic_launcher_background
                    // 添加专属页面的其他图片资源
                )
                preloadDrawables(exclusiveDrawables, LoadPriority.MEDIUM)
                
                // 预加载灵感页面资源（中优先级）
                val inspirationDrawables = listOf<Int>(
                    // 添加灵感页面的图片资源
                )
                if (inspirationDrawables.isNotEmpty()) {
                    preloadDrawables(inspirationDrawables, LoadPriority.MEDIUM)
                }
                
                // 预加载百科页面资源（高优先级，因为这个页面最卡）
                val encyclopediaDrawables = listOf(
                    R.drawable.ic_honoka_head
                    // 添加百科页面的其他图片资源
                )
                preloadDrawables(encyclopediaDrawables, LoadPriority.HIGH)
                
                // 预加载个人资料页面资源（低优先级）
                val profileDrawables = listOf<Int>(
                    // 添加个人资料页面的图片资源
                )
                if (profileDrawables.isNotEmpty()) {
                    preloadDrawables(profileDrawables, LoadPriority.LOW)
                }
                
                // 预加载角色详情页面资源（中优先级）
                val characterDetailDrawables = listOf(
                    R.drawable.ic_honoka_head
                    // 添加角色详情页面的其他图片资源
                )
                preloadDrawables(characterDetailDrawables, LoadPriority.MEDIUM)
                
                // 设置资源加载完成状态
                _resourceState.value = ResourceState.Success
                _isResourceLoadingComplete.value = true
                Log.d(TAG, "所有页面资源预加载完成")
            } catch (e: Exception) {
                _resourceState.value = ResourceState.Error(e.message ?: "预加载资源失败")
                Log.e(TAG, "预加载所有页面资源失败: ${e.message}")
            }
        }
    }
}


