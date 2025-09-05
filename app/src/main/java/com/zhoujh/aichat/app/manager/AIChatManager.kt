package com.zhoujh.aichat.app.manager

import android.util.Log
import com.zhoujh.aichat.app.AppContext
import com.zhoujh.aichat.database.entity.AICharacter
import com.zhoujh.aichat.database.entity.ChatMessage
import com.zhoujh.aichat.network.model.Message
import com.zhoujh.aichat.database.entity.MessageType
import com.zhoujh.aichat.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Collections

object AIChatManager {
    private val TAG = "AIChatManager"
    private val listeners = Collections.synchronizedSet(mutableSetOf<AIChatMessageListener>())
    private lateinit var configManager: ConfigManager
    private lateinit var apiKey: String
    private lateinit var baseUrl: String
    private lateinit var selectedModel: String
    private val chatMessageDao = AppContext.appDatabase.chatMessageDao()
    private lateinit var apiService: ApiService

    fun init() {
        configManager = ConfigManager()
        apiKey = configManager.getApiKey() ?: ""
        baseUrl = configManager.getBaseUrl() ?: ""
        selectedModel = configManager.getSelectedModel() ?: ""
        apiService = ApiService(baseUrl, apiKey)
    }

    // 注册监听器
    fun registerListener(listener: AIChatMessageListener) {
        listeners.add(listener)
    }

    // 取消注册监听器
    fun unregisterListener(listener: AIChatMessageListener) {
        listeners.remove(listener)
    }

    // 发送消息 包含数据库操作 以及网络请求 需要在子线程
    suspend fun send(aiCharacter: AICharacter?, message: String, oldMessages: List<ChatMessage>) {
        // 添加用户消息到列表
        if (aiCharacter == null) {
            Log.e(TAG, "send: 未选择AI角色")
            return
        }
        val userMessage = ChatMessage(
            id = "${aiCharacter.aiCharacterId}:${System.currentTimeMillis()}",
            content = message,
            type = MessageType.USER,
            characterId = aiCharacter.aiCharacterId,
            chatUserId = AppContext.USER_ID
        )
        // 保存用户消息到数据库
        chatMessageDao.insertMessage(userMessage)
        // 通知所有监听器消息已发送
        listeners.forEach { it.onMessageSent(userMessage) }

        // 构建消息列表
        val messages = mutableListOf<Message>()

        // 添加系统提示消息
        val prompt = aiCharacter.prompt
        if (prompt.isNotEmpty()) {
            messages.add(Message("system", prompt))
        }
        // 添加历史消息
        for (message in oldMessages) {
            val role = when(message.type) {
                MessageType.USER -> "user"
                MessageType.AI -> "assistant"
                MessageType.SYSTEM -> "system"
            }
            messages.add(Message(role, message.content))
        }
        // 添加新消息
        messages.add(Message("user", message))

        // 这里调用网络API发送消息
        apiService.sendMessage(
            messages = messages,
            model = selectedModel,
            onSuccess = { aiResponse ->
                // 收到回复
                val aiMessage = ChatMessage(
                    id = "${aiCharacter.aiCharacterId}:${System.currentTimeMillis()}",
                    content = aiResponse,
                    type = MessageType.AI,
                    characterId = aiCharacter.aiCharacterId,
                    chatUserId = AppContext.USER_ID
                )
                // 保存AI消息到数据库 创建一个新的协程来执行挂起函数
                CoroutineScope(Dispatchers.IO).launch {
                    chatMessageDao.insertMessage(aiMessage)
                }
                CoroutineScope(Dispatchers.Main).launch {
                    // 通知所有监听器消息已接收
                    listeners.forEach { it.onMessageReceived(aiMessage) }
                }
            },
            onError = { errorMessage ->
                // 发生错误
                CoroutineScope(Dispatchers.Main).launch {
                    listeners.forEach { it.onError(errorMessage) }
                }
            }
        )
    }
}