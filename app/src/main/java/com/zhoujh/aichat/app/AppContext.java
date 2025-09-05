package com.zhoujh.aichat.app;

import android.app.Application;
import android.content.Context;

import com.zhoujh.aichat.database.AppDatabase;

public class AppContext extends Application {
    private static final String TAG = AppContext.class.getName();
    public static final String USER_ID = "123123";
    private static AppContext instance;
    public static AppContext getInstance(){return instance;}

    public static Context getContext(){return instance.getApplicationContext();}

    public static AppDatabase appDatabase;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        //构建数据库
        appDatabase = AppDatabase.Companion.getDatabase(getContext());
    }
}
