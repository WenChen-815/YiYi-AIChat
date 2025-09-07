package com.zhoujh.aichat.app.manager

import android.util.Log
import com.zhoujh.aichat.app.AppContext
import com.zhoujh.aichat.database.entity.AICharacter
import com.zhoujh.aichat.database.entity.AIChatMemory
import com.zhoujh.aichat.database.entity.ChatMessage
import com.zhoujh.aichat.network.model.Message
import com.zhoujh.aichat.database.entity.MessageType
import com.zhoujh.aichat.database.entity.TempChatMessage
import com.zhoujh.aichat.network.ApiService
import com.zhoujh.aichat.utils.ChatUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object AIChatManager {
    private val TAG = "AIChatManager"
    private val listeners = Collections.synchronizedSet(mutableSetOf<AIChatMessageListener>())
    private lateinit var configManager: ConfigManager
    private lateinit var apiKey: String
    private lateinit var baseUrl: String
    private lateinit var selectedModel: String
    private val chatMessageDao = AppContext.appDatabase.chatMessageDao()
    private val tempChatMessageDao = AppContext.appDatabase.tempChatMessageDao()
    private val aiChatMemoryDao = AppContext.appDatabase.aiChatMemoryDao()
    private lateinit var apiService: ApiService
    // 添加一个 ConcurrentHashMap 来跟踪每个 aiCharacter 的总结操作状态
    private val summarizingInProgress = ConcurrentHashMap<String, Boolean>()

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
    suspend fun sendMessage(
        aiCharacter: AICharacter?,
        newMessage: String,
        oldMessages: List<TempChatMessage>
    ){
        // 添加用户消息到列表
        if (aiCharacter == null) {
            Log.e(TAG, "未选择AI角色")
            return
        }
        val currentDate =
            SimpleDateFormat("yyyy-MM-dd EEEE HH:mm:ss", Locale.getDefault()).format(Date())
        val currentUserName = "[${AppContext.USER_NAME}]"
        var newContent = "${currentDate}|${currentUserName}$newMessage"
        val userMessage = ChatMessage(
            id = "${aiCharacter.aiCharacterId}:${System.currentTimeMillis()}",
            content = newContent,
            type = MessageType.USER,
            characterId = aiCharacter.aiCharacterId,
            chatUserId = AppContext.USER_ID
        )

        // 构建消息列表
        val messages = mutableListOf<Message>()

        // 添加系统提示消息
        val prompt = aiCharacter.prompt
        val history = aiChatMemoryDao.getByCharacterId(aiCharacter.aiCharacterId)?.content
        if (prompt.isNotEmpty()) {
            messages.add(Message("system", "$prompt\n以下为角色记忆:\n$history"))
        }
        // 添加历史消息
        for (message in oldMessages) {
            when (message.type) {
                MessageType.SYSTEM -> {
                    messages.add(Message("system", message.content))
                }

                MessageType.USER -> {
                    messages.add(Message("user", message.content))
                }

                MessageType.AI -> {
                    messages.add(Message("assistant", ChatUtil.parseMessage(message)))
                }
            }
        }
        // 添加新消息
        messages.add(Message("user", newContent))
        // 保存用户消息到数据库
        chatMessageDao.insertMessage(userMessage)
        tempChatMessageDao.insert(
            TempChatMessage(
                id = "${aiCharacter.aiCharacterId}:${System.currentTimeMillis()}",
                content = newContent,
                type = MessageType.USER,
                characterId = aiCharacter.aiCharacterId,
                chatUserId = AppContext.USER_ID
            )
        )
        // 通知所有监听器消息已发送
        CoroutineScope(Dispatchers.Main).launch {
            // 通知所有监听器消息已接收
            listeners.forEach { it.onMessageSent(userMessage) }
        }
        Log.i(TAG,"用户 调用总结")
        summarize(aiCharacter)
        send(aiCharacter, messages)
    }

    suspend fun send(
        aiCharacter: AICharacter?,
//        newMessage: String,
//        oldMessages: List<TempChatMessage>
        messages: MutableList<Message>
    ) {
        if (aiCharacter == null) {
            Log.e(TAG, "未选择AI角色")
            return
        }
//        val currentDate =
//            SimpleDateFormat("yyyy-MM-dd EEEE HH:mm:ss", Locale.getDefault()).format(Date())
//        val currentUserName = "[${AppContext.USER_NAME}]"
//        var newContent = "${currentDate}|${currentUserName}$newMessage"
//        val userMessage = ChatMessage(
//            id = "${aiCharacter.aiCharacterId}:${System.currentTimeMillis()}",
//            content = newContent,
//            type = MessageType.USER,
//            characterId = aiCharacter.aiCharacterId,
//            chatUserId = AppContext.USER_ID
//        )
//
//        // 构建消息列表
//        val messages = mutableListOf<Message>()
//
//        // 添加系统提示消息
//        val prompt = aiCharacter.prompt
//        if (prompt.isNotEmpty()) {
//            messages.add(Message("system", prompt))
//        }
//        // 添加历史消息
//        for (message in oldMessages) {
//            when (message.type) {
//                MessageType.SYSTEM -> {
//                    messages.add(Message("system", message.content))
//                }
//
//                MessageType.USER -> {
//                    messages.add(Message("user", message.content))
//                }
//
//                MessageType.AI -> {
//                    messages.add(Message("assistant", ChatUtil.parseMessage(message)))
//                }
//            }
//        }
//        // 添加新消息
//        messages.add(Message("user", newContent))
//        // 保存用户消息到数据库
//        chatMessageDao.insertMessage(userMessage)
//        tempChatMessageDao.insert(
//            TempChatMessage(
//                id = "${aiCharacter.aiCharacterId}:${System.currentTimeMillis()}",
//                content = newContent,
//                type = MessageType.USER,
//                characterId = aiCharacter.aiCharacterId,
//                chatUserId = AppContext.USER_ID
//            )
//        )
//        // 通知所有监听器消息已发送
//        CoroutineScope(Dispatchers.Main).launch {
//            // 通知所有监听器消息已接收
//            listeners.forEach { it.onMessageSent(userMessage) }
//        }
//        Log.d(TAG,"历史消息:\n${messages.map { it.content }.joinToString("\n")}")
        // 这里调用网络API发送消息
        apiService.sendMessage(
            messages = messages,
            model = selectedModel,
            onSuccess = { aiResponse ->
                // 收到回复
//                Log.d(TAG,"原始的AI回复:$aiResponse")
                // 获取当前日期 格式为yyyy-MM-dd Monday HH:mm:ss
                val currentDate =
                    SimpleDateFormat("yyyy-MM-dd EEEE HH:mm:ss", Locale.getDefault()).format(Date())
                val currentCharacterName = "[${aiCharacter.name}]"
                val aiMessage = ChatMessage(
                    id = "${aiCharacter.aiCharacterId}:${System.currentTimeMillis()}",
                    content = "$currentDate|${currentCharacterName}$aiResponse",
                    type = MessageType.AI,
                    characterId = aiCharacter.aiCharacterId,
                    chatUserId = AppContext.USER_ID
                )
                // 保存AI消息到数据库 创建一个新的协程来执行挂起函数
                CoroutineScope(Dispatchers.IO).launch {
                    chatMessageDao.insertMessage(aiMessage)
                    tempChatMessageDao.insert(
                        TempChatMessage(
                            id = "${aiCharacter.aiCharacterId}:${System.currentTimeMillis()}",
                            content = "$currentDate|${currentCharacterName}$aiResponse",
                            type = MessageType.AI,
                            characterId = aiCharacter.aiCharacterId,
                            chatUserId = AppContext.USER_ID
                        )
                    )
                    Log.i(TAG,"AI调用总结")
                    summarize(aiCharacter) // 总结
                }
//                Log.d(TAG,"存储的AI回复:${aiMessage.content}")
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

    suspend fun summarize(
        aiCharacter: AICharacter?,
        callback: (() -> Unit)? = null
    ) {
        // 添加用户消息到列表
        if (aiCharacter == null) {
            Log.e(TAG, "summarize: 未选择AI角色")
            return
        }
        // 检查是否已经有总结操作在进行
        val characterId = aiCharacter.aiCharacterId
        if (summarizingInProgress.putIfAbsent(characterId, true) == true) {
            Log.d(TAG, "summarize: 该角色已有总结操作在进行中，跳过此次调用")
            return
        }
        val count = tempChatMessageDao.getCountByCharacterId(aiCharacter.aiCharacterId)
        if(count < 10){
            Log.d(TAG,"总结个蛋 count=$count")
            // 清除总结操作标记
            summarizingInProgress.remove(characterId)
            return
        }
        Log.d(TAG, "summarize: 开始总结 count=$count")
        val allHistory = tempChatMessageDao.getByCharacterId(aiCharacter.aiCharacterId)
        val summaryMessages = allHistory.subList(0, allHistory.size - 5)
        // 构建消息列表
        val messages = mutableListOf<Message>()
        // 添加系统提示消息
        val prompt = aiCharacter.prompt
        val currentDate = SimpleDateFormat("yyyy-MM-dd EEEE HH:mm:ss", Locale.getDefault()).format(Date())
        var summaryRequest =
            """
                # [任务] 作为一个对话总结助手，根据对话内容、角色设定、当前时间，总结对话中的重要信息
                # [当前时间] [$currentDate]
                # [角色设定] 这是一份角色设定,你只需要关注角色本身的性格特点,无视与角色无关联的内容（如输出要求、字数要求、格式要求等）
                ====角色设定====
                $prompt
                ====角色设定结束====
                现在请你代入${aiCharacter.name}的角色并以“我”自称，用中文总结与${AppContext.USER_NAME}的对话，结合代入角色的性格特点，将以下对话片段提取重要信息总结为一段话作为记忆片段(直接回复一段话):
            """.trimIndent()
        // 添加历史消息
        for (message in summaryMessages) {
            summaryRequest += "\n${message.content}"
        }
        messages.add(Message("system", summaryRequest))
        apiService.sendMessage(
            messages = messages,
            model = selectedModel,
            onSuccess = { aiResponse ->
                // 收到回复
                var newMemoryContent =
                    """
                        ## 记忆片段 [${currentDate}]
                        **摘要**:$aiResponse
                    """.trimIndent()
                CoroutineScope(Dispatchers.IO).launch {
                    Log.d(TAG, "总结成功 delete :${summaryMessages.map { it.content }}")
                    // 删除已总结的消息
                    tempChatMessageDao.deleteAll(summaryMessages)
                    var aiMemory = aiChatMemoryDao.getByCharacterId(aiCharacter.aiCharacterId)
                    if (aiMemory == null) {
                        aiMemory = AIChatMemory(
                            characterId = aiCharacter.aiCharacterId,
                            content = newMemoryContent,
                            createdAt = System.currentTimeMillis()
                        )
                    } else {
                        aiMemory.content = if(aiMemory.content.isNotEmpty() == true)"${aiMemory.content}\n\n$newMemoryContent" else newMemoryContent
                    }
                    aiChatMemoryDao.update(aiMemory)
                    // 总结完成后，清除标记
                    summarizingInProgress.remove(characterId)
                    callback?.invoke()
                }
            },
            onError = {
                // 发生错误，也需要清除标记
                summarizingInProgress.remove(characterId)
                // 发生错误
                Log.e(TAG, "summarize: 总结失败")
            }
        )
    }
}