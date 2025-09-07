package com.zhoujh.aichat.database.dao

import androidx.room.*
import com.zhoujh.aichat.database.entity.AIChatMemory

@Dao
interface AIChatMemoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: AIChatMemory): Long

    @Update
    suspend fun update(memory: AIChatMemory): Int

    @Delete
    suspend fun delete(memory: AIChatMemory): Int

    @Query("SELECT * FROM ai_chat_memory WHERE characterId = :characterId")
    suspend fun getByCharacterId(characterId: String): AIChatMemory?

    @Query("SELECT * FROM ai_chat_memory")
    suspend fun getAll(): List<AIChatMemory>

    @Query("DELETE FROM ai_chat_memory WHERE characterId = :characterId")
    suspend fun deleteByCharacterId(characterId: String)

    @Query("SELECT COUNT(*) FROM ai_chat_memory")
    suspend fun getCount(): Int
}
