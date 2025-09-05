package com.zhoujh.aichat.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.zhoujh.aichat.database.entity.ChatMessage

@Dao
interface ChatMessageDao {
    // 插入新消息
    @Insert
    suspend fun insertMessage(message: ChatMessage): Long

    // 根据角色ID查询所有聊天记录
//    @Query("SELECT * FROM chat_messages WHERE characterId = :characterId ORDER BY timestamp ASC")
//    fun getMessagesByCharacterId(characterId: String): Flow<List<ChatMessage>>
    @Query("SELECT * FROM chat_messages WHERE characterId = :characterId ORDER BY timestamp ASC")
    fun getMessagesByCharacterId(characterId: String): List<ChatMessage>

    // 分页查询，按时间倒序排列（最新的在前面）
    @Query("SELECT * FROM chat_messages WHERE characterId = :characterId ORDER BY timestamp DESC LIMIT :pageSize OFFSET :offset")
    suspend fun getMessagesByPage(characterId: String, pageSize: Int, offset: Int): List<ChatMessage>

    // 获取消息总数，用于判断是否还有更多数据
    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun getTotalMessageCount(): Int

    // 删除指定角色的所有聊天记录
    @Query("DELETE FROM chat_messages WHERE characterId = :characterId")
    suspend fun deleteMessagesByCharacterId(characterId: String)

    // 清空所有聊天记录
    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()
}