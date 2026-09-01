package com.silema.app.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalSource
import com.silema.app.data.VitalType
import com.silema.app.store.AppRepository
import com.silema.app.store.appRepositoryFrom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.LinkedList
import java.util.UUID
import kotlin.math.pow

/**
 * 蓝牙 BLE 直连采集层：支持 Bluetooth SIG 标准协议的设备 ——
 *  · 心率带 / 心率手环   Heart Rate Service      0x180D (测量特征 0x2A37)
 *  · 电子血压计          Blood Pressure Service  0x1810 (测量特征 0x2A35)
 *  · 脉搏血氧仪          Pulse Oximeter Service  0x1822 (连续测量 0x2A5F)
 * 数值字段采用 IEEE-11073 16-bit SFLOAT 编码，本文件含其解析实现。
 * 所有读数以接收时刻为准写入本地仓储（source=ble）。
 */
@SuppressLint("MissingPermission")
object BleVitals {

    /** 通过 Hilt EntryPoint 获取 AppRepository 单例（v0.6.0 起 AppRepository 改为 @Singleton 类）。 */
    private fun repo(context: Context): AppRepository = appRepositoryFrom(context)

    /** 保存 ApplicationContext，供 gattCallback 等成员回调使用。 */
    private var appContext: Context? = null

    private val SERVICE_HR: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    private val SERVICE_BP: UUID = UUID.fromString("00001810-0000-1000-8000-00805f9b34fb")
    private val SERVICE_PLX: UUID = UUID.fromString("00001822-0000-1000-8000-00805f9b34fb")

    private val CHAR_HR_MEASUREMENT: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
    private val CHAR_BP_MEASUREMENT: UUID = UUID.fromString("00002a35-0000-1000-8000-00805f9b34fb")
    private val CHAR_PLX_CONTINUOUS: UUID = UUID.fromString("00002a5f-0000-1000-8000-00805f9b34fb")
    private val DESC_CLIENT_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    data class FoundDevice(
        val name: String,
        val address: String,
        val kind: String // "心率" | "血压计" | "血氧仪"
    )

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _found = MutableStateFlow<List<FoundDevice>>(emptyList())
    val found: StateFlow<List<FoundDevice>> = _found.asStateFlow()

    private val _connectionState = MutableStateFlow("未连接")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private val _liveReadings = MutableStateFlow<Map<String, Double>>(emptyMap())
    val liveReadings: StateFlow<Map<String, Double>> = _liveReadings.asStateFlow()

    private var adapter: BluetoothAdapter? = null
    private var gatt: BluetoothGatt? = null
    private val subscribeQueue = LinkedList<BluetoothGattCharacteristic>()

    fun hasBluetooth(context: Context): Boolean {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        adapter = manager?.adapter ?: adapter ?: BluetoothAdapter.getDefaultAdapter()
        return adapter != null
    }

    fun startScan(context: Context): String {
        if (!hasBluetooth(context)) return "此设备没有蓝牙"
        if (adapter?.isEnabled != true) return "请先打开手机蓝牙开关"
        val scanner = adapter!!.bluetoothLeScanner ?: return "蓝牙扫描器不可用（重启蓝牙后重试）"

        val filters = listOf(SERVICE_HR, SERVICE_BP, SERVICE_PLX).map { svc ->
            ScanFilter.Builder().setServiceUuid(ParcelUuid(svc)).build()
        }
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        runCatching { scanner.startScan(filters, settings, scanCallback) }
            .onFailure { return "扫描启动失败：${it.message}" }
        _scanning.value = true
        return "扫描中… 请让设备进入配对/广播模式"
    }

