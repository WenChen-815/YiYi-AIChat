package com.zhoujh.aichat.app.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.zhoujh.aichat.app.App

class ConfigManager() {
    private val PREF_NAME = "ai_chat_config"
    private val KEY_API_KEY = "api_key"
    private val KEY_BASE_URL = "base_url"
    private val KEY_SELECTED_MODEL = "selected_model"
    // 添加图片识别相关的KEY
    private val KEY_IMG_RECOGNITION_ENABLED = "img_recognition_enabled"
    private val KEY_IMG_API_KEY = "img_api_key"
    private val KEY_IMG_BASE_URL = "img_base_url"
    private val KEY_SELECTED_IMG_MODEL = "selected_img_model"

    private val KEY_MAX_CONTEXT_MESSAGE_SIZE = "max_context_message_size"
    private val KEY_SUMMARIZE_TRIGGER_COUNT = "summarize_trigger_count"

    private val prefs: SharedPreferences =
        App.instance.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // 保存API Key
    fun saveApiKey(apiKey: String) {
        prefs.edit { putString(KEY_API_KEY, apiKey) }
    }

    // 获取API Key
    fun getApiKey(): String? {
        return prefs.getString(KEY_API_KEY, null)
    }

    // 保存中转地址
    fun saveBaseUrl(baseUrl: String) {
        prefs.edit { putString(KEY_BASE_URL, baseUrl) }
    }

    // 获取中转地址
    fun getBaseUrl(): String? {
        return prefs.getString(KEY_BASE_URL, null)
    }

    // 保存选中的模型
    fun saveSelectedModel(modelId: String) {
        prefs.edit { putString(KEY_SELECTED_MODEL, modelId) }
    }

    // 获取选中的模型
    fun getSelectedModel(): String? {
        return prefs.getString(KEY_SELECTED_MODEL, null)
    }

    // 保存用户名
    fun saveUserName(userName: String) {
        prefs.edit { putString("user_name", userName) }
    }

    // 获取用户名
    fun getUserName(): String? {
        return prefs.getString("user_name", null)
    }

    // 保存用户ID
    fun saveUserId(userId: String) {
        prefs.edit { putString("user_id", userId) }
    }

    // 获取用户ID
    fun getUserId(): String? {
        return prefs.getString("user_id", null)
    }

    // 检查是否有完整的配置
    fun hasCompleteConfig(): Boolean {
        return !getApiKey().isNullOrEmpty() &&
                !getBaseUrl().isNullOrEmpty() &&
                !getSelectedModel().isNullOrEmpty() &&
                !getUserName().isNullOrEmpty() &&
                !getUserId().isNullOrEmpty()
    }

    // 选中的角色相关
    fun saveSelectedCharacterId(characterId: String) {
        prefs.edit { putString("selected_character_id", characterId) }
    }

    fun getSelectedCharacterId(): String? {
        return prefs.getString("selected_character_id", null)
    }

    // 清除选中的角色
    fun clearSelectedCharacter() {
        prefs.edit { remove("selected_character_id") }
    }

    // 清除所有配置
    fun clearAllConfig() {
        prefs.edit { clear() }
    }

    // ===== 图片识别相关配置 =====
    // 保存图片识别开关状态
    fun saveImgRecognitionEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_IMG_RECOGNITION_ENABLED, enabled) }
    }

    // 获取图片识别开关状态
    fun isImgRecognitionEnabled(): Boolean {
        return prefs.getBoolean(KEY_IMG_RECOGNITION_ENABLED, false)
    }
    // 保存图片识别API Key
    fun saveImgApiKey(apiKey: String) {
        prefs.edit { putString(KEY_IMG_API_KEY, apiKey) }
    }

    // 获取图片识别API Key
    fun getImgApiKey(): String? {
        return prefs.getString(KEY_IMG_API_KEY, null)
    }

    // 保存图片识别中转地址
    fun saveImgBaseUrl(baseUrl: String) {
        prefs.edit { putString(KEY_IMG_BASE_URL, baseUrl) }
    }

    // 获取图片识别中转地址
    fun getImgBaseUrl(): String? {
        return prefs.getString(KEY_IMG_BASE_URL, null)
    }

    // 保存选中的图片识别模型
    fun saveSelectedImgModel(modelId: String) {
        prefs.edit { putString(KEY_SELECTED_IMG_MODEL, modelId) }
    }

    // 获取选中的图片识别模型
    fun getSelectedImgModel(): String? {
        return prefs.getString(KEY_SELECTED_IMG_MODEL, null)
    }

    // 保存最大上下文消息数
    fun saveMaxContextMessageSize(size: Int) {
        prefs.edit { putInt(KEY_MAX_CONTEXT_MESSAGE_SIZE, size) }
    }

    // 获取最大上下文消息数
    fun getMaxContextMessageSize(): Int {
        return prefs.getInt(KEY_MAX_CONTEXT_MESSAGE_SIZE, 5)
    }

    // 保存触发总结的对话数
    fun saveSummarizeTriggerCount(count: Int) {
        prefs.edit { putInt(KEY_SUMMARIZE_TRIGGER_COUNT, count) }
    }

    // 获取触发总结的对话数
    fun getSummarizeTriggerCount(): Int {
        return prefs.getInt(KEY_SUMMARIZE_TRIGGER_COUNT, 20)
    }
}