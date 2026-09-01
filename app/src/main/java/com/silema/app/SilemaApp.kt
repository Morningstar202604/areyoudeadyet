package com.silema.app

import android.app.Application
import com.silema.app.di.AppRepositoryEntryPoint
import com.silema.app.work.Reminders
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * 应用入口。
 *
 * v0.5.0 起接入 Hilt 依赖注入与 Timber 日志。
 * v0.6.0 起 AppRepository 重构为 @Singleton 类，通过 Hilt 注入自动初始化，
 * 不再需要手动调用 AppRepository.init()。此处通过 EntryPoint 触发首次创建，
 * 确保 Room 数据流订阅和旧版 JSON 迁移在应用启动时立即执行。
 */
@HiltAndroidApp
class SilemaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化日志（Debug 输出到 Logcat；Release 可扩展为崩溃上报）
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // 触发 AppRepository 首次创建（@Singleton 懒加载，通过 EntryPoint 获取实例触发初始化）
        val entryPoint =
            EntryPointAccessors.fromApplication(
                this,
                AppRepositoryEntryPoint::class.java,
            )
        entryPoint.appRepository() // 触发初始化（Room 订阅 + JSON 迁移）
        // 创建通知渠道（测量提醒、久坐提醒）
        Reminders.ensureChannel(this)
        Timber.i("SilemaApp initialized: version=${BuildConfig.VERSION_NAME}")
    }
}
