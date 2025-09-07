package com.zhoujh.aichat.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zhoujh.aichat.database.dao.AICharacterDao
import com.zhoujh.aichat.database.dao.AIChatMemoryDao
import com.zhoujh.aichat.database.dao.ChatMessageDao
import com.zhoujh.aichat.database.dao.TempChatMessageDao
import com.zhoujh.aichat.database.entity.AICharacter
import com.zhoujh.aichat.database.entity.AIChatMemory
import com.zhoujh.aichat.database.entity.ChatMessage
import com.zhoujh.aichat.database.entity.TempChatMessage

@Database(
    entities = [
        AICharacter::class,
        ChatMessage::class,
        TempChatMessage::class,
        AIChatMemory::class,
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun aiCharacterDao(): AICharacterDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun aiChatMemoryDao(): AIChatMemoryDao
    abstract fun tempChatMessageDao(): TempChatMessageDao

    companion object {
        // 单例模式防止多次实例化数据库
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_chat_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}