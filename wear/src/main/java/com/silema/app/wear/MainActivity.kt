package com.silema.app.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.silema.app.wear.data.WearStore

/**
 * 手表端主入口（主产品，给老人家佩戴）。
 * 仅做本地初始化 + 挂载 Compose，不含任何云端/模拟逻辑。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 手表端本地持久化：体征与运动都先存在手表自身文件里，
        // 与手机端同步（蓝牙 DataClient）是后续阶段，不在本 MVP 内。
        WearStore.init(filesDir)
        setContent {
            WearApp()
        }
    }
}
