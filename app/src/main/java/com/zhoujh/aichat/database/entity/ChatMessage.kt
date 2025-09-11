package com.zhoujh.aichat.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageType {
    USER, ASSISTANT, SYSTEM
}
enum class MessageContentType {
    TEXT, IMAGE, VOICE
}
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey val id: String,
    val content: String,
    val type: MessageType,
    val timestamp: Long = System.currentTimeMillis(),
    val characterId: String, // 用于区分不同AI角色的聊天记录
    val chatUserId: String, // 用于区分不同用户的聊天记录
    val contentType: MessageContentType = MessageContentType.TEXT, // 消息类型，默认是文本
    val imgUrl: String? = null, // 图片消息的URL，非图片消息为null
    val voiceUrl: String? = null, // 语音消息的URL，非语音消息为null
    val isShow: Boolean = true
)
