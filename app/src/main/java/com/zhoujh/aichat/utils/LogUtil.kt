package com.zhoujh.aichat.utils

import android.util.Log

object LogUtil {
    // Android日志默认最大长度约为4000个字符
    private const val MAX_LOG_LENGTH = 4000

    /**
     * 完整输出长日志字符串，不被截断
     * @param tag 日志标签
     * @param message 要输出的日志消息
     */
    fun d(tag: String, message: String) {
        if (message.length <= MAX_LOG_LENGTH) {
            // 如果消息长度不超过限制，直接输出
            Log.d(tag, message)
            return
        }

        // 将长消息分割成多个片段输出
        var index = 0
        val totalLength = message.length
        while (index < totalLength) {
            val remainingLength = totalLength - index
            val chunkSize = if (remainingLength > MAX_LOG_LENGTH) MAX_LOG_LENGTH else remainingLength
            val chunk = message.substring(index, index + chunkSize)

            // 在每个片段前添加索引信息，便于识别完整日志
            Log.d(tag, "[Part ${index / MAX_LOG_LENGTH + 1}] $chunk")

            index += chunkSize
        }
    }
}
