package com.zhoujh.aichat.app.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.zhoujh.aichat.app.AppContext

class ConfigManager() {
    private val PREF_NAME = "ai_chat_config"
    private val KEY_API_KEY = "api_key"
    private val KEY_BASE_URL = "base_url"
    private val KEY_SELECTED_MODEL = "selected_model"

    private val prefs: SharedPreferences =
        AppContext.getInstance().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

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

    // 检查是否有完整的配置
    fun hasCompleteConfig(): Boolean {
        return !getApiKey().isNullOrEmpty() &&
                !getBaseUrl().isNullOrEmpty() &&
                !getSelectedModel().isNullOrEmpty()
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
}