# 「死了吗？」v0.5.0 — 双端架构定稿（手机=守护端 / 手表=主产品）

> 本次会话在「端架构重大更正」基础上收尾：补齐手表端编译验证、把手机端重定位为纯守护端、双端签名出包、算法门禁复验。

## 端拓扑（用户第 12 条确认）

| 端 | 角色 | 目标用户 | 模块 | minSdk | 状态 |
|---|---|---|---|---|---|
| **手表端 (Wear OS)** | **主产品** | 老人家佩戴 | `:wear` | 30 | ✅ 5 屏全部真实调用 `:core` |
| **手机端 (Android)** | **纯守护端**（次端） | 监视老人家的人 | `:app` | 26 | ✅ 已去老人家自用屏，现代风 |

共享：`com.silema.app.core`（Kotlin-JVM 纯库）——`RiskEngine` / `Stats` / `HealthReport` / `FhirExporter` / 数据模型，手机与手表共用，规则引擎不重复实现。

## 手表端（主产品，老人家佩戴）5 屏
- **健康首页**：`RiskEngine.evaluate(records)` 真实风险评估 + 最近 5 条记录 + 空态「还没有测量数据」
- **体征录入**：`Picker` 选值 + 切换 心率/血压/血氧/体温/步数，存 `VitalRecord`
- **SOS**：120dp 红色按钮 → 本地「已通知家人」确认（真实送达家人需后续蓝牙 `DataClient` 同步）
- **运动**：今日步数 / 本周次数，`Picker` 选时长存 `Workout`（手表无 GPS，距离记 0，诚实）
- **AI 简报**：`HealthReport.weekly(...)` 文本 + 风险等级

## 手机端（纯守护端）导航
底栏 5 项（现代风，不老龄化）：**监护／设备／报告／家人／更多**
- 监护（首页看板）：真实监测数据（风险评估、体征卡、预警列表、心率趋势），去掉 SOS 自按压与录入/运动快捷操作
- 设备：`DevicesScreen` 连接手表（Health Connect）
- 报告：`ReportScreen` 健康报告
- 家人：`FamilyScreen` 远程监护（本地模式诚实空态）
- 更多：`MoreScreen` 聚合 AI 分析 / 医疗对接(FHIR R4 导出) / 设置
- 已删除老人家自用屏：`EntryScreen`/`WorkoutScreen`/`SosScreen`（手表端已有，消除死代码）

## 编译 / 门禁 / 出包
- `:wear:assembleDebug` / `:app:assembleDebug`：BUILD SUCCESSFUL
- `:wear:assembleRelease` + `:app:assembleRelease` + `runAlgoTests`：BUILD SUCCESSFUL
- 算法门禁 **34/34 PASS**（RiskEngine 6 + Features 10 + Stats 18）
- `dist/` 已刷新双端签名包（证书 `CN=Silema AreYouDead`）：
  - `silema-v0.5.0-release.apk`（手机守护端，12.9MB）
  - `silema-wear-v0.5.0-release.apk`（手表主产品，16.9MB）

## 诚实边界（仍非本地、需企业后端，README 已标注）
- 手表 SOS 真实送达家人：需后续蓝牙 `DataClient` 同步
- 远程实时监护（家人远程拉取/推送告警）：`FamilyScreen` 本地模式诚实空态，导出分享可用，实时监护需后端

## 未完成（按用户要求：先不提交）
- 未 git 提交 / 未推双远程 / 未发 release tag。
