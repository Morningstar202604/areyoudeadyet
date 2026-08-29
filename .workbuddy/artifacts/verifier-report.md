# 全量 Verifier 复查报告 · 「死了吗？」v0.5.0

- **复查范围**：engine / stats / ppg / ble / medical / ai / remote / ui / sos / hc / workout / store 全模块
- **方法**：主线程逐文件读码 + 对照 `test/` 三个算法测试套件（TestEngine / TestStats / TestFeatures）
- **标注约定**：`[事实]`=已读代码确认；`[推论]`=基于测试引用与可编译性推断；`[待核实]`=需真机/联网/外部资源验证
- **日期**：2026-08-27

---

## 一、核心结论

**之前"以为完成其实不够"的部分，主要集中在前端 UI 与 README 宣传，而非算法/医疗内核。**
内核（规则引擎、统计、PPG、BLE、FHIR）经逐行读码 + 测试对照，确为**真实、完整、高质量实现**；
AI 与远程监护是**可插拔架构占位（永远返回 Mock）**，README 将其描述为"已支持大模型/实时监护"属宣传夸大，但代码层是可接受的架构选择。
**真正欠账的是前端适老化专业度（约 55–60%）与若干运行期 bug、导航可达性、测试覆盖缺口。**

---

## 二、模块真实完成度

