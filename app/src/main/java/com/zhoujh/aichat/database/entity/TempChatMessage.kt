package com.zhoujh.aichat.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "temp_chat_messages")
data class TempChatMessage(
    @PrimaryKey val id: String,
    val content: String,
    val type: MessageType,
    val timestamp: Long = System.currentTimeMillis(),
    val characterId: String, // 用于区分不同AI角色的聊天记录
    val chatUserId: String // 用于区分不同用户的聊天记录
)
