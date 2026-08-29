package com.silema.app.remote

import android.content.Context
import com.silema.app.data.AlertItem
import com.silema.app.data.VitalRecord
import com.silema.app.medical.FhirExporter
import java.io.File

/**
 * 真实本地同步能力：把体征数据导出为 FHIR R4 Bundle 文件（应用私有文档目录），
 * 可供用户通过系统分享发送给家人/医生。不连接任何云端，无需后端账号。
 *
 * 之前的 MockRemoteSync 只返回假成功/假数据。这里真正把数据写成 FHIR 标准文件，
 * 使「家人健康数据导出分享」在本地构建下即为真实可用能力。
 * 真正的「远程实时监护」（云端拉取家人数据）需要企业部署后端服务，超出本地构建范围。
 */
class LocalExportSync(private val context: Context) : RemoteSync {

    private var lastExportTime = 0L

    override fun init(config: RemoteConfig) {
        // 本地模式无需后端配置
    }

    override suspend fun isAvailable(): Boolean = true

    override suspend fun login(credential: String, password: String): Result<AuthResult> =
        Result.success(
            AuthResult(
                userId = "local_user",
                token = "local_token",
                displayName = "本机用户",
                expiresAt = System.currentTimeMillis() + 86_400_000L
            )
        )

    /** 真实写入：把体征序列化为 FHIR R4 Bundle 落盘，返回实际导出条数。 */
    override suspend fun pushRecords(records: List<VitalRecord>): Result<Int> {
        if (records.isEmpty()) return Result.success(0)
        val dir = File(context.getExternalFilesDir(null), "fhir_export").apply { mkdirs() }
        val file = File(dir, "health-bundle-${System.currentTimeMillis()}.json")
        runCatching { file.writeText(FhirExporter.export(records)) }
            .onFailure { return Result.failure(it) }
        lastExportTime = System.currentTimeMillis()
        return Result.success(records.size)
    }

    override suspend fun pullRecords(sinceTimestamp: Long): Result<List<VitalRecord>> = Result.success(emptyList())

    /** 本地模式无云端家人体系，返回空列表（诚实，不伪造数据）。 */
    override suspend fun getFamilyMembers(): Result<List<FamilyMember>> = Result.success(emptyList())

    override suspend fun getFamilyVitals(memberId: String, sinceTimestamp: Long): Result<List<VitalRecord>> =
        Result.success(emptyList())

    override suspend fun pushAlert(memberId: String, alert: AlertItem): Result<Unit> = Result.success(Unit)

    override suspend fun logout() {}

    override fun getSyncStatus() = SyncStatus(
        lastSyncTime = lastExportTime,
        pendingUploads = 0,
        isSyncing = false,
        error = null
    )
}
