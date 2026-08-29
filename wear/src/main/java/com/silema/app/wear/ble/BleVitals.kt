package com.silema.app.wear.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalSource
import com.silema.app.data.VitalType
import com.silema.app.ble.BleCodec
import com.silema.app.wear.data.WearStore
import java.util.UUID

/** Wear OS BLE 客户端 */
class BleVitals(private val context: Context) {
    // ... (rest of the code remains the same until line 135)

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var gatt: BluetoothGatt? = null
    private var scanCallback: ScanCallback? = null

    /** 启动扫描，发现设备后自动连接 */
    fun startScan(onDeviceFound: (BluetoothDevice) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothManager = context.getSystemService(BluetoothManager::class.java)
            bluetoothAdapter = bluetoothManager?.adapter
        } else {
            @Suppress("DEPRECATION")
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        }

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            return
        }

        val scanner = bluetoothManager?.adapter?.bluetoothLeScanner ?: return

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                // 检查是否为目标服务 UUID
                val serviceUuids = result.scanRecord?.serviceUuids ?: return
                val targetUuids = listOf(
                    UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"), // Heart Rate
                    UUID.fromString("00001810-0000-1000-8000-00805f9b34fb"), // Blood Pressure
                    UUID.fromString("00001822-0000-1000-8000-00805f9b34fb")  // Pulse Oximetry
                )
                if (serviceUuids.any { it.uuid in targetUuids }) {
                    scanner.stopScan(this)
                    onDeviceFound(device)
                    connectToDevice(device)
                }
            }
        }

        scanner.startScan(scanCallback)
    }

    /** 连接到指定设备 */
    private fun connectToDevice(device: BluetoothDevice) {
        gatt = device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // 查找并订阅目标服务
                gatt.services.forEach { service ->
                    when (service.uuid.toString().lowercase()) {
                        "0000180d-0000-1000-8000-00805f9b34fb" -> {
                            // Heart Rate Service
                            val hrChar = service.getCharacteristic(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"))
                            hrChar?.let {
                                gatt.setCharacteristicNotification(it, true)
                                val descriptor = it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(descriptor)
                            }
                        }
                        "00001810-0000-1000-8000-00805f9b34fb" -> {
                            // Blood Pressure Service
                            val bpChar = service.getCharacteristic(UUID.fromString("00002a35-0000-1000-8000-00805f9b34fb"))
                            bpChar?.let {
                                gatt.setCharacteristicNotification(it, true)
                                val descriptor = it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(descriptor)
                            }
                        }
                        "00001822-0000-1000-8000-00805f9b34fb" -> {
                            // Pulse Oximetry Service
                            val spo2Char = service.getCharacteristic(UUID.fromString("00002a5e-0000-1000-8000-00805f9b34fb"))
                            spo2Char?.let {
                                gatt.setCharacteristicNotification(it, true)
                                val descriptor = it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(descriptor)
                            }
                        }
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val value = characteristic.value ?: return
            when (characteristic.uuid.toString().lowercase()) {
                "00002a37-0000-1000-8000-00805f9b34fb" -> {
                    // Heart Rate
                    val hr = BleCodec.parseHeartRate(value)
                    if (hr != null) {
                        WearStore.addRecord(
                            VitalRecord.of(VitalType.HEART_RATE, hr, System.currentTimeMillis(), VitalSource.BLE)
                        )
                    }
                }
                "00002a35-0000-1000-8000-00805f9b34fb" -> {
                    // Blood Pressure
                    val bpValues = BleCodec.parseBloodPressure(value)
                    if (bpValues != null && bpValues.size >= 2) {
                        WearStore.addRecord(
                            VitalRecord.of(VitalType.SYSTOLIC, bpValues[0], System.currentTimeMillis(), VitalSource.BLE)
                        )
                        WearStore.addRecord(
                            VitalRecord.of(VitalType.DIASTOLIC, bpValues[1], System.currentTimeMillis(), VitalSource.BLE)
                        )
                    }
                }
                "00002a5e-0000-1000-8000-00805f9b34fb" -> {
                    // SpO2
                    val spo2 = BleCodec.sfloat(value, 0)
                    if (spo2 != null) {
                        WearStore.addRecord(
                            VitalRecord.of(VitalType.SPO2, spo2, System.currentTimeMillis(), VitalSource.BLE)
                        )
                    }
                }
            }
        }
    }

    fun stop() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
    }
}
