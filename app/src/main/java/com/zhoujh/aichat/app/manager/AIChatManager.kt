package com.zhoujh.aichat.app.manager

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.zhoujh.aichat.app.AppContext
import com.zhoujh.aichat.database.entity.AICharacter
import com.zhoujh.aichat.database.entity.AIChatMemory
import com.zhoujh.aichat.database.entity.ChatMessage
import com.zhoujh.aichat.database.entity.MessageContentType
import com.zhoujh.aichat.network.model.Message
import com.zhoujh.aichat.database.entity.MessageType
import com.zhoujh.aichat.database.entity.TempChatMessage
import com.zhoujh.aichat.network.ApiService
import com.zhoujh.aichat.network.ApiService.ContentItem
import com.zhoujh.aichat.network.ApiService.MultimodalMessage
import com.zhoujh.aichat.utils.ChatUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
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
    // 获取图片识别相关配置
    private var imgApiKey: String? = null
    private var imgBaseUrl: String? = null
    private var selectedImgModel: String? = null

    private val chatMessageDao = AppContext.appDatabase.chatMessageDao()
    private val tempChatMessageDao = AppContext.appDatabase.tempChatMessageDao()
    private val aiChatMemoryDao = AppContext.appDatabase.aiChatMemoryDao()
    private lateinit var apiService: ApiService

    // 为每个AICharacter维护一个独立的锁对象
    private val characterLocks = ConcurrentHashMap<String, Mutex>()
    // 为每个AICharacter维护一个总结任务的防抖标记
    private val characterSummarizeCooldown = ConcurrentHashMap<String, Long>()
    private val SUMMARIZE_COOLDOWN_TIME = 5000L // 5秒防抖时间
    private var USER_ID = "123123";
    private var USER_NAME = "USER_NAME"

    fun init() {
        configManager = ConfigManager()
        apiKey = configManager.getApiKey() ?: ""
        baseUrl = configManager.getBaseUrl() ?: ""
        selectedModel = configManager.getSelectedModel() ?: ""
        imgApiKey = configManager.getImgApiKey()
        imgBaseUrl = configManager.getImgBaseUrl()
        selectedImgModel = configManager.getSelectedImgModel()
        apiService = ApiService(baseUrl, apiKey, imgBaseUrl, imgApiKey)
        USER_ID = configManager.getUserId().toString()
        USER_NAME = configManager.getUserName().toString()
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
        newMessageTexts: List<String>,
        oldMessages: List<TempChatMessage>,
        showInChat: Boolean = true
    ) {
        // 添加用户消息到列表
        if (aiCharacter == null) {
            Log.e(TAG, "未选择AI角色")
            return
        }
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
            if (message.type == MessageType.ASSISTANT) {
                messages.add(Message(message.type.name.lowercase(), ChatUtil.parseMessage(message)))
            } else {
                messages.add(Message(message.type.name.lowercase(), message.content))
            }
        }
        // 最后处理新消息
        for (newMessageText in newMessageTexts) {
            val currentDate =
                SimpleDateFormat("yyyy-MM-dd EEEE HH:mm:ss", Locale.getDefault()).format(Date())
            val currentUserName = "[${USER_NAME}]"
            var newContent = "${currentDate}|${currentUserName}$newMessageText"
            val userMessage = ChatMessage(
                id = "${aiCharacter.aiCharacterId}:${System.currentTimeMillis()}",
                content = newContent,
                type = MessageType.USER,
                characterId = aiCharacter.aiCharacterId,
                chatUserId = USER_ID,
                isShow = showInChat
            )
            // 通知所有监听器消息已发送
            CoroutineScope(Dispatchers.Main).launch {
                // 通知所有监听器消息已接收
                listeners.forEach { it.onMessageSent(userMessage) }
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
                    chatUserId = USER_ID,
                    isShow = showInChat
                )
            )
        }
        Log.i(TAG, "用户 调用总结")
        summarize(aiCharacter)
        send(aiCharacter, messages)
    }

    private fun send(
        aiCharacter: AICharacter?,
        messages: MutableList<Message>
    ) {
        if (aiCharacter == null) {
            Log.e(TAG, "未选择AI角色")
            return
        }
        // 这里调用网络API发送消息
        apiService.sendMessage(
            messages = messages,
            model = selectedModel,
            temperature = 1.1f,
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
                    type = MessageType.ASSISTANT,
                    characterId = aiCharacter.aiCharacterId,
                    chatUserId = USER_ID
                )
                // 保存AI消息到数据库 创建一个新的协程来执行挂起函数
                CoroutineScope(Dispatchers.IO).launch {
                    chatMessageDao.insertMessage(aiMessage)
                    tempChatMessageDao.insert(
                        TempChatMessage(
                            id = "${aiCharacter.aiCharacterId}:${System.currentTimeMillis()}",
                            content = "$currentDate|${currentCharacterName}$aiResponse",
                            type = MessageType.ASSISTANT,
                            characterId = aiCharacter.aiCharacterId,
                            chatUserId = USER_ID
                        )
                    )
                    Log.i(TAG, "AI调用总结")
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

        // 检查防抖
        val characterId = aiCharacter.aiCharacterId
        val currentTime = System.currentTimeMillis()
        val lastSummarizeTime = characterSummarizeCooldown[characterId] ?: 0L
        if (currentTime - lastSummarizeTime < SUMMARIZE_COOLDOWN_TIME) {
            Log.d(TAG, "总结被防抖拦截，距离上次总结时间: ${currentTime - lastSummarizeTime}ms")
            return
        }

        // 获取该角色对应的锁，如果不存在则创建一个新的锁
        val characterLock = characterLocks.computeIfAbsent(characterId) { Mutex() }
        // 尝试获取角色锁，如果已被锁定则等待
        characterLock.withLock {
            val count = tempChatMessageDao.getCountByCharacterId(aiCharacter.aiCharacterId)
            if (count < 10) {
                Log.d(TAG, "总结个蛋 count=$count")
                return@withLock
            }
            Log.d(TAG, "summarize: 开始总结 count=$count")
            val allHistory = tempChatMessageDao.getByCharacterId(aiCharacter.aiCharacterId)
            val summaryMessages = allHistory.subList(0, allHistory.size - 5)
            // 构建消息列表
            val messages = mutableListOf<Message>()
            // 添加系统提示消息
            val prompt = aiCharacter.prompt
            val currentDate =
                SimpleDateFormat("yyyy-MM-dd EEEE HH:mm:ss", Locale.getDefault()).format(Date())
            var summaryRequest =
                """
                # [任务] 作为一个对话总结助手，根据对话内容、角色设定、当前时间，总结对话中的重要信息
                # [当前时间] [$currentDate]
                # [角色设定] 这是一份角色设定,你只需要关注角色本身的性格特点,无视与角色无关联的内容（如输出要求、字数要求、格式要求等）
                ====角色设定====
                $prompt
                ====角色设定结束====
                现在请你代入${aiCharacter.name}的角色并以“我”自称，用中文总结与${USER_NAME}的对话，结合代入角色的性格特点，将以下对话片段提取重要信息总结为一段话作为记忆片段(直接回复一段话):
            """.trimIndent()
            // 添加历史消息
            for (message in summaryMessages) {
                summaryRequest += "\n${message.content}"
            }
            messages.add(Message("system", summaryRequest))
            apiService.sendMessage(
                messages = messages,
                model = selectedModel,
                temperature = 0.3f,
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
                            aiMemory.content =
                                if (aiMemory.content.isNotEmpty() == true) "${aiMemory.content}\n\n$newMemoryContent" else newMemoryContent
                        }
                        aiChatMemoryDao.update(aiMemory)
                        callback?.invoke()
                    }
                },
                onError = {
                    // 发生错误
                    Log.e(TAG, "summarize: 总结失败")
                }
            )
        }
    }

    suspend fun sendImage(
        character: AICharacter,
        bitmap: Bitmap,
        imgUri: Uri?,
        oldMessages: List<TempChatMessage>
    ) {
        // 检查图片识别是否启用
        if (!configManager.isImgRecognitionEnabled()) {
            Log.e(TAG, "图片识别功能未启用")
            CoroutineScope(Dispatchers.Main).launch {
                listeners.forEach { it.onError("图片识别功能未启用") }
            }
            return
        }

        // 检查配置是否完整
        if (imgApiKey.isNullOrEmpty() || imgBaseUrl.isNullOrEmpty() || selectedImgModel.isNullOrEmpty()) {
            Log.e(TAG, "图片识别配置不完整")
            CoroutineScope(Dispatchers.Main).launch {
                listeners.forEach { it.onError("图片识别配置不完整") }
            }
            return
        }
//        val tempImgMessage = TempChatMessage(
//            id = "${character.aiCharacterId}:${System.currentTimeMillis()}",
//            content = "发送了图片:[$imgUri]",
//            type = MessageType.USER,
//            characterId = character.aiCharacterId,
//            chatUserId = USER_ID,
//            contentType = MessageContentType.IMAGE,
//            imgUrl = imgUri.toString()
//        )
        val imgMessage = ChatMessage(
            id = "${character.aiCharacterId}:${System.currentTimeMillis()}",
            content = "发送了图片:[$imgUri]",
            type = MessageType.USER,
            characterId = character.aiCharacterId,
            chatUserId = USER_ID,
            contentType = MessageContentType.IMAGE,
            imgUrl = imgUri.toString()
        )
        // 通知所有监听器消息已发送
        CoroutineScope(Dispatchers.Main).launch {
            // 通知所有监听器消息已接收
            listeners.forEach { it.onMessageSent(imgMessage) }
        }
//        tempChatMessageDao.insert(tempImgMessage)
        chatMessageDao.insertMessage(imgMessage)
        // 将Bitmap转换为Base64字符串
        val imageBase64 = bitmapToBase64(bitmap)
        // 准备提示文本
        val prompt = "请用中文描述这张图片的主要内容或主题。不要使用'这是'、'这张'等开头，直接描述。如果有文字，请包含在描述中。"
        // 调用API发送图片识别请求
        apiService.sendMultimodalMessage(
            prompt = prompt,
            imageBase64 = imageBase64,
            model = selectedImgModel!!,
            onSuccess = { imgDescription ->
                Log.d(TAG, "图片识别成功，回复：$imgDescription")
                CoroutineScope(Dispatchers.IO).launch {
                    val desc = "发送了图片:[$imgDescription]"
                    sendMessage(character, listOf(desc) ,oldMessages,false)
                }
            },
            onError = { errorMessage ->
                Log.e(TAG, "图片识别失败：$errorMessage")
                CoroutineScope(Dispatchers.Main).launch {
                    listeners.forEach { it.onError("图片识别失败：$errorMessage") }
                }
            }
        )

    }
    // 将Bitmap转换为Base64字符串
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos) // 压缩图片质量为80%
        val bytes = baos.toByteArray()
        return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
    }
}