    fun stopScan(context: Context) {
        runCatching { adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
        _scanning.value = false
    }

    private fun kindOf(uuids: List<ParcelUuid>?): String? = when {
        uuids == null -> null
        uuids.any { it.uuid == SERVICE_HR } -> "心率"
        uuids.any { it.uuid == SERVICE_BP } -> "血压计"
        uuids.any { it.uuid == SERVICE_PLX } -> "血氧仪"
        else -> null
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val kind = kindOf(result.scanRecord?.serviceUuids) ?: return
            if (result.device?.address == null) return
            val name = (result.scanRecord?.deviceName ?: result.device?.name ?: "未知设备").ifBlank { "未知设备" }
            val device = FoundDevice(name, result.device.address, kind)
            val current = _found.value.toMutableList()
            val existing = current.indexOfFirst { it.address == device.address }
            if (existing >= 0) current[existing] = device else current.add(device)
            _found.value = current.sortedWith(compareBy({ it.kind }, { it.name }))
        }

        override fun onScanFailed(errorCode: Int) {
            _scanning.value = false
            _connectionState.value = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "扫描已在进行中"
                else -> "扫描失败(code=$errorCode)"
            }
        }
    }

    fun disconnect() {
        runCatching { gatt?.disconnect() }
        runCatching { gatt?.close() }
        gatt = null
        subscribeQueue.clear()
        _connectionState.value = "未连接"
    }

    fun connect(context: Context, address: String) {
        if (!hasBluetooth(context)) return
        appContext = context.applicationContext
        disconnect()
        val device: BluetoothDevice = try {
            adapter!!.getRemoteDevice(address)
        } catch (e: Exception) {
            _connectionState.value = "设备地址无效"
            return
        }
        _connectionState.value = "连接中…"
        runCatching {
            gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        }.onFailure {
            _connectionState.value = "连接失败：${it.message}"
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = "已连接，发现服务中…"
                    runCatching { g.discoverServices() }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    subscribeQueue.clear()
                    _connectionState.value = "设备断开"
                    runCatching { g.close() }
                    if (gatt === g) gatt = null
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = "服务发现失败(code=$status)"
                return
            }
            subscribeQueue.clear()
            for (service in g.services ?: emptyList()) {
                for (char in service.characteristics) {
                    val propertyNotify = char.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
                    if (!propertyNotify) continue
                    when (char.uuid) {
                        CHAR_HR_MEASUREMENT, CHAR_BP_MEASUREMENT, CHAR_PLX_CONTINUOUS ->
                            subscribeQueue.add(char)
                    }
                }
            }
            if (subscribeQueue.isEmpty()) {
                _connectionState.value = "已连接，但该设备未提供可订阅的标准测量通道"
                return
            }
            subscribeNext(g)
        }

        private fun subscribeNext(g: BluetoothGatt) {
            val char = subscribeQueue.poll()
            if (char == null) {
                _connectionState.value = "已连接，等待设备推送测量数据…"
                return
            }
            val desc = char.getDescriptor(DESC_CLIENT_CONFIG)
            if (desc == null) {
                subscribeNext(g)
                return
            }
            runCatching {
                g.setCharacteristicNotification(char, true)
                desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                if (!g.writeDescriptor(desc)) {
                    subscribeNext(g)
                }
            }.onFailure { subscribeNext(g) }
        }

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            subscribeNext(g)
        }

        // Android 13+ 使用新回调；旧系统走旧回调。两版都解析。
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handle(characteristic.uuid, value)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            @Suppress("DEPRECATION")
            val value = characteristic.value ?: return
            handle(characteristic.uuid, value)
        }
    }

    private fun handle(uuid: UUID, payload: ByteArray) {
        val ctx = appContext ?: return
        val now = System.currentTimeMillis()
        when (uuid) {
            CHAR_HR_MEASUREMENT -> {
                val bpm = BleCodec.parseHeartRate(payload) ?: return
                putLive("心率", bpm)
                repo(ctx).addRecord(VitalRecord.of(VitalType.HEART_RATE, bpm, now, VitalSource.BLE))
            }
            CHAR_BP_MEASUREMENT -> {
                val bp = BleCodec.parseBloodPressure(payload) ?: return
                putLive("收缩压", bp[0])
                putLive("舒张压", bp[1])
                repo(ctx).addRecord(VitalRecord.of(VitalType.SYSTOLIC, bp[0], now, VitalSource.BLE))
                repo(ctx).addRecord(VitalRecord.of(VitalType.DIASTOLIC, bp[1], now, VitalSource.BLE))
                _connectionState.value = "收到血压：${bp[0].toInt()}/${bp[1].toInt()} mmHg"
            }
            CHAR_PLX_CONTINUOUS -> {
                val plx = BleCodec.parsePulseOx(payload) ?: return
                putLive("血氧", plx.first)
                putLive("脉率", plx.second)
                repo(ctx).addRecord(VitalRecord.of(VitalType.SPO2, plx.first, now, VitalSource.BLE))
                _connectionState.value = "收到血氧：${plx.first.toInt()}%"
            }
        }
    }

    private fun putLive(label: String, v: Double) {
        _liveReadings.value = _liveReadings.value + (label to v)
    }
}
