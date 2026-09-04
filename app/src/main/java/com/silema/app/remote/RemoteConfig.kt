package com.silema.app.remote

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RemoteConfig(
    val enabled: Boolean = false,
    // "custom" | "aliyun" | "tencent" | "mock"
    val provider: String = "custom",
    // e.g. "https://api.example.com/v1"
    val baseUrl: String = "",
    val apiKey: String = "",
    val projectId: String = "",
    // auto-sync interval
    val syncIntervalMinutes: Int = 30,
    // AI health analysis toggle
    val enableAiAnalysis: Boolean = false,
    // "custom" | "qwen" | "zhipu" | "wenxin"
    val aiProvider: String = "custom",
    val aiApiKey: String = "",
    val aiBaseUrl: String = "",
    // FHIR export toggle
    val fhirEnabled: Boolean = false,
    val fhirVersion: String = "R4",
) {
    companion object {
        private var cached: RemoteConfig? = null

        fun load(context: Context): RemoteConfig {
            cached?.let { return it }
            val config =
                try {
                    val json = context.assets.open("remote_config.json").bufferedReader().readText()
                    Json.decodeFromString<RemoteConfig>(json)
                } catch (_: Exception) {
                    RemoteConfig()
                }
            cached = config
            return config
        }

        /** 配置可能被外部替换（如调试/热更新）时清除缓存，下次 load 重新读取。 */
        fun clearCache() {
            cached = null
        }
    }
}
