package com.zhoujh.aichat.ui.activity

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
import com.zhoujh.aichat.network.model.Model
import com.zhoujh.aichat.network.ApiService
import com.zhoujh.aichat.app.manager.ConfigManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

class ConfigActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private lateinit var binding: ActivityConfigBinding
    private lateinit var configManager: ConfigManager
    // 对话模型列表
    private var models: List<Model> = emptyList()
    // 图片识别模型列表
    private var imgModels: List<Model> = emptyList()
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
        binding.btnLoadModels.performClick()
    }

    private fun setupViews() {
        // 加载已保存的对话模型配置
        configManager.getApiKey()?.let {
            binding.etApiKey.setText(it)
        }
        configManager.getBaseUrl()?.let {
            binding.etBaseUrl.setText(it)
        }
        // 加载图片识别开关状态
        binding.switchImgRecognition.isChecked = configManager.isImgRecognitionEnabled()

        // 根据开关状态更新UI
        updateImgRecognitionUI(configManager.isImgRecognitionEnabled())
        // 加载已保存的图片识别配置
        configManager.getImgApiKey()?.let {
            binding.etImgApiKey.setText(it)
        }
        configManager.getImgBaseUrl()?.let {
            binding.etImgBaseUrl.setText(it)
        }
    }

    private fun setupListeners() {
        // 加载对话模型列表
        binding.btnLoadModels.setOnClickListener {
            val apiKey = binding.etApiKey.text.toString().trim()
            val baseUrl = binding.etBaseUrl.text.toString().trim()

            if (apiKey.isEmpty() || baseUrl.isEmpty()) {
                Toast.makeText(this, "请先填写API Key和中转地址", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loadModels(apiKey, baseUrl, 0)
        }

        // 图片识别开关监听
        binding.switchImgRecognition.setOnCheckedChangeListener {
                _, isChecked ->
            updateImgRecognitionUI(isChecked)
        }

        // 加载图片识别模型列表
        binding.btnLoadImgModels.setOnClickListener {
            // 检查开关是否开启
            if (!binding.switchImgRecognition.isChecked) {
                Toast.makeText(this, "请先开启图片识别开关", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val imgApiKey = binding.etImgApiKey.text.toString().trim()
            val imgBaseUrl = binding.etImgBaseUrl.text.toString().trim()

            if (imgApiKey.isEmpty() || imgBaseUrl.isEmpty()) {
                Toast.makeText(this, "请先填写图片识别API Key和中转地址", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loadModels(imgApiKey, imgBaseUrl, 1)
        }
        // 保存配置
        binding.btnSave.setOnClickListener {
            // 用户配置
            val userId = binding.etUserId.text.toString().trim()
            val userName = binding.etUserName.text.toString().trim()
            // 对话模型配置
            val apiKey = binding.etApiKey.text.toString().trim()
            val baseUrl = binding.etBaseUrl.text.toString().trim()
            val selectedModelPosition = binding.spinnerModels.selectedItemPosition
            // 图片识别相关配置
            val imgRecognitionEnabled = binding.switchImgRecognition.isChecked
            val imgApiKey = binding.etImgApiKey.text.toString().trim()
            val imgBaseUrl = binding.etImgBaseUrl.text.toString().trim()
            val selectedImgModelPosition = binding.spinnerImgModels.selectedItemPosition

            if (apiKey.isEmpty() || baseUrl.isEmpty()) {
                Toast.makeText(this, "请填写完整的API Key和中转地址", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (models.isEmpty() || selectedModelPosition < 0) {
                Toast.makeText(this, "请先加载并选择模型", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 检查图片识别开关状态
            if (imgRecognitionEnabled) {
                if (imgApiKey.isEmpty() || imgBaseUrl.isEmpty()) {
                    Toast.makeText(this, "请填写完整的图片识别API Key和中转地址", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (imgModels.isEmpty() || selectedImgModelPosition < 0) {
                    Toast.makeText(this, "请先加载并选择模型", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // 保存配置
            configManager.saveUserId(userId)
            configManager.saveUserName(userName)

            configManager.saveApiKey(apiKey)
            configManager.saveBaseUrl(baseUrl)
            configManager.saveSelectedModel(models[selectedModelPosition].id)

            configManager.saveImgRecognitionEnabled(imgRecognitionEnabled)
            // 只有当图片识别开关开启时才保存图片识别相关配置
            if (imgRecognitionEnabled) {
                configManager.saveImgApiKey(imgApiKey)
                configManager.saveImgBaseUrl(imgBaseUrl)
                if (imgModels.isNotEmpty() && selectedImgModelPosition >= 0) {
                    configManager.saveSelectedImgModel(imgModels[selectedImgModelPosition].id)
                }
            }

            Toast.makeText(this, "配置保存成功", Toast.LENGTH_SHORT).show()

            // 返回主界面
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    // 根据图片识别开关状态更新UI
    private fun updateImgRecognitionUI(isEnabled: Boolean) {
        binding.etImgApiKey.isEnabled = isEnabled
        binding.etImgBaseUrl.isEnabled = isEnabled
        binding.spinnerImgModels.isEnabled = isEnabled
        binding.btnLoadImgModels.isEnabled = isEnabled

        // 可以根据需要设置透明度来表示可用性
        if (isEnabled) {
            binding.etImgApiKey.alpha = 1.0f
            binding.etImgBaseUrl.alpha = 1.0f
            binding.spinnerImgModels.alpha = 1.0f
        } else {
            binding.etImgApiKey.alpha = 0.5f
            binding.etImgBaseUrl.alpha = 0.5f
            binding.spinnerImgModels.alpha = 0.5f
        }
    }

    private fun loadModels(apiKey: String, baseUrl: String, type: Int = 0) {
        // 显示进度条
        if (type == 0) {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnLoadModels.isEnabled = false
        } else {
            binding.progressImgBar.visibility = View.VISIBLE
            binding.btnLoadImgModels.isEnabled = false
        }
        // 使用Thread替代协程执行后台任务
        Thread {
            val apiService = ApiService(baseUrl, apiKey)
            apiService.getSupportedModels(
                onSuccess = { modelList ->
                    // 使用Handler在主线程更新UI
                    mainHandler.post {
                        if (type == 0) {
                            models = modelList
                            binding.progressBar.visibility = View.GONE
                            binding.btnLoadModels.isEnabled = true
                        } else {
                            imgModels = modelList
                            binding.progressImgBar.visibility = View.GONE
                            binding.btnLoadImgModels.isEnabled = true
                        }

                        // 将模型ID显示在下拉列表中
                        val modelIds = modelList.map { it.id }.toTypedArray()
                        val adapter = ArrayAdapter(
                            this@ConfigActivity,
                            R.layout.simple_spinner_item,
                            modelIds
                        )
                        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
                        if (type == 0) {
                            binding.spinnerModels.adapter = adapter
                        } else {
                            binding.spinnerImgModels.adapter = adapter
                        }

                        // 如果有已保存的模型，选中它
                        configManager.getSelectedModel()?.let { savedModel ->
                            val position = modelList.indexOfFirst { it.id == savedModel }
                            if (position != -1) {
                                binding.spinnerModels.setSelection(position)
                            }
                        }
                        // 如果有已保存的图片识别模型，选中它
                        configManager.getSelectedImgModel()?.let { savedImgModel ->
                            val imgPosition = modelList.indexOfFirst { it.id == savedImgModel }
                            if (imgPosition != -1) {
                                binding.spinnerImgModels.setSelection(imgPosition)
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
                        if (type == 0) {
                            binding.progressBar.visibility = View.GONE
                            binding.btnLoadModels.isEnabled = true
                        } else {
                            binding.progressImgBar.visibility = View.GONE
                            binding.btnLoadImgModels.isEnabled = true
                        }
                        Toast.makeText(this@ConfigActivity, errorMsg, Toast.LENGTH_LONG).show()
                        Log.e("ConfigActivity", "加载模型失败: $errorMsg")
                    }
                }
            )
        }.start()
    }
}
