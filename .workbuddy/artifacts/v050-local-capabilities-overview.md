# v0.5.0 真实本地能力补齐（消除 Mock 占位）

## 本轮做了什么

按主人「先不用提交，先继续推进」的授权，选定方向＝补齐真实本地能力、消除 Mock 占位。
两个 Mock 占位已替换为**真实在端/本地**实现，零新增第三方依赖、零构建回归。

### 1. AI 健康分析 → 真实在端推理
- 新建 `app/src/main/java/com/silema/app/ai/LocalAiAnalyzer.kt`
  - 完全本地运行，调用已有的真实 `RiskEngine.evaluate()`，把 `Assessment` 转译为 AI 报告所需的 `HealthInsight`（综合风险评分 / 检查发现 / 健康建议）。
  - 风险评分映射：`NORMAL→0.1 / WATCH→0.35 / WARNING→0.6 / CRITICAL→0.85`，驱动报告环形仪表着色。
  - 有告警的指标列出发现；核心指标（心率/血氧/血压/体温）无告警时补「正常」发现，避免报告自相矛盾。
- `AiAnalyzerProvider` 现固定返回 `LocalAiAnalyzer`（无需云端模型/后端配置）。
- 删除 `MockAiAnalyzer.kt`（原永远返回假数据）。

### 2. 远程同步 → 真实本地 FHIR 导出
- 新建 `app/src/main/java/com/silema/app/remote/LocalExportSync.kt`
  - 复用真实 `FhirExporter`，把体征序列化为 FHIR R4 Bundle 落盘到应用私有文档目录（`fhir_export/`），返回实际导出条数——**真实文件写入，非假成功**。
  - `getFamilyMembers()` 返回空列表（诚实，不伪造云端家人）；`isAvailable()` 恒真。
- `RemoteSyncProvider` 现返回 `LocalExportSync`。
- 删除 `MockRemoteSync.kt`（原返回假 success/stub）。
- `FamilyScreen` 空态文案改为本地模式诚实说明：可在「医疗对接」页导出 FHIR 健康档案分享给家人；远程实时监护需企业部署后端，未含于本地构建。

### 3. 持久化决策（明确）
- `AppRepository` 已是 **JSON 离线优先持久化**（真实本地能力，非 Mock）。
- 主人确认：**保留 JSON，不引入 Room**。理由——构建环境脆弱（Kotlin 2.0.21 + Gradle 8.10），Room+KSP 有把刚跑绿的 release 再次弄挂的风险；JSON 已满足「真实本地持久化」目标。Room 列为后续可选优化。

### 4. 文档对齐
- `README.md` 能力表 + 英文 Features：AI 分析改为「完全在端、基于 RiskEngine、无需云端模型」；远程监护改为「本地 FHIR 导出/分享（真实），远程实时监护需后端」。

## 验证结果
| 项目 | 结果 |
|------|------|
| `compileDebugKotlin` | ✅ EXIT=0 |
| `runAlgoTests`（算法门禁） | ✅ 34/34 PASS |
| `assembleRelease` | ✅ BUILD SUCCESSFUL |
| `dist/silema-v0.5.0-release.apk` | ✅ 12.9 MB，apksigner 校验 SIGNED OK（CN=Silema AreYouDead） |

## 边界（诚实提示）
- **仍非本地、需企业后端**：`RemoteSync` 接口的云端实时监护（家人远程拉取数据 / 推送告警）。本地构建仅提供 FHIR 导出分享，README 已标注。
- **尚未 git 提交**（按主人要求「先不用提交」）。发布动作（推 GitCode + GitHub 双远程、打 tag、附 release 说明）待授权。

## 改动文件清单
- 新增：`ai/LocalAiAnalyzer.kt`、`remote/LocalExportSync.kt`
- 删除：`ai/MockAiAnalyzer.kt`、`remote/MockRemoteSync.kt`
- 修改：`ai/AiAnalyzerProvider.kt`、`remote/RemoteSyncProvider.kt`、`ui/FamilyScreen.kt`、`README.md`
