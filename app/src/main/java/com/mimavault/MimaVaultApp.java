package com.mimavault;

import android.app.Application;
import android.content.Context;

import com.mimavault.db.DatabaseManager;

/**
 * 应用入口：持有全局 Context，初始化数据库
 */
public class MimaVaultApp extends Application {

    private static Context appContext;
    private static DatabaseManager db;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        db = new DatabaseManager();
        db.init();
    }

    public static Context getAppContext() {
        return appContext;
    }

    public static DatabaseManager db() {
        return db;
    }
}
