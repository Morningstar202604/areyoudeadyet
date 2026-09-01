package com.silema.app.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import com.silema.app.wear.ble.BleVitals
import com.silema.app.wear.data.WearStore
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * 手表端主入口（主产品，给老人家佩戴）。
 * 自动扫描并连接 BLE 医疗设备（心率带/血压计/血氧仪）
 *
 * v0.5.0 起添加 [AndroidEntryPoint] 以支持 Hilt 注入。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var bleVitals: BleVitals? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化本地持久化
        WearStore.init(filesDir)

        // 检查并请求 BLE 权限后启动扫描
        checkAndRequestPermissions()

        setContent {
            WearApp()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                )
            }

        val missingPermissions =
            permissions.filter {
                ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

        if (missingPermissions.isEmpty()) {
            startBleScan()
        } else {
            // 首次启动时请求权限
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 1001)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startBleScan()
        } else {
            Timber.w("BLE permissions denied, scan not started")
        }
    }

    private fun startBleScan() {
        bleVitals =
            BleVitals(applicationContext).apply {
                startScan { device ->
                    // 发现设备后自动连接（在 BleVitals 内部处理）
                    Timber.d("Found BLE device: ${device.name ?: "Unknown"}")
                }
            }
        Timber.i("BLE scan started")
    }

    override fun onDestroy() {
        super.onDestroy()
        bleVitals?.stop()
        Timber.d("MainActivity destroyed, BLE stopped")
    }
}
