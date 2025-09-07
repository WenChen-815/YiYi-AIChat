package com.zhoujh.aichat.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_chat_memory")
data class AIChatMemory (
    @PrimaryKey val characterId: String,
    var content: String = "",
    val createdAt: Long,
)
