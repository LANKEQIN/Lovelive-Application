package com.lovelive.dreamycolor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lovelive.dreamycolor.database.dao.EncyclopediaDao
import com.lovelive.dreamycolor.model.CharacterCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class EncyclopediaViewModel(private val dao: EncyclopediaDao) : ViewModel() {

    // 修改为 Flow 以便观察数据变化
    fun getCharacter(name: String): Flow<CharacterCard?> = dao.getCharacterByName(name)

    // 插入测试数据
    fun addTestData() {
        viewModelScope.launch {
            val sampleCharacter = CharacterCard(
                name = "高坂穗乃果",
                japaneseName = "高坂 穂乃果",
                birthday = "8月3日",
                description = "音乃木坂学院校园偶像团体μ's的发起人兼队长，热情开朗的元气少女。",
                imageRes = ""
            )
            dao.insertCharacter(sampleCharacter)
        }
    }
}
