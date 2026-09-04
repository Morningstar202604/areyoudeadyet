package com.silema.app.di

import android.content.Context
import com.silema.app.ai.AiAnalyzer
import com.silema.app.ai.AiAnalyzerProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 模块：提供 AI 分析器实例。
 *
 * 替代原来的静态单例 [AiAnalyzerProvider]，实现依赖注入，
 * 便于测试替换 Mock 实现。
 */
@Module
@InstallIn(SingletonComponent::class)
object AiAnalyzerModule {
    @Provides
    @Singleton
    fun provideAiAnalyzer(
        @ApplicationContext context: Context,
    ): AiAnalyzer = AiAnalyzerProvider.get(context)
}
