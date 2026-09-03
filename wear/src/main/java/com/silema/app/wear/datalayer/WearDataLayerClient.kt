package com.silema.app.wear.datalayer

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalSource
import com.silema.app.data.VitalType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class WearDataLayerClient(private val context: Context) {

    companion object {
        const val PATH_VITAL_RECORDS = "/vital_records"
        const val PATH_HEART_RATE = "/heart_rate"
        const val PATH_SYNC_REQUEST = "/sync_request"
        const val PATH_SYNC_RESPONSE = "/sync_response"

        const val KEY_RECORDS_JSON = "records_json"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_HEART_RATE = "heart_rate"

        private val URI_VITAL_RECORDS: Uri = Uri.parse("wear://*$/vital_records")
        private val URI_HEART_RATE: Uri = Uri.parse("wear://*$/heart_rate")
    }

    private val dataClient: DataClient = Wearable.getDataClient(context)

    private val _heartRateFlow = MutableSharedFlow<Double>(replay = 1)
    val heartRateFlow: SharedFlow<Double> = _heartRateFlow.asSharedFlow()

    private val _syncStateFlow = MutableSharedFlow<SyncState>(replay = 1)
    val syncStateFlow: SharedFlow<SyncState> = _syncStateFlow.asSharedFlow()

    sealed class SyncState {
        data object Idle : SyncState()
        data object Syncing : SyncState()
        data class Success(val recordsCount: Int) : SyncState()
        data class Error(val message: String) : SyncState()
    }

    private fun recordsToJson(records: List<VitalRecord>): String {
        val jsonArray = JSONArray()
        for (record in records) {
            val obj = JSONObject().apply {
                put("typeId", record.typeId)
                put("value", record.value)
                put("timestamp", record.timestampMillis)
                put("source", record.source)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    suspend fun sendVitalRecords(records: List<VitalRecord>): Boolean = withContext(Dispatchers.IO) {
        try {
            val putRequest = PutDataMapRequest.create(PATH_VITAL_RECORDS).apply {
                dataMap.putString(KEY_RECORDS_JSON, recordsToJson(records))
                dataMap.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            }

            val request = putRequest.asPutDataRequest().setUrgent()
            dataClient.putDataItem(request).await()
            _syncStateFlow.tryEmit(SyncState.Success(records.size))
            true
        } catch (e: Exception) {
            _syncStateFlow.tryEmit(SyncState.Error(e.message ?: "Unknown error"))
            false
        }
    }

    suspend fun sendHeartRate(heartRate: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            val putRequest = PutDataMapRequest.create(PATH_HEART_RATE).apply {
                dataMap.putDouble(KEY_HEART_RATE, heartRate)
                dataMap.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            }

            val request = putRequest.asPutDataRequest().setUrgent()
            dataClient.putDataItem(request).await()
            _heartRateFlow.tryEmit(heartRate)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun requestSync(): Boolean = withContext(Dispatchers.IO) {
        try {
            val putRequest = PutDataMapRequest.create(PATH_SYNC_REQUEST).apply {
                dataMap.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            }

            val request = putRequest.asPutDataRequest().setUrgent()
            dataClient.putDataItem(request).await()
            _syncStateFlow.tryEmit(SyncState.Syncing)
            true
        } catch (e: Exception) {
            _syncStateFlow.tryEmit(SyncState.Error(e.message ?: "Sync request failed"))
            false
        }
    }

    fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            val uri = event.dataItem.uri
            when (uri.path) {
                PATH_HEART_RATE -> {
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val heartRate = dataMapItem.dataMap.getDouble(KEY_HEART_RATE)
                    _heartRateFlow.tryEmit(heartRate)
                }
                PATH_SYNC_RESPONSE -> {
                    _syncStateFlow.tryEmit(SyncState.Idle)
                }
            }
        }
    }

    suspend fun getSyncedRecords(): List<VitalRecord> = withContext(Dispatchers.IO) {
        val records = mutableListOf<VitalRecord>()
        try {
            val dataItems = dataClient.getDataItems(URI_VITAL_RECORDS).await()
            for (item in dataItems) {
                val dataMapItem = DataMapItem.fromDataItem(item)
                val jsonStr = dataMapItem.dataMap.getString(KEY_RECORDS_JSON) ?: continue
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val typeId = obj.getString("typeId")
                    val value = obj.getDouble("value")
                    val timestamp = obj.getLong("timestamp")
                    val source = obj.optString("source", VitalSource.MANUAL)

                    VitalType.fromId(typeId)?.let {
                        records.add(VitalRecord(typeId, value, timestamp, source))
                    }
                }
            }
        } catch (_: Exception) {
            // silent
        }
        records
    }

    fun disconnect() {
        // Data Client does not need explicit disconnect
    }
}
