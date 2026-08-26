package com.silema.app.remote

import com.silema.app.data.AlertItem
import com.silema.app.data.VitalRecord

/**
 * Mock implementation for offline mode and demo.
 * All operations return success with empty/stub data.
 * Companies replace this with their actual RemoteSync implementation.
 */
class MockRemoteSync : RemoteSync {
    private var config = RemoteConfig()
    private var loggedIn = false

    override fun init(config: RemoteConfig) { this.config = config }

    override suspend fun isAvailable(): Boolean = config.enabled && config.baseUrl.isNotBlank()

    override suspend fun login(credential: String, password: String): Result<AuthResult> {
        loggedIn = true
        return Result.success(AuthResult(
            userId = "demo_user",
            token = "mock_token",
            displayName = "演示用户",
            expiresAt = System.currentTimeMillis() + 86400000L
        ))
    }

    override suspend fun pushRecords(records: List<VitalRecord>): Result<Int> = Result.success(0)

    override suspend fun pullRecords(sinceTimestamp: Long): Result<List<VitalRecord>> = Result.success(emptyList())

    override suspend fun getFamilyMembers(): Result<List<FamilyMember>> = Result.success(listOf(
        FamilyMember("demo_1", "爸爸", "父亲", null, System.currentTimeMillis()),
        FamilyMember("demo_2", "妈妈", "母亲", null, System.currentTimeMillis())
    ))

    override suspend fun getFamilyVitals(memberId: String, sinceTimestamp: Long): Result<List<VitalRecord>> = Result.success(emptyList())

    override suspend fun pushAlert(memberId: String, alert: AlertItem): Result<Unit> = Result.success(Unit)

    override suspend fun logout() { loggedIn = false }

    override fun getSyncStatus() = SyncStatus(
        lastSyncTime = 0L,
        pendingUploads = 0,
        isSyncing = false,
        error = null
    )
}
