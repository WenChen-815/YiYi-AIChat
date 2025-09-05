package com.zhoujh.aichat.ui

import android.R
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.zhoujh.aichat.databinding.ActivityConfigBinding
import com.zhoujh.aichat.model.Model
import com.zhoujh.aichat.network.ApiService
import com.zhoujh.aichat.utils.ConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

class ConfigActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private lateinit var binding: ActivityConfigBinding
    private lateinit var configManager: ConfigManager
    private var models: List<Model> = emptyList()

    // 创建主线程Handler用于UI更新
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigBinding.inflate(layoutInflater)
        enableEdgeToEdge()

        setContentView(binding.root)

        configManager = ConfigManager()
        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        // 加载已保存的配置
        configManager.getApiKey()?.let {
            binding.etApiKey.setText(it)
        }
        configManager.getBaseUrl()?.let {
            binding.etBaseUrl.setText(it)
        }
    }

    private fun setupListeners() {
        // 加载模型列表
        binding.btnLoadModels.setOnClickListener {
            val apiKey = binding.etApiKey.text.toString().trim()
            val baseUrl = binding.etBaseUrl.text.toString().trim()

            if (apiKey.isEmpty() || baseUrl.isEmpty()) {
                Toast.makeText(this, "请先填写API Key和中转地址", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loadModels(apiKey, baseUrl)
        }

        // 保存配置
        binding.btnSave.setOnClickListener {
            val apiKey = binding.etApiKey.text.toString().trim()
            val baseUrl = binding.etBaseUrl.text.toString().trim()
            val selectedModelPosition = binding.spinnerModels.selectedItemPosition

            if (apiKey.isEmpty() || baseUrl.isEmpty()) {
                Toast.makeText(this, "请填写完整的API Key和中转地址", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (models.isEmpty() || selectedModelPosition < 0) {
                Toast.makeText(this, "请先加载并选择模型", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 保存配置
            configManager.saveApiKey(apiKey)
            configManager.saveBaseUrl(baseUrl)
            configManager.saveSelectedModel(models[selectedModelPosition].id)

            Toast.makeText(this, "配置保存成功", Toast.LENGTH_SHORT).show()

            // 返回主界面
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun loadModels(apiKey: String, baseUrl: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLoadModels.isEnabled = false

        // 使用Thread替代协程执行后台任务
        Thread {
            val apiService = ApiService(baseUrl, apiKey)
            apiService.getSupportedModels(
                onSuccess = { modelList ->
                    // 使用Handler在主线程更新UI
                    mainHandler.post {
                        models = modelList
                        binding.progressBar.visibility = View.GONE
                        binding.btnLoadModels.isEnabled = true

                        // 将模型ID显示在下拉列表中
                        val modelIds = modelList.map { it.id }.toTypedArray()
                        val adapter = ArrayAdapter(
                            this@ConfigActivity,
                            R.layout.simple_spinner_item,
                            modelIds
                        )
                        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
                        binding.spinnerModels.adapter = adapter

                        // 如果有已保存的模型，选中它
                        configManager.getSelectedModel()?.let { savedModel ->
                            val position = modelList.indexOfFirst { it.id == savedModel }
                            if (position != -1) {
                                binding.spinnerModels.setSelection(position)
                            }
                        }

                        Toast.makeText(
                            this@ConfigActivity,
                            "成功加载${modelList.size}个模型",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onError = { errorMsg ->
                    // 使用Handler在主线程显示错误信息
                    mainHandler.post {
                        binding.progressBar.visibility = View.GONE
                        binding.btnLoadModels.isEnabled = true
                        Toast.makeText(this@ConfigActivity, errorMsg, Toast.LENGTH_LONG).show()
                        Log.e("ConfigActivity", "加载模型失败: $errorMsg")
                    }
                }
            )
        }.start()
    }
}
