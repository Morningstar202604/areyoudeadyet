package com.silema.app.wear.datalayer

import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Wear OS Data Layer 监听服务
 * 接收来自手机的数据更新
 */
class WearDataLayerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var dataLayerClient: WearDataLayerClient

    override fun onCreate() {
        super.onCreate()
        dataLayerClient = WearDataLayerClient(applicationContext)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        dataLayerClient.onDataChanged(dataEvents)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
