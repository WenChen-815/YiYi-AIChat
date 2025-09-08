package com.zhoujh.aichat.network

import android.util.Log
import com.zhoujh.aichat.network.model.ChatResponse
import com.zhoujh.aichat.network.model.ModelsResponse
import com.google.gson.Gson
import com.zhoujh.aichat.network.model.Message
import com.zhoujh.aichat.network.model.Model
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiService(private val baseUrl: String, private val apiKey: String) {
    private val tag = "ApiService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

    // 发送聊天消息
    fun sendMessage(
        messages: MutableList<Message>,
        model: String,
        temperature: Float? = 1.00f,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // 创建请求体对象
        val chatRequest = ChatRequest(model, messages,temperature)

        // 使用Gson将请求体对象转换为JSON字符串
        val requestBodyStr = gson.toJson(chatRequest)
        val requestBody = requestBodyStr.toRequestBody(jsonMediaType)
        Log.d(tag, requestBodyStr)
        // 构建请求
        val request = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        // 执行请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("网络请求失败：${e.message ?: "未知错误"}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    try {
                        val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
                        val aiMessage = chatResponse.choices?.firstOrNull()?.message?.content
                        if (aiMessage.isNullOrEmpty()) {
                            onError("未获取到AI回复")
                        } else {
                            onSuccess(aiMessage)
                        }
                    } catch (e: Exception) {
                        onError("解析响应失败：${e.message}")
                    }
                } else {
                    onError("请求失败：$responseBody（状态码：${response.code}）")
                    Log.d(tag, "请求失败：$responseBody（状态码：${response.code}）")
                }
            }
        })
    }

    // 获取支持的模型列表
    fun getSupportedModels(
        onSuccess: (List<Model>) -> Unit,
        onError: (String) -> Unit
    ) {
        // 构建请求
        val request = Request.Builder()
            .url("$baseUrl/v1/models")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .get()
            .build()

        // 执行请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("网络请求失败：${e.message ?: "未知错误"}")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    try {
                        val modelsResponse = gson.fromJson(responseBody, ModelsResponse::class.java)
                        if (modelsResponse.success) {
                            onSuccess(modelsResponse.data)
                        } else {
                            onError("获取模型列表失败：接口返回不成功")
                        }
                    } catch (e: Exception) {
                        onError("解析模型列表失败：${e.message}")
                    }
                } else {
                    onError("获取模型列表失败：$responseBody（状态码：${response.code}）")
                }
            }
        })
    }

    // 定义聊天请求体的数据类
    data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val temperature: Float? = null
    ) {
        override fun toString(): String {
            return "ChatRequest(model='$model', messagesCount=${messages.size}, messages=\n${
                messages.joinToString(
                    ", \n"
                ) { "[role=${it.role}, content=${it.content?.take(50)}${if ((it.content?.length ?: 0) > 50) "..." else ""}]" }
            })"
        }
    }
}
