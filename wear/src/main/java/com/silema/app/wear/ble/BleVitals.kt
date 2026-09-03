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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/** Wear OS BLE 客户端 */
class BleVitals(private val context: Context) {

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var gatt: BluetoothGatt? = null
    private var scanCallback: ScanCallback? = null

    // 心率状态
    private val _heartRate = MutableStateFlow(0.0)
    val heartRate: StateFlow<Double> = _heartRate.asStateFlow()

    // 血压状态
    private val _systolic = MutableStateFlow(0.0)
    val systolic: StateFlow<Double> = _systolic.asStateFlow()

    private val _diastolic = MutableStateFlow(0.0)
    val diastolic: StateFlow<Double> = _diastolic.asStateFlow()

    // 血氧状态
    private val _spo2 = MutableStateFlow(0.0)
    val spo2: StateFlow<Double> = _spo2.asStateFlow()

    // 连接状态
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

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
                _isConnected.value = true
                gatt.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                _isConnected.value = false
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
                        _heartRate.value = hr
                        WearStore.addRecord(
                            VitalRecord.of(VitalType.HEART_RATE, hr, System.currentTimeMillis(), VitalSource.BLE)
                        )
                    }
                }
                "00002a35-0000-1000-8000-00805f9b34fb" -> {
                    // Blood Pressure
                    val bpValues = BleCodec.parseBloodPressure(value)
                    if (bpValues != null && bpValues.size >= 2) {
                        _systolic.value = bpValues[0]
                        _diastolic.value = bpValues[1]
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
                    val spo2Value = BleCodec.sfloat(value, 0)
                    if (spo2Value != null) {
                        _spo2.value = spo2Value
                        WearStore.addRecord(
                            VitalRecord.of(VitalType.SPO2, spo2Value, System.currentTimeMillis(), VitalSource.BLE)
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
        _isConnected.value = false
    }

    /**
     * 启动心率监测
     * 自动扫描并连接支持心率服务的 BLE 设备
     */
    fun startHeartRateMonitoring() {
        startScan { device ->
            // 设备发现后自动连接
            connectToDevice(device)
        }
    }

    /**
     * 停止监测
     */
    fun stopMonitoring() {
        stop()
    }

    /**
     * 获取当前心率值
     */
    fun getCurrentHeartRate(): Double {
        return _heartRate.value
    }

    /**
     * 获取当前血压值
     */
    fun getCurrentBloodPressure(): Pair<Double, Double> {
        return Pair(_systolic.value, _diastolic.value)
    }

    /**
     * 获取当前血氧值
     */
    fun getCurrentSpo2(): Double {
        return _spo2.value
    }

    /**
     * 检查是否已连接
     */
    fun isDeviceConnected(): Boolean {
        return _isConnected.value
    }
}
