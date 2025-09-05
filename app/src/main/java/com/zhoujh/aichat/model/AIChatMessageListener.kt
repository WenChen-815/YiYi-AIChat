package com.zhoujh.aichat.model

interface AIChatMessageListener {
    fun onMessageSent(message: ChatMessage)
    fun onMessageReceived(message: ChatMessage)
    fun onError(error: String)
}