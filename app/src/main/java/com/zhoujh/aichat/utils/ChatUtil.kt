package com.zhoujh.aichat.utils

import com.zhoujh.aichat.database.entity.ChatMessage

class ChatUtil {
    companion object{
        fun parseMessage(message: ChatMessage): String{
            var cleanedContent = message.content
            // 解析AI回复中的日期和角色名称
            val dateRegex = Regex("\\d{4}-\\d{2}-\\d{2} \\w+ \\d{2}:\\d{2}:\\d{2}")
            val roleRegex = Regex("\\[.*?\\]")

            val dateMatch = dateRegex.find(message.content)
            val roleMatch = roleRegex.find(message.content)
            if (dateMatch != null && roleMatch != null) {
                // 提取日期和角色名称
                val date = dateMatch.value
                val role = roleMatch.value

                // 移除日期和角色名称，只保留实际回复内容
                cleanedContent = message.content.replace("${date}|$role", "").trim()
            }
            return cleanedContent
        }
    }
}