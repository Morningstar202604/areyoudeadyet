package com.silema.app.ai

import android.content.Context

object AiAnalyzerProvider {
    private var instance: AiAnalyzer? = null
    private var lastApiKey: String? = null

    /**
     * 获取 AI 分析器实例：优先使用云端 API（用户配置），失败时自动降级到本地规则引擎。
     * @param context Android 上下文
     * @param apiKey 可选的 NewAPI 中转站 Key，为空时使用本地模式
     * @param baseUrl API 基础 URL，默认 https://api.hcnsec.cn/v1
     * @param model 模型名称，默认 qwen-plus
     */
    fun get(
        context: Context,
        apiKey: String? = null,
        baseUrl: String = "https://api.hcnsec.cn/v1",
        model: String = "qwen-plus",
    ): AiAnalyzer {
        // 如果 API Key 变化了，重建实例
        if (apiKey != lastApiKey) {
            instance = null
            lastApiKey = apiKey
        }

        instance?.let { return it }

        val impl: AiAnalyzer =
            if (!apiKey.isNullOrEmpty()) {
                // 尝试云端模式
                CloudAiAnalyzer(baseUrl = baseUrl, apiKey = apiKey, model = model)
            } else {
                // 本地备用模式
                LocalAiAnalyzer()
            }
        instance = impl
        return impl
    }

    /** 重置实例（配置变更时调用） */
    fun reset() {
        instance = null
        lastApiKey = null
    }
}
