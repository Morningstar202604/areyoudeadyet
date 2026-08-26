package com.silema.app.ai

import android.content.Context
import com.silema.app.remote.RemoteConfig

object AiAnalyzerProvider {
    private var instance: AiAnalyzer? = null
    
    fun get(context: Context): AiAnalyzer {
        instance?.let { return it }
        val config = RemoteConfig.load(context)
        val impl: AiAnalyzer = if (config.enableAiAnalysis && config.aiApiKey.isNotBlank()) {
            // Companies plug in their AI SDK here (e.g., Qwen, Zhipu, Wenxin)
            MockAiAnalyzer()
        } else {
            MockAiAnalyzer()
        }
        instance = impl
        return impl
    }
    
    fun reset() { instance = null }
}