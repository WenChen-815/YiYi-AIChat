package com.zhoujh.aichat.app.manager

import com.zhoujh.aichat.database.entity.ChatMessage

interface AIChatMessageListener {
    fun onMessageSent(message: ChatMessage)
    fun onMessageReceived(message: ChatMessage)
    fun onError(error: String)
}