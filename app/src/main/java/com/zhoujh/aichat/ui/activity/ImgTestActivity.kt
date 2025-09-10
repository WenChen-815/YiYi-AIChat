package com.zhoujh.aichat.ui.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.zhoujh.aichat.app.manager.ConfigManager
import com.zhoujh.aichat.databinding.ActivityImgTestBinding
import com.zhoujh.aichat.network.ApiService
import com.zhoujh.aichat.network.model.Model
import java.io.ByteArrayOutputStream
import java.io.IOException

class ImgTestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityImgTestBinding
    private lateinit var configManager: ConfigManager
    private var models: List<Model> = emptyList()
    private var selectedImageUri: Uri? = null
    private val REQUEST_CODE_PICK_IMAGE = 1001
    private val REQUEST_CODE_PERMISSION = 1002

    // 创建主线程Handler用于UI更新
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImgTestBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        configManager = ConfigManager()
        setupViews()
        setupListeners()

        // 在进入界面时动态请求权限
        requestImagePermissionOnStartup()

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
        // 选择图片按钮
        binding.btnSelectImage.setOnClickListener {
            checkPermissionAndPickImage()
        }

        // 发送按钮
        binding.btnSend.setOnClickListener {
            sendMultimodalMessage()
        }
    }

    // 在进入界面时请求权限的方法
    private fun requestImagePermissionOnStartup() {
        // 检查Android版本，适配不同的权限模型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14及以上版本 - 使用新的精选照片API，无需提前请求权限
            Log.d("ImgTestActivity", "Android 14+，使用精选照片API无需提前请求权限")
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33) - 使用READ_MEDIA_IMAGES权限
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
                    REQUEST_CODE_PERMISSION
                )
            }
        } else {
            // Android 12及以下版本 - 使用READ_EXTERNAL_STORAGE权限
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_PERMISSION
                )
            }
        }
    }

    private fun checkPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14及以上版本 - 直接使用新的精选照片API
            pickImageFromGallery()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33) - 使用READ_MEDIA_IMAGES权限
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
                    REQUEST_CODE_PERMISSION
                )
            } else {
                pickImageFromGallery()
            }
        } else {
            // Android 12及以下版本 - 使用READ_EXTERNAL_STORAGE权限
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_PERMISSION
                )
            } else {
                pickImageFromGallery()
            }
        }
    }

    private fun pickImageFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14及以上版本 - 使用新的精选照片API
            val intent = Intent(MediaStore.ACTION_PICK_IMAGES)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
        } else {
            // 旧版本使用传统方法
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("ImgTestActivity", "图片访问权限已授予")
            } else {
                // 权限被拒绝，显示提示信息
                Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImageUri)
                binding.imagePreview.setImageBitmap(bitmap)
                Toast.makeText(this, "图片选择成功", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show()
            }
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
                            this@ImgTestActivity,
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
                            this@ImgTestActivity,
                            "成功加载${modelList.size}个模型",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onError = { errorMsg ->
                    // 使用Handler在主线程显示错误信息
                    mainHandler.post {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@ImgTestActivity, errorMsg, Toast.LENGTH_LONG).show()
                        Log.e("ImgTestActivity", "加载模型失败: $errorMsg")
                    }
                }
            )
        }.start()
    }

    private fun sendMultimodalMessage() {
        val apiKey = configManager.getApiKey()
        val baseUrl = configManager.getBaseUrl()
        val prompt = binding.etPrompt.text.toString().trim()
        val selectedModelPosition = binding.spinnerModels.selectedItemPosition

        if (apiKey.isNullOrEmpty() || baseUrl.isNullOrEmpty()) {
            Toast.makeText(this, "请先在设置中配置API Key和中转地址", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, "请先选择一张图片", Toast.LENGTH_SHORT).show()
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
                // 将图片转换为Base64
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImageUri)
                val byteArrayOutputStream = ByteArrayOutputStream()
                // 压缩图片质量以减少大小
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                val base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT)

                val apiService = ApiService(baseUrl, apiKey)
                apiService.sendMultimodalMessage(
                    prompt = prompt,
                    imageBase64 = base64Image,
                    model = selectedModel,
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
                            Toast.makeText(this@ImgTestActivity, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            } catch (e: Exception) {
                mainHandler.post {
                    binding.tvAIResponse.text = "处理图片失败: ${e.message}"
                    binding.btnSend.isEnabled = true
                    Toast.makeText(this@ImgTestActivity, "处理图片失败", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}
