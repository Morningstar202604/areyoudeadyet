# 远程同步配置指南

## 概述

「死了吗？」支持可插拔的远程后端同步。**我们不提供服务器**，您需要自行部署后端服务。

配置步骤：

1. 将 `app/src/main/assets/remote_config.json` 复制到您的项目
2. 修改配置项指向您的后端
3. 实现 `RemoteSync` 接口（参考 `MockRemoteSync.kt`）
4. 构建并部署

## 配置文件

编辑 `app/src/main/assets/remote_config.json`：

```json
{
  "enabled": true,
  "provider": "custom",
  "baseUrl": "https://your-api-server.com/v1",
  "apiKey": "your-api-key-here",
  "projectId": "your-project-id",
  "syncIntervalMinutes": 30,
  "enableAiAnalysis": true,
  "aiProvider": "qwen",
  "aiApiKey": "your-ai-api-key",
  "aiBaseUrl": "https://dashscope.aliyuncs.com/api/v1",
  "fhirEnabled": true,
  "fhirVersion": "R4"
}
```

## 配置项说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `enabled` | Boolean | 是否启用远程同步 |
| `provider` | String | 后端提供商：`mock` / `custom` / `aliyun` / `tencent` |
| `baseUrl` | String | API 服务器地址 |
| `apiKey` | String | API 密钥 |
| `projectId` | String | 项目 ID |
| `syncIntervalMinutes` | Int | 自动同步间隔（分钟） |
| `enableAiAnalysis` | Boolean | 是否启用 AI 健康分析 |
| `aiProvider` | String | AI 提供商：`custom` / `qwen` / `zhipu` / `wenxin` |
| `aiApiKey` | String | AI API 密钥 |
| `aiBaseUrl` | String | AI API 地址 |
| `fhirEnabled` | Boolean | 是否启用 FHIR 导出 |
| `fhirVersion` | String | FHIR 版本（默认 R4） |

## 实现自定义后端

### 1. 实现 RemoteSync 接口

参考 `app/src/main/java/com/silema/app/remote/MockRemoteSync.kt`，实现以下方法：

```kotlin
class YourRemoteSync : RemoteSync {
    override suspend fun isAvailable(): Boolean { ... }
    override suspend fun login(credential: String, password: String): Result<AuthResult> { ... }
    override suspend fun pushRecords(records: List<VitalRecord>): Result<Int> { ... }
    override suspend fun pullRecords(sinceTimestamp: Long): Result<List<VitalRecord>> { ... }
    override suspend fun getFamilyMembers(): Result<List<FamilyMember>> { ... }
    override suspend fun getFamilyVitals(memberId: String, sinceTimestamp: Long): Result<List<VitalRecord>> { ... }
    override suspend fun pushAlert(memberId: String, alert: AlertItem): Result<Unit> { ... }
    override suspend fun logout() { ... }
    override fun getSyncStatus(): SyncStatus { ... }
}
```

### 2. 注册到 Provider

在 `RemoteSyncProvider.kt` 中添加您的实现：

```kotlin
val impl: RemoteSync = when {
    !config.enabled -> MockRemoteSync()
    config.provider == "mock" -> MockRemoteSync()
    config.provider == "custom" -> YourRemoteSync()  // 添加这行
    else -> MockRemoteSync()
}
```

### 3. 推荐后端方案

#### 阿里云方案
- **函数计算 FC**：Serverless API
- **表格存储 OTS**：体征数据存储
- **消息推送 MNS**：告警推送
- **对象存储 OSS**：健康报告文件

#### 腾讯云方案
- **云函数 SCF**：Serverless API
- **云数据库 MongoDB**：体征数据
- **消息推送 TBP**：告警推送
- **对象存储 COS**：报告文件

#### 自建方案
- **后端**：Spring Boot / FastAPI / Gin
- **数据库**：PostgreSQL / MySQL / MongoDB
- **缓存**：Redis
- **消息队列**：RabbitMQ / Kafka
- **部署**：Docker + K8s

## AI 分析配置

AI 分析支持国内大模型：

| 提供商 | aiProvider 值 | API 地址 |
|--------|--------------|----------|
| 通义千问 | `qwen` | https://dashscope.aliyuncs.com/api/v1 |
| 智谱 AI | `zhipu` | https://open.bigmodel.cn/api/paas/v4 |
| 文心一言 | `wenxin` | https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop |
| 自定义 | `custom` | 自行填写 |

## FHIR 标准

FHIR R4 是医疗信息互操作国际标准。导出的 JSON 可被：
- 医院 HIS 系统
- 电子病历 EHR 系统
- 健康管理平台
- 医生端 App

直接读取和解析。

LOINC 编码对照：
- 8867-4: 心率
- 8480-6: 收缩压
- 8462-4: 舒张压
- 2708-6: 血氧饱和度
- 8310-5: 体温
- 41901-0: 步数

## 安全建议

1. **HTTPS**：生产环境必须使用 HTTPS
2. **Token 认证**：使用 JWT 或 OAuth2.0
3. **数据加密**：体征数据传输时加密
4. **审计日志**：记录所有数据访问
5. **合规**：符合《个人信息保护法》和《健康医疗大数据管理暂行办法》
