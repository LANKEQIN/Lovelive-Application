package com.lovelive.dreamycolor.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lovelive.dreamycolor.data.repository.EncyclopediaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EncyclopediaViewModel(
    application: Application,
    private val repository: EncyclopediaRepository
) : AndroidViewModel(application) {
    // 获取所有角色数据
    val allCharacters = repository.getAllCharacters()

    init {
        viewModelScope.launch {
            if (shouldInitializeData()) {
                repository.initializeFromAssets(getApplication<Application>().applicationContext)
            }
        }
    }

    private suspend fun shouldInitializeData(): Boolean {
        return repository.getAllCharacters().first().isEmpty()
    }

    // 获取单个角色（原有功能）
    fun getCharacter(name: String) = repository.getCharacter(name)

    // 新增：手动刷新数据方法，供 UI 层调用
    fun refreshData(context: Context) {
        viewModelScope.launch {
            repository.refreshData(context)
        }
    }
}
