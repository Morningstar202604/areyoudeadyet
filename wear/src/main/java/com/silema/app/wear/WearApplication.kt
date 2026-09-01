package com.silema.app.wear

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * 手表端 Application 入口。
 *
 * v0.5.0 起接入 Hilt 依赖注入与 Timber 日志，与手机端架构对齐。
 * - [HiltAndroidApp] 触发 Hilt 代码生成
 * - Debug 构建安装 [Timber.DebugTree]
 */
@HiltAndroidApp
class WearApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.i("WearApplication initialized: version=${BuildConfig.VERSION_NAME}")
    }
}