| 模块 | README 声称 | 代码事实 | 结论 | 证据 |
|------|------------|----------|------|------|
| engine/RiskEngine | 四级判定+组合规则+连续升级 | 完整实现：血压/心率/血氧/体温四级、3 条组合规则、MAP/SI/PP 衍生、z-score 基线、趋势回归、streak 升级、空数据告警、三段齐全 | **已完成·测试充分** `[事实]` | `RiskEngine.kt:83–571`；`TestEngine.java` 7 用例全中 |
| engine/Stats + VitalsMath | 统计原语 + MAP/SI/PP | mean/std/最小二乘/z-score/Haversine + MAP/PP/SI 公式完整 | **已完成** `[事实]` | `Stats.kt`；`TestStats.java` |
| engine/HealthReport(StressMath/weekly) | 压力指数 + 周报 | StressMath 对数线性映射、周报本周/上周对比 + 总结 | **已完成** `[事实]` | `TestFeatures.java` 3 用例；`HealthReport.kt:10` |
| ppg/PpgAnalyzer | 摄像头实测心率/HRV | 真实峰值检测算法；合成 60bpm 通过、短信号诚实拒绝 | **已完成** `[待核实·真机]` | `TestStats.java:72–92` |
| ble/BleCodec | 蓝牙协议解析 | HR u8/u16、IEEE-11073 SFLOAT、血压解析全部实现并通过 | **已完成** `[待核实·真机]` | `TestStats.java:94–111` |
| medical/FhirExporter | FHIR R4 导出 | 真实 Bundle JSON + LOINC 编码 + RFC3339 时间戳 + 文本摘要 | **已完成·缺自动测试** `[事实]` | `FhirExporter.kt:18–102` |
| medical/PdfReportGenerator | PDF 健康报告 | 真实调用生成（Explore 确认） | **已完成·缺自动测试** `[推论]` | `MedicalScreen.kt:134+` |
| ai/AiAnalyzerProvider | 支持通义/智谱/文心 | **永远返回 `MockAiAnalyzer()`**，与配置无关 | **架构占位·README 夸大** `[事实]` | `AiAnalyzerProvider.kt:12–17` |
| remote/RemoteSyncProvider | 家人实时监护/推送 | 三分支**全部返回 `MockRemoteSync()`** | **架构占位·README 夸大** `[事实]` | `RemoteSyncProvider.kt:11–15` |
| ui/* (10 屏) | 12 屏适老化 | 10 屏 UI 全部实装可交互；适老化专业度低、含运行期 bug | **部分·最大短板** `[事实]` | 见 Explore 调研报告 |
| sos / hc / Workout / store / work | 真实功能 | 代码层实装 | **已完成·缺自动测试** `[推论]` | 源码层确认 |

> 注：README 结构图列 10 屏、文案称 12 屏，自身数量矛盾；实际路由 10 条 `[事实]`。

---

## 三、测试覆盖缺口 `[待核实·需补]`

1. **无自动化的模块**：medical(Fhir/Pdf)、sos、hc(HealthConnect)、WorkoutService、store、work(Reminders)、ui（零 `@Preview`、零 Compose 测试）。
2. **算法测试形态**：`test/` 下三个文件是 JVM `main()` 断言脚本，**非 JUnit**；CI 是否真正执行并在失败时报红需查 `ci.yml` 确认 `[待核实]`。
3. **真机能力无法 CI 验证**：PPG 相机、BLE 设备、Health Connect、GPS 运动依赖真机与硬件，仅能人工回归。

---

## 四、前端现状（最大问题，详细证据见 Explore 报告）

- **对比度不达标**：白字叠柔色品牌，主色 2.3:1 / 次色 2.9:1 / 风险徽章 2.7:1，远低于 WCAG 3:1（大字号）/4.5:1（正文）。`Theme.kt:65,69,84`、`Widgets.kt:58–63`。
- **尺寸体系混乱**：按钮 40/60/64dp 混用，README 宣称的 76dp 从未实现；圆角 10–24dp 乱跳；全项目零 `@Preview`；无尺寸/间距/圆角令牌。
- **导航可达性缺口**：底部导航仅 4 tab + SOS，`Devices/Workout/Guardian/AiReport/Medical` 五屏只能经首页卡片进入，无独立入口/深链。
- **运行期 bug（非崩溃但需修）**：`DevicesScreen.kt:193` 读 `.value` 非响应式致计数永不过期；`:321` Magic String 比较扫描返回值（字符串一改即静默失效）；多处 `items()` 无 `key` 致错位；`AiReportScreen` 失败不自动重试；`PpgMeasureSection` 相机并发/退出空转。

---

## 五、架构占位说明（非 bug，需外部资源）

`AiAnalyzerProvider` 与 `RemoteSyncProvider` 永远返回 Mock，是"可插拔后端"定位的产物——App 不提供服务器，企业自行对接。
**风险点**：README 将"支持通义/智谱/文心""家人实时监护"写成已实现功能，与代码（默认恒为 Mock）不符，属对外宣传失真，应在文档层面修正或预留真实实现分支。

---

## 六、Top 风险排序与修复优先级

| 优先级 | 风险 | 性质 | 处置 |
|--------|------|------|------|
| P0 | 前端对比度/适老化不达标 | 用户主诉·合规/可用性质疑 | 彻底重设计（设计系统令牌化 + 达标对比度） |
| P0 | 前端运行期 bug（非响应式/Magic String/列表错位） | 功能正确性 | 重设计阶段一并修复 |
| P1 | 五屏导航可达性缺口 | 可用性 | 补入口（底部导航/抽屉） |
| P1 | README 宣传与代码不符 | 对外失真 | 文档层面标注 Mock/可插拔 |
| P2 | 测试覆盖缺口（medical/sos/真机） | 质量门禁 | 后续补 JVM/仪器测试 |
| P2 | Mock 后端未接线 | 架构·需外部资源 | 企业对接，非本次范围 |

---

## 七、给后续阶段（前端重设计）的约束

1. 重设计只改视觉层（Theme / Widgets / 各 Screen 样式与布局），**不删功能逻辑**。
2. 风险分级配色必须改用深饱和色，确保白字对比度 ≥ 3:1（大字号）/4.5:1（正文）。
3. 抽出 `AppShapes` / `AppSpacing` / 按钮高度常量，消除 Magic Number；主按钮统一 76dp。
4. 各主要屏幕补 `@Preview`；为五屏补稳定导航入口。
5. 修复第四节列出的运行期 bug。
6. 重设计后必须 `compileDebug` 通过并重新 `assembleRelease` 出包，闭环验证。
