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
    val dataState: StateFlow<DataState> = _dataState

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
        initializeData(getApplication())
    }

    private fun initializeData(context: Context) {
        viewModelScope.launch {
            try {
                _dataState.value = DataState.Loading
                repository.initializeFromAssets(context)
                _dataState.value = DataState.Success
                Log.d("ViewModel", "数据初始化成功")
            } catch (e: Exception) {
                Log.e("ViewModel", "数据初始化失败: ${e.message}")
                _dataState.value = DataState.Error(e.message ?: "未知错误")
            }
        }
    }

    fun refreshData(context: Context) {
        viewModelScope.launch {
            try {
                _dataState.value = DataState.Loading
                repository.refreshData(context)
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

    override fun onCleared() {
        super.onCleared()
        Log.d("ViewModel", "ViewModel已清理")
    }
}

