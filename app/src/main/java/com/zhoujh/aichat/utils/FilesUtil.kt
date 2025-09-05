package com.zhoujh.aichat.utils

import android.content.Context
import java.io.File
import java.io.FileWriter

class FilesUtil(
    var context: Context
) {
    fun appendToFile(message: String, fileName: String) {
        var file = File(context.filesDir, fileName)
        if (!file.exists()) {
            file.createNewFile()
        }
        // 在末尾新的一行添加内容，true为追加模式
        /*
        use是 Kotlin 提供的扩展函数，专门用于资源自动释放（类似 Java 的 try-with-resources 语法）。
        作用：在 lambda 表达式执行完毕后（无论正常结束还是抛出异常），会自动调用资源的close()方法，确保文件流被关闭，避免资源泄漏（比如文件被长期占用无法删除）。
        lambda 中的writer：是FileWriter的实例，通过它可以调用写入方法。
        */
        FileWriter(file, true).use { writer ->
            writer.append(message)
            writer.append("\n")
        }
    }
}