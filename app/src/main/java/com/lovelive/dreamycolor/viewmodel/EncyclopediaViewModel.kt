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

class EncyclopediaViewModel(
    application: Application,
    private val repository: EncyclopediaRepository
) : AndroidViewModel(application) {

    // 角色数据流
    val allCharacters: Flow<List<CharacterCard>> = repository.getAllCharacters()
        .catch { e ->
            Log.e("ViewModel", "Error collecting characters: ${e.message}")
            emit(emptyList())
        }

    // 声优数据流
    val allVoiceActors: Flow<List<VoiceActorCard>> = repository.getAllVoiceActors()
        .catch { e ->
            Log.e("ViewModel", "Error collecting voice actors: ${e.message}")
            emit(emptyList())
        }

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 错误状态
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        Log.d("ViewModel", "ViewModel initialized")
        initializeData(getApplication())
    }

    // 初始化数据
    fun initializeData(context: android.content.Context) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.initializeFromAssets(context)
                repository.initializeVoiceActorsFromAssets(context)
                Log.d("ViewModel", "Data initialization completed")
            } catch (e: Exception) {
                Log.e("ViewModel", "Error initializing data: ${e.message}")
                _error.value = "数据初始化失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 刷新数据
    fun refreshData(context: android.content.Context) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // 刷新角色数据
                repository.refreshData(context)
                Log.d("ViewModel", "Character data refreshed")

                // 刷新声优数据
                repository.initializeVoiceActorsFromAssets(context)
                Log.d("ViewModel", "Voice actor data refreshed")
            } catch (e: Exception) {
                Log.e("ViewModel", "Error refreshing data: ${e.message}")
                _error.value = "数据刷新失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 获取单个角色数据
    fun getCharacter(name: String): Flow<CharacterCard?> {
        return repository.getCharacter(name)
            .catch { e ->
                Log.e("ViewModel", "Error getting character: ${e.message}")
                emit(null)
            }
    }

    // 清理资源
    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "ViewModel cleared")
    }

    // 错误处理
    fun clearError() {
        _error.value = null
    }

    // 用于调试的数据验证
    fun validateData() {
        viewModelScope.launch {
            try {
                allCharacters.collect { characters ->
                    Log.d("ViewModel", "Characters count: ${characters.size}")
                }
                allVoiceActors.collect { voiceActors ->
                    Log.d("ViewModel", "Voice actors count: ${voiceActors.size}")
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error validating data: ${e.message}")
            }
        }
    }
}
