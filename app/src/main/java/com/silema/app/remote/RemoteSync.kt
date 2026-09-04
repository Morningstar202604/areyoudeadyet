package com.silema.app.remote

import com.silema.app.data.AlertItem
import com.silema.app.data.VitalRecord

interface RemoteSync {
    /** Initialize with config. Called once at app startup. */
    fun init(config: RemoteConfig)

    /** Check if remote backend is configured and available */
    suspend fun isAvailable(): Boolean

    /** Authenticate with remote backend (phone/email/token) */
    suspend fun login(
        credential: String,
        password: String,
    ): Result<AuthResult>

    /** Push local records to remote. Returns number of records synced. */
    suspend fun pushRecords(records: List<VitalRecord>): Result<Int>

    /** Pull records from remote. Returns new records not yet local. */
    suspend fun pullRecords(sinceTimestamp: Long): Result<List<VitalRecord>>

    /** Get family members this user can monitor remotely */
    suspend fun getFamilyMembers(): Result<List<FamilyMember>>

    /** Get remote vital data for a specific family member */
    suspend fun getFamilyVitals(
        memberId: String,
        sinceTimestamp: Long,
    ): Result<List<VitalRecord>>

    /** Push alert notification to family members */
    suspend fun pushAlert(
        memberId: String,
        alert: AlertItem,
    ): Result<Unit>

    /** Logout */
    suspend fun logout()

    /** Get sync status */
    fun getSyncStatus(): SyncStatus
}

data class AuthResult(
    val userId: String,
    val token: String,
    val displayName: String,
    val expiresAt: Long,
)

data class FamilyMember(
    val id: String,
    val name: String,
    // "父亲", "母亲", "配偶", "子女"
    val relationship: String,
    val avatarUrl: String?,
    val lastOnline: Long,
)

data class SyncStatus(
    val lastSyncTime: Long,
    val pendingUploads: Int,
    val isSyncing: Boolean,
    val error: String?,
)
