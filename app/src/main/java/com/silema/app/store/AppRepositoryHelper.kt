package com.silema.app.store

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.silema.app.di.AppRepositoryEntryPoint
import dagger.hilt.android.EntryPointAccessors

/**
 * 在 Composable 中获取 [AppRepository] 单例的辅助函数。
 *
 * 使用方式：
 * ```
 * val repository = rememberAppRepository()
 * val records by repository.records.collectAsState()
 * ```
 *
 * v0.6.0 起 AppRepository 重构为 @Singleton 类，不再支持静态访问；
 * 此函数通过 Hilt EntryPoint 获取实例，适用于尚未迁移到 ViewModel 的 UI 屏幕。
 * 新代码优先使用 [androidx.lifecycle.viewmodel.compose.viewModel] + @HiltViewModel。
 */
@Composable
fun rememberAppRepository(): AppRepository {
    val context = LocalContext.current
    return remember(context) {
        appRepositoryFrom(context)
    }
}

/**
 * 从 Context 获取 [AppRepository] 单例（非 Composable 场景使用）。
 *
 * 适用于 Service、BroadcastReceiver、工具类等无法直接注入的场景。
 */
fun appRepositoryFrom(context: Context): AppRepository =
    EntryPointAccessors
        .fromApplication(
            context.applicationContext,
            AppRepositoryEntryPoint::class.java,
        ).appRepository()
