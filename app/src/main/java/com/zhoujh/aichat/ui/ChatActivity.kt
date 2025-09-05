package com.zhoujh.aichat.ui

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.zhoujh.aichat.AppContext
import com.zhoujh.aichat.R
import com.zhoujh.aichat.adapter.ChatAdapter
import com.zhoujh.aichat.database.ChatMessageDao
import com.zhoujh.aichat.databinding.ActivityChatBinding
import com.zhoujh.aichat.model.AICharacter
import com.zhoujh.aichat.model.AIChatManager
import com.zhoujh.aichat.model.AIChatMessageListener
import com.zhoujh.aichat.model.ChatMessage
import com.zhoujh.aichat.model.Model
import com.zhoujh.aichat.network.ApiService
import com.zhoujh.aichat.utils.ConfigManager
import com.zhoujh.aichat.utils.limitMutableListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity(), CoroutineScope by MainScope(),
    NavigationView.OnNavigationItemSelectedListener {

    // 常量定义
    private val TAG = "ChatActivity"
    private var MAX_CONTEXT_MESSAGE_SIZE = 10

    // 布局与视图相关变量
    private lateinit var binding: ActivityChatBinding
    private val headerView: View?
        get() {
            val headerView = binding.navView.getHeaderView(0)
            return headerView
        }

    // 适配器与数据列表相关变量
    private lateinit var chatAdapter: ChatAdapter
    private val chatMessages = mutableListOf<ChatMessage>()
    private var chatContext = limitMutableListOf<ChatMessage>(MAX_CONTEXT_MESSAGE_SIZE)

    // 配置与服务相关变量
    private lateinit var configManager: ConfigManager
    private var apiService: ApiService? = null
    private var selectedModel: String = ""
    private var supportedModels: List<Model> = emptyList()

    // 监听器相关变量
    private lateinit var aIChatMessageListener: AIChatMessageListener

    // 数据库相关变量
    private lateinit var chatMessageDao: ChatMessageDao

    // 协程与任务相关变量
    private var currentFlowJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper()) // 主线程Handler用于UI更新

    // 分页与滚动相关变量
    private val PAGE_SIZE = 20
    private var currentPage = 0
    private var isLoading = false
    private var hasMoreData = true

    companion object {
        private const val CHARACTER_SELECT_REQUEST = 1001
    }

    // 角色相关变量
    private lateinit var currentAICharacter: AICharacter
    private var currentCharacterId = ""
    private var currentCharacterName = ""

    // 生命周期方法
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化配置管理器
        configManager = ConfigManager()
        // 检查是否有配置，如果没有则跳转到配置页面
        if (!configManager.hasCompleteConfig()) {
            startActivity(Intent(this, ConfigActivity::class.java))
            finish()
            return
        }

        // 初始化数据库DAO
        chatMessageDao = AppContext.appDatabase.chatMessageDao()

        // 初始化API服务
        initApiService()
        // 初始化聊天适配器
        setupRecyclerView()
        // 初始化监听器
        setupListeners()

        loadSupportedModels()

        // 检查是否有选中的角色
        checkSelectedCharacter()
    }

    override fun onDestroy() {
        super.onDestroy()
        currentFlowJob?.cancel()
        AIChatManager.unregisterListener(aIChatMessageListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CHARACTER_SELECT_REQUEST && resultCode == RESULT_OK) {
            val characterId = data?.getStringExtra("SELECTED_CHARACTER_ID")
            if (characterId != null) {
                // 从数据库加载角色信息
                currentAICharacter =
                    AppContext.appDatabase.aiCharacterDao().getCharacterById(characterId)!!
                currentCharacterId = characterId
                currentCharacterName = currentAICharacter!!.name
                updateCharacterDisplay()
                Toast.makeText(this, "已选择角色：${currentCharacterName}", Toast.LENGTH_SHORT)
                    .show()
                // 加载聊天记录
                loadInitialData()
            }
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // 初始化相关方法
    private fun initApiService() {
        val apiKey = configManager.getApiKey() ?: ""
        val baseUrl = configManager.getBaseUrl() ?: ""
        selectedModel = configManager.getSelectedModel() ?: ""

        apiService = ApiService(baseUrl, apiKey)

        // 更新抽屉头部的当前模型信息
        val tvCurrentModelName = headerView?.findViewById<TextView>(R.id.tvCurrentModelName)
        tvCurrentModelName?.text = selectedModel
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.rvChat.apply {
            this.adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                // 聊天记录通常最新的在底部，这里反转布局使最新消息在底部(傻逼AI教我的, 2025年AI依然无法完全取代我……还是靠自己吧)
//                reverseLayout = true
//                stackFromEnd = true
            }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    // 获取当前可见的第一个item的位置
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    // 当滚动到顶部（考虑到反转布局）且有更多数据且不在加载中时，加载更多
                    if (firstVisibleItemPosition == 0 && hasMoreData && !isLoading) {
                        loadMoreData()
                    }
                }
            })
        }
    }

    private fun setupListeners() {
        aIChatMessageListener = object : AIChatMessageListener {
            override fun onMessageSent(userMessage: ChatMessage) {
                Log.d(TAG, "onMessageSent: $userMessage")
                // 清空输入框
                binding.etMessage.text.clear()
                // 显示加载状态
                binding.progressBar.visibility = View.VISIBLE
                binding.btnSend.isEnabled = false
                if (userMessage.characterId == currentAICharacter?.aiCharacterId) {
                    handleNewMessage(userMessage)
                }
            }

            override fun onMessageReceived(aiMessage: ChatMessage) {
                // 处理成功响应
                Log.d(TAG, "onMessageReceived: $aiMessage")
                // 隐藏加载状态
                binding.progressBar.visibility = View.GONE
                binding.btnSend.isEnabled = true
                if (aiMessage.characterId == currentAICharacter?.aiCharacterId) {
                    handleNewMessage(aiMessage)
                }
            }

            override fun onError(errorMessage: String) {
                Log.e(TAG, "onError: $errorMessage")
                // 处理错误
                Toast.makeText(this@ChatActivity, errorMessage, Toast.LENGTH_LONG).show()
                // 隐藏加载状态
                binding.progressBar.visibility = View.GONE
                binding.btnSend.isEnabled = true
            }
        }
        AIChatManager.registerListener(aIChatMessageListener)

        // 打开抽屉菜单
        binding.ivMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // 设置导航视图的菜单项选中监听器
        binding.navView.setNavigationItemSelectedListener(this)

        // 发送按钮点击事件
        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        // 软键盘回车发送
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }

        // 设置键盘监听器
        setupKeyboardListener()
    }

    private fun setupKeyboardListener() {
        // 获取根视图
        val rootView = window.decorView.findViewById<View>(android.R.id.content)

        // 设置全局布局监听器
        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            // 获取根视图在屏幕上的可见区域
            rootView.getWindowVisibleDisplayFrame(rect)

            // 屏幕高度
            val screenHeight = rootView.rootView.height

            // 计算不可见区域高度（即软键盘高度）
            val keyboardHeight = screenHeight - rect.bottom

            // 当不可见区域高度大于屏幕高度的1/3时，认为软键盘弹出
            if (keyboardHeight > screenHeight * 0.3) {
                // 确保聊天消息列表不为空
                if (chatMessages.isNotEmpty()) {
                    // 使用post方法确保在UI更新完成后执行滚动
                    binding.rvChat.post {
                        val itemCount = chatAdapter.getProcessedItemCount()
                        if (itemCount > 0) {
                            binding.rvChat.scrollToPosition(itemCount - 1)
                        }
                    }
                }
            }
        }

        // 添加布局监听器
        rootView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)

        // 在Activity销毁时移除监听器，避免内存泄漏
        lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    rootView.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
                }
            }
        })
    }

    // 数据处理相关方法
    private fun sendMessage() {
        // 先复制一份聊天上下文，之后发送消息与保存到数据库操作不在同一线程，因为使用了Flow，虽然实现了chatContext也提高了同步的难度，直接临时存一份反而简单
        val tempChatContext = chatContext
        val messageText = binding.etMessage.text.toString().trim()
        if (messageText.isEmpty()) {
            return
        }
        // 发送消息到AI
        launch {
            AIChatManager.send(
                currentAICharacter,
                messageText,
                tempChatContext
            )
        }
    }

    // 获取支持的模型列表
    /*空安全处理： .let 通常与空安全操作符 ?. 结合使用（如 对象?.let { ... }），只有当对象不为 null 时，才会执行 let 代码块内的逻辑。
      变量转换： 在 let 代码块内部，原始对象可以通过参数（通常命名为 it，但也可以自定义名称如示例中的 service）来引用，避免了重复使用原始变量名。
      返回值： let 函数会返回其代码块的最后一个表达式的结果，这使得它可以用于链式调用或结果转换。*/
    private fun loadSupportedModels() {
        apiService?.let { service ->
            /*异步执行：通过launch(Dispatchers.IO)启动一个Kotlin协程，在IO线程池上执行网络请求操作，避免阻塞主线程。*/
            launch(Dispatchers.IO) {
                service.getSupportedModels(
                    onSuccess = { models ->
                        mainHandler.post {
                            supportedModels = models
                        }
                    },
                    onError = { errorMsg ->
                        // 使用Handler在主线程显示错误信息
                        mainHandler.post {
                            Toast.makeText(
                                this@ChatActivity,
                                "加载模型列表失败: $errorMsg",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
            }
        }
    }

    // 检查是否有选中的角色
    private fun checkSelectedCharacter() {
        // 首先尝试从Intent中获取JSON数据
        intent.getStringExtra("SELECTED_CHARACTER").let { characterJson ->
            try {
                // 使用Gson将JSON字符串转换为AICharacter对象
                currentAICharacter = Gson().fromJson(characterJson, AICharacter::class.java)
                currentCharacterId = currentAICharacter.aiCharacterId
                currentCharacterName = currentAICharacter.name
                updateCharacterDisplay()
                // 加载聊天记录
                loadInitialData()
                return
            } catch (e: Exception) {
                Log.e(TAG, "解析角色JSON失败", e)
                Toast.makeText(this, "解析角色信息失败", Toast.LENGTH_SHORT).show()
            }
        }
        var characterId = configManager.getSelectedCharacterId() ?: ""
        if (characterId.isNotEmpty()) {
            launch(Dispatchers.IO) {
                AppContext.appDatabase.aiCharacterDao().getCharacterById(characterId)?.let { character ->
                    currentAICharacter = character
                    currentCharacterId = character.aiCharacterId
                    currentCharacterName = character.name
                    mainHandler.post {
                        updateCharacterDisplay()
                        // 加载聊天记录
                        loadInitialData()
                    }
                }
            }
        } else {
            // 如果没有选中的角色，可能需要跳转到角色选择页面
            // 这里可以根据需求添加相应逻辑
            Toast.makeText(this, "请选择一个角色开始聊天", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@ChatActivity, MainActivity::class.java))
        }
    }

    // 加载初始数据
    private fun loadInitialData() {
        lifecycleScope.launch {
            val initialMessages = chatMessageDao.getMessagesByPage(
                currentAICharacter?.aiCharacterId.toString(),
                PAGE_SIZE,
                0
            )
            Log.d(
                "loadInitialData",
                "initialMessages[ ${initialMessages.map { it -> it.content }}]"
            )
            chatContext.clear()
            var count = 0
            initialMessages.forEach { message ->
                if (count < MAX_CONTEXT_MESSAGE_SIZE) {
                    chatContext.add(0, message)
                    count++
                }
            }
            // 将初始数据存入 chatMessages
            chatMessages.clear()
            val reversedMessages = initialMessages.reversed()
            chatMessages.addAll(reversedMessages) // 反转降序数据为升序
            chatAdapter.setMessages(reversedMessages)
            currentPage = 1
            // 检查是否还有更多数据
            val totalCount = chatMessageDao.getTotalMessageCount()
            hasMoreData = totalCount > PAGE_SIZE

            // 滚动到底部
            binding.rvChat.scrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    // 加载更多数据
    private fun loadMoreData() {
        if (isLoading || !hasMoreData) return

        lifecycleScope.launch {
            isLoading = true

            val offset = currentPage * PAGE_SIZE
            val moreMessages = chatMessageDao.getMessagesByPage(
                currentAICharacter?.aiCharacterId.toString(),
                PAGE_SIZE,
                offset
            )
            Log.d("loadMoreData", "moreMessages[ ${moreMessages.map { it -> it.content }}]")
            if (moreMessages.isNotEmpty()) {
                // 将更多数据存入 chatMessages（反转顺序）
                chatMessages.addAll(0, moreMessages.reversed())
                chatAdapter.addMoreMessages(moreMessages.reversed())
                currentPage++

                // 检查是否还有更多数据
                val totalCount = chatMessageDao.getTotalMessageCount()
                hasMoreData = (currentPage * PAGE_SIZE) < totalCount
            } else {
                hasMoreData = false
            }

            isLoading = false
        }
    }

    // 添加加载聊天记录的方法
    private fun loadChatHistory() {
        // 首先取消现有的Flow订阅
        currentFlowJob?.cancel()
        currentAICharacter?.aiCharacterId?.let { characterId ->
            // 关键修复：将新的订阅保存到currentFlowJob变量中
            currentFlowJob = launch(Dispatchers.IO) {
                chatMessageDao.getMessagesByCharacterId(characterId).let { messages ->
                    mainHandler.post {
                        // 确保只处理当前角色的消息
                        val currentCharacterId = currentAICharacter?.aiCharacterId
                        if (currentCharacterId == characterId) {
                            chatMessages.clear()
                            chatMessages.addAll(messages)
                            // submitList是一个异步方法，如果没有提交完数据就scroll会导致无法移动到最底部，这里采用了submitList的回调方法
                            chatAdapter.setMessages(chatMessages.toList()) {
                                if (chatMessages.isNotEmpty()) {
                                    val itemCount = chatAdapter.getProcessedItemCount()
                                    if (itemCount > 0) {
                                        binding.rvChat.scrollToPosition(itemCount - 1)
                                    }
                                }
                            }

                            // 维护chatContext变量，保留10条最新消息
                            // 如果消息总数超过10条，则只保留最新的10条
//                            chatContext = (if (messages.size > 10) {
//                                messages.takeLast(10)
//                            } else {
//                                messages
//                            }) as LimitMutableList<ChatMessage>
                        }
                    }
                }
            }
        }
    }

    private fun handleNewMessage(message: ChatMessage) {
        // 添加新的消息到列表中
        chatMessages.add(message)
        // 添加到聊天上下文中
        chatContext.add(message)
        Log.d("chatContext", "chatContext[ ${chatContext.map { it -> it.content }}]")
        // 提交列表数据到适配器中
        chatAdapter.setMessages(chatMessages) {
            // 滚动到列表底部
            binding.rvChat.scrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    // UI交互相关方法
    // 更新角色显示
    private fun updateCharacterDisplay() {
        binding.tvCurrentCharacter.text = currentAICharacter?.name
    }

    // 展示模型列表对话框
    private fun showModelsDialog() {
        if (supportedModels.isEmpty()) {
            Toast.makeText(this, "正在加载模型列表，请稍后重试", Toast.LENGTH_SHORT).show()
            return
        }

        // 提取模型ID列表
        val modelIds = supportedModels.map { it.id }.toTypedArray()
        // 找到当前选中模型的位置
        val checkedItem = supportedModels.indexOfFirst { it.id == selectedModel }

        AlertDialog.Builder(this)
            .setTitle("选择模型（当前：$selectedModel）")
            .setSingleChoiceItems(modelIds, checkedItem) { dialog, position ->
                // 选择模型后保存选中的模型
                selectedModel = supportedModels[position].id
                configManager.saveSelectedModel(selectedModel)

                // 更新抽屉头部的当前模型信息
                val tvCurrentModelName = headerView?.findViewById<TextView>(R.id.tvCurrentModelName)
                tvCurrentModelName?.text = selectedModel

                dialog.dismiss()

                Toast.makeText(this, "已选择模型：$selectedModel", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 清空聊天记录
    private fun clearChatHistory() {
        AlertDialog.Builder(this)
            .setTitle("确认清空")
            .setMessage("确定要清空当前角色的所有聊天记录吗？")
            .setPositiveButton("确定") { _, _ ->
                currentAICharacter?.aiCharacterId?.let {
                    launch(Dispatchers.IO) {
                        chatMessageDao.deleteMessagesByCharacterId(it)
                        mainHandler.post {
                            chatMessages.clear()
                            chatAdapter.setMessages(emptyList())
                            chatContext.clear()
                            Toast.makeText(this@ChatActivity, "聊天记录已清空", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 显示关于对话框
    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("关于")
            .setMessage(getString(R.string.app_name) + "\n版本: 1.0\n\n一个基于AI的聊天应用")
            .setPositiveButton("确定", null)
            .show()
    }

    // 导航相关方法
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_config -> {
                startActivity(Intent(this, ConfigActivity::class.java))
            }

            R.id.nav_switch_model -> {
                showModelsDialog()
            }

            R.id.nav_clear_chat -> {
                clearChatHistory()
            }

            R.id.nav_about -> {
//                showAboutDialog()
                var s = chatMessages.map { it.content }
                Log.d("Data", "$s")
            }
        }
        return true
    }
}