package com.lovelive.dreamycolor.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lovelive.dreamycolor.data.repository.EncyclopediaRepository
import com.lovelive.dreamycolor.model.CharacterCard
import com.lovelive.dreamycolor.model.VoiceActorCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class EncyclopediaViewModel(
    application: Application,
    private val repository: EncyclopediaRepository
) : AndroidViewModel(application) {

    sealed class DataState {
        data object Initial : DataState()
        data object Loading : DataState()
        data object Success : DataState()
        data class Error(val message: String) : DataState()
    }

    private val _dataState = MutableStateFlow<DataState>(DataState.Initial)
    val dataState: StateFlow<DataState> = _dataState.asStateFlow()
    
    // 初始化任务的Job引用，用于取消
    private var initJob: Job? = null
    
    // 是否已经初始化完成
    private var isInitialized = false
    
    // 延迟加载配置
    private val lazyLoadDelay = 500L // 延迟加载时间（毫秒）

    val allCharacters: Flow<List<CharacterCard>> = repository.getAllCharacters()
        .catch { e ->
            Log.e("ViewModel", "获取角色数据失败: ${e.message}")
            emit(emptyList())
        }

    val allVoiceActors: Flow<List<VoiceActorCard>> = repository.getAllVoiceActors()
        .catch { e ->
            Log.e("ViewModel", "获取声优数据失败: ${e.message}")
            emit(emptyList())
        }

    init {
        Log.d("ViewModel", "ViewModel初始化")
        // 使用延迟初始化，减轻应用启动负担
        viewModelScope.launch {
            delay(lazyLoadDelay) // 延迟一段时间再初始化
            initializeData(getApplication())
        }
    }

    private fun initializeData(context: Context) {
        // 如果已经初始化过，则不再重复初始化
        if (isInitialized) return
        
        // 取消之前的初始化任务（如果有）
        initJob?.cancel()
        
        initJob = viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
            try {
                _dataState.value = DataState.Loading
                
                // 在IO线程中执行耗时的初始化操作
                withContext(Dispatchers.IO) {
                    repository.initializeFromAssets(context)
                }
                
                isInitialized = true
                _dataState.value = DataState.Success
                Log.d("ViewModel", "数据初始化成功")
            } catch (e: Exception) {
                Log.e("ViewModel", "数据初始化失败: ${e.message}")
                _dataState.value = DataState.Error(e.message ?: "未知错误")
            }
        }
    }


    fun refreshData(context: Context) {
        // 取消之前的初始化任务（如果有）
        initJob?.cancel()
        
        initJob = viewModelScope.launch {
            try {
                _dataState.value = DataState.Loading
                
                // 在IO线程中执行耗时的刷新操作
                withContext(Dispatchers.IO) {
                    repository.refreshData(context)
                }
                
                isInitialized = true
                _dataState.value = DataState.Success
                Log.d("ViewModel", "数据刷新成功")
            } catch (e: Exception) {
                Log.e("ViewModel", "数据刷新失败: ${e.message}")
                _dataState.value = DataState.Error(e.message ?: "刷新失败")
            }
        }
    }

    fun getCharacter(name: String): Flow<CharacterCard?> {
        return repository.getCharacter(name)
            .catch { e ->
                Log.e("ViewModel", "获取角色失败: ${e.message}")
                emit(null)
            }
    }

    // 优化：使用缓存机制减少重复计算
    private val _charactersByGroup = MutableStateFlow<Map<String, List<CharacterCard>>>(emptyMap())
    private val _voiceActorsByGroup = MutableStateFlow<Map<String, List<VoiceActorCard>>>(emptyMap())

    // 新增：根据团体名称分组角色 - 优化版本
    fun getCharactersByGroup(): Flow<Map<String, List<CharacterCard>>> {
        // 如果缓存为空，则进行一次计算
        if (_charactersByGroup.value.isEmpty()) {
            viewModelScope.launch(Dispatchers.Default) { // 使用Default调度器进行计算密集型操作
                allCharacters.collect { characters ->
                    // 在计算线程中执行分组操作
                    val groupedCharacters = characters.groupBy { getCharacterGroupName(it) }
                    _charactersByGroup.value = groupedCharacters
                }
            }
        }
        return _charactersByGroup
    }

    // 新增：根据团体名称分组声优 - 优化版本
    fun getVoiceActorsByGroup(): Flow<Map<String, List<VoiceActorCard>>> {
        // 如果缓存为空，则进行一次计算
        if (_voiceActorsByGroup.value.isEmpty()) {
            viewModelScope.launch(Dispatchers.Default) { // 使用Default调度器进行计算密集型操作
                allVoiceActors.collect { voiceActors ->
                    // 在计算线程中执行分组操作
                    val groupedVoiceActors = voiceActors.groupBy { getVoiceActorGroupName(it) }
                    _voiceActorsByGroup.value = groupedVoiceActors
                }
            }
        }
        return _voiceActorsByGroup
    }

    // 新增：根据角色获取所属团体名称
    private fun getCharacterGroupName(character: CharacterCard): String {
        return when (character.name) {
            in listOf("高坂穗乃果", "南小鸟", "园田海未", "绚濑绘里", "东条希", "矢泽妮可", "西木野真姬", "小泉花阳", "星空凛") -> "μ's"
            in listOf("高海千歌", "樱内梨子", "渡边曜", "松浦果南", "黑泽黛雅", "小原鞠莉", "津岛善子", "国木田花丸", "黑泽露比") -> "Aqours"
            in listOf("高咲侑", "上原步梦", "中须霞", "樱坂雫", "朝香果林", "宫下爱", "近江彼方", "艾玛·维尔德", "天王寺璃奈", "三船栞子", "钟岚珠", "米雅·泰勒", "优木雪菜") -> "虹咲学园学园偶像同好会"
            in listOf("涩谷香音", "唐可可", "岚千砂都", "平安名堇", "叶月恋", "樱小路希奈子", "米女芽衣", "若菜四季", "鬼塚夏美", "鬼塚冬毬", "薇恩·玛格丽特") -> "Liella!"
            in listOf("日野下花帆", "村野沙耶香", "乙宗梢", "夕雾缀理", "大泽瑠璃乃", "藤岛慈", "百生吟子", "徒町小铃", "安养寺姬芽") -> "莲之空女学院学园偶像俱乐部"
            else -> "其他"
        }
    }

    // 新增：根据声优获取所属团体名称 (需要根据实际情况补充)
    private fun getVoiceActorGroupName(voiceActor: VoiceActorCard): String {
        return when (voiceActor.name) {
            in listOf("新田惠海", "内田彩", "三森铃子", "南条爱乃", "楠田亚衣奈", "德井青空", "Pile", "久保由利香", "饭田里穗") -> "μ's"
            in listOf("伊波杏树", "逢田梨香子", "诹访奈奈香", "小宫有纱", "齐藤朱夏", "小林爱香", "高槻加奈子", "铃木爱奈", "降幡爱") -> "Aqours"
            in listOf("矢野妃菜喜", "大西亚玖璃", "相良茉优", "前田佳织里", "久保田未梦", "村上奈津实", "鬼头明里", "指出毬亚", "田中千惠美", "小泉萌香", "法元明菜", "内田秀", "楠木灯", "林鼓子") ->"虹咲学园学园偶像同好会"
            in listOf("伊达小百合", "Liyuu", "岬奈子", "Payton尚未", "青山渚", "铃原希实", "薮岛朱音", "大熊和奏", "绘森彩", "坂仓花", "结那") -> "Liella!"
            in listOf("榆井希实", "野中心菜", "花宫初奈", "佐佐木琴子", "菅叶和", "月音粉", "樱井阳菜", "叶山风花", "来栖凛") -> "莲之空女学院学园偶像俱乐部"
            else -> "其他"
        }
    }

    /**
     * 强制立即初始化数据
     * 在需要立即获取数据的场景使用
     */
    fun forceInitialize(context: Context) {
        // 取消延迟初始化
        initJob?.cancel()
        
        // 立即初始化
        initializeData(context)
    }
    
    /**
     * 预加载特定角色的数据
     * 在用户可能即将访问特定角色详情时调用
     */
    fun preloadCharacter(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getCharacter(name).collect { /* 仅触发加载，不处理结果 */ }
                Log.d("ViewModel", "预加载角色 $name 数据成功")
            } catch (e: Exception) {
                Log.e("ViewModel", "预加载角色数据失败: ${e.message}")
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // 取消所有正在进行的任务
        initJob?.cancel()
        Log.d("ViewModel", "ViewModel已清理")
    }
}

