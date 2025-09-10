package com.zhoujh.aichat.app.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * 聊天图片管理器：按会话ID在外部存储私有目录分类管理图片
 */
class ChatImageManager(private val context: Context) {

    /**
     * 获取图片存储的根目录
     * 路径格式：<外部存储应用私有目录>/chatImg/
     */
    private val imageRootDir: File
        get() = File(context.getExternalFilesDir(null), "chatImg").apply {
            if (!exists()) mkdirs() // 确保根目录存在
        }

    /**
     * 保存图片到指定会话的文件夹
     * 最终路径：<外部存储应用私有目录>/chat_img/<conversationId>/img_<conversationId>_<时间戳>.jpg
     *
     * @param conversationId 会话ID
     * @param bitmap 要保存的图片
     * @param format 图片格式，默认JPEG
     * @param quality 压缩质量，0-100，默认100
     * @return 保存成功的文件对象，失败返回null
     */
    fun saveImageToConversation(
        conversationId: String,
        bitmap: Bitmap,
        format: CompressFormat = CompressFormat.JPEG,
        quality: Int = 100
    ): File? {
        // 1. 创建会话专属文件夹
        val conversationDir = File(imageRootDir, conversationId).apply {
            if (!exists()) mkdirs() // 确保会话目录存在
        }

        // 2. 生成符合规则的图片文件名：img_<conversationId>_<当前时间戳>.jpg
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileExtension = if (format == CompressFormat.PNG) "png" else "jpg"
        val fileName = "img_${conversationId}_${timeStamp}.$fileExtension"

        // 3. 创建文件对象
        val imageFile = File(conversationDir, fileName)

        return try {
            // 4. 保存图片
            imageFile.outputStream().use { outputStream ->
                bitmap.compress(format, quality, outputStream)
            }
            imageFile // 保存成功，返回文件对象
        } catch (e: IOException) {
            e.printStackTrace()
            null // 保存失败
        }
    }

    /**
     * 获取指定会话的所有图片文件
     * @param conversationId 会话ID
     * @return 按修改时间排序的图片文件列表（最新的在前面）
     */
    fun getImagesForConversation(conversationId: String): List<File> {
        val conversationDir = File(imageRootDir, conversationId)
        return if (conversationDir.exists() && conversationDir.isDirectory) {
            // 获取所有图片文件并按修改时间倒序排列
            conversationDir.listFiles()?.filter { it.isFile }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        } else {
            emptyList()
        }
    }

    /**
     * 获取指定会话的图片存储目录
     * @param conversationId 会话ID
     * @return 目录文件对象
     */
    fun getConversationImageDir(conversationId: String): File {
        return File(imageRootDir, conversationId).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * 删除指定会话的单张图片
     * @param conversationId 会话ID
     * @param fileName 图片文件名
     * @return 是否删除成功
     */
    fun deleteImage(conversationId: String, fileName: String): Boolean {
        val imageFile = File(File(imageRootDir, conversationId), fileName)
        return if (imageFile.exists() && imageFile.isFile) {
            imageFile.delete()
        } else {
            false
        }
    }

    /**
     * 删除指定会话的所有图片
     * @param conversationId 会话ID
     * @return 是否删除成功
     */
    fun deleteAllImagesForConversation(conversationId: String): Boolean {
        val conversationDir = File(imageRootDir, conversationId)
        return if (conversationDir.exists() && conversationDir.isDirectory) {
            conversationDir.deleteRecursively()
        } else {
            true // 目录不存在，视为删除成功
        }
    }

    /**
     * 获取图片文件的完整路径
     * @param conversationId 会话ID
     * @param fileName 图片文件名
     * @return 完整路径字符串
     */
    fun getImageFilePath(conversationId: String, fileName: String): String {
        return File(File(imageRootDir, conversationId), fileName).absolutePath
    }
}
