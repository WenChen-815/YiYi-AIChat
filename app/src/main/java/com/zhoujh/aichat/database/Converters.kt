package com.zhoujh.aichat.database

import androidx.room.TypeConverter
import com.zhoujh.aichat.database.entity.MessageContentType
import com.zhoujh.aichat.database.entity.MessageType

class Converters {

    @TypeConverter
    fun fromMessageType(type: MessageType): String {
        return type.name
    }

    @TypeConverter
    fun toMessageType(type: String): MessageType {
        return MessageType.valueOf(type)
    }

    @TypeConverter
    fun fromMessageContentType(type: MessageContentType): String {
        return type.name
    }

    @TypeConverter
    fun toMessageContentType(type: String): MessageContentType {
        return MessageContentType.valueOf(type)
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): Long {
        return value ?: System.currentTimeMillis()
    }
}