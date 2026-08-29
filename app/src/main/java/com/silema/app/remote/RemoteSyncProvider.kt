package com.silema.app.remote

import android.content.Context

object RemoteSyncProvider {
    private var instance: RemoteSync? = null

    fun get(context: Context): RemoteSync {
        instance?.let { return it }
        // 真实本地能力：FHIR 导出到本机文件 + 系统分享；非云端实时监护。
        val impl: RemoteSync = LocalExportSync(context)
        impl.init(RemoteConfig())
        instance = impl
        return impl
    }

    /** Force reset (for testing or config change) */
    fun reset() {
        instance = null
        RemoteConfig.clearCache()
    }
}
