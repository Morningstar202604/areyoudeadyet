package com.silema.app.remote

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RemoteConfig(
    val enabled: Boolean = false,
    val provider: String = "custom",        // "custom" | "aliyun" | "tencent" | "mock"
    val baseUrl: String = "",               // e.g. "https://api.example.com/v1"
    val apiKey: String = "",
    val projectId: String = "",
    val syncIntervalMinutes: Int = 30,      // auto-sync interval
    val enableAiAnalysis: Boolean = false,  // AI health analysis toggle
    val aiProvider: String = "custom",      // "custom" | "qwen" | "zhipu" | "wenxin"
    val aiApiKey: String = "",
    val aiBaseUrl: String = "",
    val fhirEnabled: Boolean = false,       // FHIR export toggle
    val fhirVersion: String = "R4"
) {
    companion object {
        private var cached: RemoteConfig? = null

        fun load(context: Context): RemoteConfig {
            cached?.let { return it }
            val config = try {
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
