package com.zhoujh.aichat.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_characters")
data class AICharacter(
    @PrimaryKey val aiCharacterId: String,
    val name: String,
    val prompt: String,
    val userId: String,
    val createdAt: Long,
    val avatarPath: String?,
    val backgroundPath: String?,
)