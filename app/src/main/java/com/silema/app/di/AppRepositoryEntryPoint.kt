package com.silema.app.di

import com.silema.app.store.AppRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint：让非 Hilt 管理的对象（如 Service、BroadcastReceiver、顶层函数）
 * 也能从 Hilt 容器获取 [AppRepository] 单例。
 *
 * 使用方式：
 * ```
 * val entryPoint = EntryPointAccessors.fromApplication(context, AppRepositoryEntryPoint::class.java)
 * val repo = entryPoint.appRepository()
 * ```
 *
 * v0.6.0 起 AppRepository 已重构为 @Singleton 类，可直接通过构造函数注入；
 * 此 EntryPoint 仅用于无法直接注入的场景。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppRepositoryEntryPoint {
    fun appRepository(): AppRepository
}
