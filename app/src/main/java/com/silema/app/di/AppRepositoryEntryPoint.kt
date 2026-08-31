package com.silema.app.di

import com.silema.app.db.SilemaDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint：让非 Hilt 管理的对象（如 object 单例 [com.silema.app.store.AppRepository]）
 * 也能从 Hilt 容器获取依赖。
 *
 * 这是渐进式迁移的中间形态：AppRepository 暂保留 object 形式以避免大面积修改调用处，
 * 后续可改为 @Singleton 类 + 构造函数注入。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppRepositoryEntryPoint {
    fun database(): SilemaDatabase
}
