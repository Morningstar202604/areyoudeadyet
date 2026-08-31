package com.silema.app

import android.app.Application
import com.silema.app.store.AppRepository
import com.silema.app.work.Reminders
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * 应用入口。
 *
 * v0.5.0 起接入 Hilt 依赖注入与 Timber 日志。
 * - [HiltAndroidApp] 触发 Hilt 代码生成，全局依赖容器由此开始
 * - Debug 构建安装 [Timber.DebugTree]，Release 可按需安装 CrashlyticsTree 等
 */
@HiltAndroidApp
class SilemaApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 初始化日志（Debug 输出到 Logcat；Release 可扩展为崩溃上报）
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // 初始化本地仓储（Room + 旧版 JSON 迁移）
        AppRepository.init(this)

        // 创建通知渠道（测量提醒、久坐提醒）
        Reminders.ensureChannel(this)

        Timber.i("SilemaApp initialized: version=${BuildConfig.VERSION_NAME}")
    }
}
