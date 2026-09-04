package com.silema.app.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * 语音播报：危险告警自动朗读 + 手动朗读当前状态。
 * 使用系统自带 TTS，离线可用；初始化失败时静默降级为纯文字展示。
 */
class TtsController(
    context: Context,
) {
    private var engine: TextToSpeech? = null

    @Volatile
    var ready: Boolean = false
        private set

    init {
        runCatching {
            engine =
                TextToSpeech(context.applicationContext) { status ->
                    ready = status == TextToSpeech.SUCCESS && applyLocale()
                }
        }
    }

    private fun applyLocale(): Boolean =
        runCatching {
            engine?.language = Locale.CHINA
            true
        }.getOrDefault(false)

    fun speak(text: String) {
        if (!ready) return
        runCatching {
            engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "silema_${System.nanoTime()}")
        }
    }

    fun shutdown() {
        runCatching { engine?.stop() }
        runCatching { engine?.shutdown() }
        engine = null
        ready = false
    }
}
