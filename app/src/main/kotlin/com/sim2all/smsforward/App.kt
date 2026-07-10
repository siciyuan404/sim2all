package com.sim2all.smsforward

import android.app.Application
import androidx.work.Configuration
import com.sim2all.smsforward.data.AppDb
import com.sim2all.smsforward.data.Settings

/**
 * 应用入口。
 *
 * 主要职责：
 * - 提供单例的 [Settings] 与 [AppDb] 访问入口
 * - 自定义 WorkManager 初始化（关闭默认 initializer，便于控制重试策略）
 */
class App : Application(), Configuration.Provider {

    val settings: Settings by lazy { Settings(this) }
    val db: AppDb by lazy { AppDb.get(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // 注意：实现 Configuration.Provider 后，WorkManager 会按需自动初始化，
        // 不要在这里手动调用 WorkManager.initialize()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    companion object {
        @Volatile
        lateinit var instance: App
            private set
    }
}
