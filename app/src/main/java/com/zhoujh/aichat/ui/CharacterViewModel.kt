package com.zhoujh.aichat.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import com.zhoujh.aichat.model.AICharacter
import com.zhoujh.aichat.repository.AICharacterRepository
import kotlinx.coroutines.flow.Flow

class CharacterViewModel : ViewModel() {
    /**
     * 当前搜索关键字
     */
    private var query: String? = null

    /**
     * 排序方法
     */
    private var order = 0

    /**
     * 通过这个变量控制外界加载数据
     */
    private val _loadData = MutableLiveData<Long>()
    var loadData: LiveData<Long> = _loadData


    fun aiCharacters() : Flow<PagingData<AICharacter>> {
        val r = aiCharacterRepository.aiCharacter(order,query)
        return r
    }

    fun setQuery(query: String?) {
        if (query != "") {
            this.query = query
        }else this.query = null

        setLoadData()
    }

    private fun setLoadData() {
        _loadData.value = System.currentTimeMillis()
    }

    companion object{
        val aiCharacterRepository = AICharacterRepository()
    }
}