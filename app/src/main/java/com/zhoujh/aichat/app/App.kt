package com.zhoujh.aichat.app

import android.app.Application
import com.zhoujh.aichat.database.AppDatabase

class App : Application() {
    companion object {
        val TAG = App::class.java.name
        lateinit var instance: App
        lateinit var appDatabase: AppDatabase
    }
    override fun onCreate() {
        super.onCreate()
        instance = this
        //构建数据库
        appDatabase = AppDatabase.Companion.getDatabase(instance.applicationContext)
    }
}