# 项目长期记忆：死了吗？ (Are You Dead Yet?) 安卓 App

## 定位
老人健康监测 App，Kotlin 2.0.21 + Jetpack Compose Material3，compileSdk 35 / minSdk 26 / targetSdk 34。
当前版本 v0.5.0（versionCode 5）。Release 签名 keystore：`D:\keys\silema.keystore`（CN=Silema AreYouDead）。
SDK：`D:\android-env\android-sdk`；构建用 JDK 21（Microsoft build）。Gradle 8.10.2，腾讯镜像。

## 前端状态（2026-08-27 重设计完成）
- 设计系统 v2「温暖医疗专业风」已落地：Theme.kt / Widgets.kt 令牌（AppShapes / AppSpacing / AppSize），深饱和、白字对比度达标色板。
- 导航：AppNav 底栏 5 项 + SOS 常驻红色 FAB（FabPosition.End）；MoreScreen 功能中心收容 检测/运动/AI/医疗/设置。
- 全屏形状令牌化 + `items` key 补全；`Previews.kt` 含 10 屏 `@Preview`。

## 后端真实能力（2026-08-27 消除 Mock 占位）
- `AiAnalyzerProvider` 现返回 `LocalAiAnalyzer`（真实在端，基于 RiskEngine 规则引擎，无需云端模型）；原 `MockAiAnalyzer` 已删除。
- `RemoteSyncProvider` 现返回 `LocalExportSync`（真实本地 FHIR R4 落盘导出，复用 `FhirExporter`）；原 `MockRemoteSync` 已删除。FamilyScreen 在本地模式下显示诚实空态（导出分享可用，远程实时监护需后端）。
- 仍非本地、需企业后端的：`RemoteSync` 的云端实时监护（家人远程拉取/推送告警）—— 本地构建不含，README 已诚实标注。
- 本地持久化：`AppRepository` 为 JSON 离线优先，已是真实本地能力；**刻意未引入 Room**（构建环境脆弱 Kotlin 2.0.21 + Gradle 8.10，Room+KSP 有回归风险；JSON 已满足目标）。

## 真实完整且测试充分的模块
`FhirExporter`(FHIR R4)、`RiskEngine`(四级阈值/组合规则/趋势)、`Stats`(MAP/SI/PP 等)；`test/` 下 3 个 Java 用例类经 `runAlgoTests` 真实运行（34 项 PASS）。

## 构建命令
`./gradlew :app:assembleRelease`（需 JAVA_HOME 指向 JDK 21，sdk.dir 在 local.properties）。产物 `app/build/outputs/apk/release/app-release.apk`，复制到 `dist/silema-vX.Y.Z-release.apk`。

## 端架构与模块划分（2026-08-27 更正）
- **端分法（用户最终确认）**：主产品＝**手表端 Wear OS**（给老人家佩戴）；次端＝**手机端 Android**（给监视老人家的人/守护，现代正常风，非老龄化）。此前误把两端都当手机。
- 三层模块：`app`(手机守护端) / `core`(纯 Kotlin-JVM 共享领域层：RiskEngine/Stats/HealthReport/FhirExporter/数据模型) / `wear`(手表端，从零建，待编译验证)。
- **`:core` 必须是 Kotlin-JVM 纯库**，不能 android-library（后者 debugRuntimeElements 暴露大量 android-* 变体，被 `:app:debugRuntimeClasspath`/JavaExec 解析时变体歧义、整构建 FAIL）。这是本项目的硬约束。
- 算法门禁 `runAlgoTests` 始终由 `:app` 跑（TestStats 依赖 `:app` 的 ppg/ble 代码）；`:core` 类经 `:app` 的 `debugRuntimeClasspath` 进入 JavaExec classpath。
- 用户确认：手机端改为纯守护端（去老人家自用屏）；手表端从零建 5 屏（健康首页/评估、SOS、运动、体征录入、AI 简报）。
