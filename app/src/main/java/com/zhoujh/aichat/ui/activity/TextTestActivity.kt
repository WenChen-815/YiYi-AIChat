package com.zhoujh.aichat.ui.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.zhoujh.aichat.app.manager.ConfigManager
import com.zhoujh.aichat.databinding.ActivityTextTestBinding
import com.zhoujh.aichat.network.ApiService
import com.zhoujh.aichat.network.model.Message
import com.zhoujh.aichat.network.model.Model
import android.util.Log
import com.zhoujh.aichat.network.ApiService.ContentItem
import com.zhoujh.aichat.network.ApiService.MultimodalMessage

class TextTestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTextTestBinding
    private lateinit var configManager: ConfigManager
    private var models: List<Model> = emptyList()

    // 创建主线程Handler用于UI更新
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextTestBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        configManager = ConfigManager()
        setupViews()
        setupListeners()

        loadModels()
    }

    private fun setupViews() {
        // 加载已保存的配置
        val savedModel = configManager.getSelectedModel()
        if (!savedModel.isNullOrEmpty()) {
            // 稍后在加载完模型后选择已保存的模型
        }
    }

    private fun setupListeners() {
        // 发送按钮
        binding.btnSend.setOnClickListener {
            sendTextMessage()
        }
    }

    private fun loadModels() {
        val apiKey = configManager.getApiKey()
        val baseUrl = configManager.getBaseUrl()

        if (apiKey.isNullOrEmpty() || baseUrl.isNullOrEmpty()) {
            Toast.makeText(this, "请先在设置中配置API Key和中转地址", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSend.isEnabled = false

        // 使用Thread执行后台任务
        Thread {
            val apiService = ApiService(baseUrl, apiKey)
            apiService.getSupportedModels(
                onSuccess = { modelList ->
                    // 使用Handler在主线程更新UI
                    mainHandler.post {
                        models = modelList
                        binding.progressBar.visibility = View.GONE
                        binding.btnSend.isEnabled = true

                        // 将模型ID显示在下拉列表中
                        val modelIds = modelList.map { it.id }.toTypedArray()
                        val adapter = ArrayAdapter(
                            this@TextTestActivity,
                            android.R.layout.simple_spinner_item,
                            modelIds
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        binding.spinnerModels.adapter = adapter

                        // 如果有已保存的模型，选中它
                        configManager.getSelectedModel()?.let { savedModel ->
                            val position = modelList.indexOfFirst { it.id == savedModel }
                            if (position != -1) {
                                binding.spinnerModels.setSelection(position)
                            }
                        }

                        Toast.makeText(
                            this@TextTestActivity,
                            "成功加载${modelList.size}个模型",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onError = { errorMsg ->
                    // 使用Handler在主线程显示错误信息
                    mainHandler.post {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@TextTestActivity, errorMsg, Toast.LENGTH_LONG).show()
                        Log.e("TextTestActivity", "加载模型失败: $errorMsg")
                    }
                }
            )
        }.start()
    }

    private fun sendTextMessage() {
        val apiKey = configManager.getApiKey()
        val baseUrl = configManager.getBaseUrl()
        val prompt = binding.etPrompt.text.toString().trim()
        val selectedModelPosition = binding.spinnerModels.selectedItemPosition

        if (apiKey.isNullOrEmpty() || baseUrl.isNullOrEmpty()) {
            Toast.makeText(this, "请先在设置中配置API Key和中转地址", Toast.LENGTH_SHORT).show()
            return
        }

        if (prompt.isEmpty()) {
            Toast.makeText(this, "请输入提示词", Toast.LENGTH_SHORT).show()
            return
        }

        if (models.isEmpty() || selectedModelPosition < 0) {
            Toast.makeText(this, "请先加载并选择模型", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedModel = models[selectedModelPosition].id
        binding.btnSend.isEnabled = false
        binding.tvAIResponse.text = "正在发送请求..."

        // 使用Thread执行后台任务
        Thread {
            try {
                // 创建消息列表
                val messages = mutableListOf<Message>()
                messages.add(Message("user", prompt))

                val apiService = ApiService(baseUrl, apiKey)
                apiService.sendMessage(
                    messages = messages,
                    model = selectedModel,
                    temperature = 1.0f,
                    onSuccess = { response ->
                        mainHandler.post {
                            binding.tvAIResponse.text = response
                            binding.btnSend.isEnabled = true
                        }
                    },
                    onError = { errorMsg ->
                        mainHandler.post {
                            binding.tvAIResponse.text = "请求失败: $errorMsg"
                            binding.btnSend.isEnabled = true
                            Toast.makeText(this@TextTestActivity, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            } catch (e: Exception) {
                mainHandler.post {
                    binding.tvAIResponse.text = "处理失败: ${e.message}"
                    binding.btnSend.isEnabled = true
                    Toast.makeText(this@TextTestActivity, "处理失败", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}
