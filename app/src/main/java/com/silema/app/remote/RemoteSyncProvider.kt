package com.silema.app.remote

import android.content.Context

object RemoteSyncProvider {
    private var instance: RemoteSync? = null

    fun get(context: Context): RemoteSync {
        instance?.let { return it }
        val config = RemoteConfig.load(context)
        val impl: RemoteSync = when {
            !config.enabled -> MockRemoteSync()
            config.provider == "mock" -> MockRemoteSync()
            else -> MockRemoteSync() // Companies replace this branch with their implementation
        }
        impl.init(config)
        instance = impl
        return impl
    }

    /** Force reset (for testing or config change) */
    fun reset() { instance = null }
}
