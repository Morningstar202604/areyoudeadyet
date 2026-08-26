<div align="center">

<img src="docs/images/logo.png" width="120" alt="死了吗？logo"/>

# 死了吗？ · Are You Dead Yet?

**老人健康监测 · 生命风险预警 · 一键 SOS**
**Elderly Health Guardian with Pluggable Remote Backend on Android**

[![CI](https://github.com/Morningstar202604/areyoudeadyet/actions/workflows/ci.yml/badge.svg)](https://github.com/Morningstar202604/areyoudeadyet/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/Morningstar202604/areyoudeadyet?color=B71C1C&label=%E7%89%88%E6%9C%AC)](https://github.com/Morningstar202604/areyoudeadyet/releases)
[![Platform](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/github/license/Morningstar202604/areyoudeadyet?color=blue)](LICENSE)
[![Stars](https://img.shields.io/github/stars/Morningstar202604/areyoudeadyet?style=social)](https://github.com/Morningstar202604/areyoudeadyet/stargazers)

*拒绝「多喝水早睡觉」式空泛健康建议 —— 有危险就大声警告，并告诉你现在该做什么。*

**[下载最新 APK](https://github.com/Morningstar202604/areyoudeadyet/releases/latest)** · [English](#english) · [算法说明](#算法与模型) · [远程配置](docs/remote-setup.md) · [参与贡献](#参与贡献)

<img src="docs/images/banner.png" width="720" alt="死了吗？banner"/>

</div>

---

## 为什么做这个 App

市面上的老人健康 App 有个通病：**没有危机意识**。数据超标了只会说「注意休息、多喝水」，从不告诉你危险不危险、该干什么。这个 App 反过来：

> 每条预警强制回答三个问题：**是什么问题 / 为什么危险 / 现在就做什么。**
> 宁可多提醒，绝不装没事。

## 核心能力

| 能力 | 说明 |
|------|------|
| 🚨 **规则预警引擎** | 医学阈值四级判定（正常/注意/警告/危险），血压危象、休克代偿、缺氧+心动过速等**组合规则**单独识别——单看正常、合起来危险 |
| 📈 **统计模型层** | 个人基线 z-score 异常检测、最小二乘趋势回归、平均动脉压 MAP、休克指数 SI、脉压差 PP——公式全部公开 |
| 👨‍👩‍👧 **远程监护** | 家人实时查看老人健康数据，异常自动推送告警（需配置远程后端） |
| 🤖 **AI 健康分析** | 基于体征数据的智能分析报告、风险评分、个性化建议（支持通义/智谱/文心等国内大模型） |
| 🏥 **医疗对接** | FHIR R4 标准导出，兼容医院 HIS/EHR 系统，一键生成健康报告 |
| ❤️ **摄像头实测心率** | PPG 光电容积波：指尖+闪光灯 30 秒实测心率与 HRV(RMSSD)，信号质量不足时拒绝出数 |
| ⌚ **蓝牙设备直连** | 标准协议心率带 0x180D / 电子血压计 0x1810 / 血氧仪 0x1822，IEEE-11073 SFLOAT 解析 |
| 📲 **穿戴数据同步** | Health Connect 接入华为/小米等运动健康数据 |
| 🏃 **GPS 运动记录** | 步行/跑步实时轨迹、距离、配速、卡路里（前台服务 + 通知栏控制，离线渲染轨迹图） |
| 📊 **周健康报告** | 本周 vs 上周逐指标对比 + 一句话总结 + 一键分享给家人 |
| 😴 **睡眠记录** | 手动记录入睡/起床，自动算时长，进入周报统计 |
| 😮‍💨 **压力指数** | 基于 PPG 实测 HRV(RMSSD) 的对数线性估算（0-100） |
| ⏰ **智能提醒** | WorkManager 测量提醒（当天已测齐自动免打扰）+ 久坐提醒（9-21 点每小时） |
| 🆘 **一键 SOS** | 全屏呼救：拨打 120 / 家人电话 / 自动附体征摘要的求救短信 |
| 🗣️ **适老化设计** | 大字号高对比、76dp+ 大按钮、危险级别自动语音播报 |
| 🔒 **离线优先** | 离线完全可用，远程同步可选；数据隐私由用户掌控 |

## 可插拔架构

本 App 采用**可插拔后端架构**，适合企业级部署：

```
┌─────────────────────────────────────────────┐
│                  App 客户端                   │
├─────────────┬───────────────┬───────────────┤
│  离线模式   │  远程同步层    │  AI 分析层    │
│  (默认)     │  RemoteSync   │  AiAnalyzer   │
│             │  接口 (可插拔) │  接口 (可插拔) │
├─────────────┴───────────────┴───────────────┤
│              配置文件 remote_config.json       │
│  enabled | provider | baseUrl | aiProvider   │
└─────────────────────────────────────────────┘
```

- **我们不提供服务器**，只提供 App + 接口规范 + 配置文档
- 企业自行对接阿里云/腾讯云/自建后端
- 详见 [远程同步配置指南](docs/remote-setup.md)

## 算法与模型

```
规则层   医学阈值判定 + 组合规则 + 连续 3 次超标自动升级
统计层   z-score = (x-μ)/σ          个人基线异常（14 天窗口）
         最小二乘回归斜率             趋势预警（21 天日均值）
         MAP = DBP + (SBP-DBP)/3     <65 器官灌注不足
         SI  = HR / SBP              ≥1.0 显性休克证据
         PP  = SBP - DBP             ≥65 动脉硬化信号
信号层   PPG：滑动均值去趋势 → 自适应峰值检测(局部μ+0.6σ, 280ms不应期)
              → HR = 60000/median(IBI)，RMSSD = √mean(ΔIBI²)
协议层   IEEE-11073 16-bit SFLOAT：值 = m(12位补码) × 10^e(4位补码)
```

## 快速开始

```powershell
git clone https://github.com/Morningstar202604/areyoudeadyet.git
cd areyoudeadyet
# Android Studio 打开项目直接 Run，或：
.\gradlew.bat :app:assembleDebug
```

- 首次打开点「**加载 7 天演示数据**」体验全部功能（守护页可一键清空）
- 华为/小米手环：在运动健康 App 开启 Health Connect 同步后，到守护页拉取
- 远程同步：编辑 `app/src/main/assets/remote_config.json`，详见 [配置指南](docs/remote-setup.md)

## 项目结构

```
app/src/main/java/com/silema/app/
├── engine/     # RiskEngine 规则引擎 + Stats/VitalsMath 统计模型
├── ppg/        # PpgAnalyzer 信号处理 + camera2 采集
├── ble/        # BleVitals GATT 客户端 + BleCodec 协议解析
├── hc/         # Health Connect 同步
├── remote/     # RemoteSync 可插拔远程同步接口
├── ai/         # AiAnalyzer 可插拔 AI 分析接口
├── medical/    # FHIR R4 导出 + 健康报告生成
├── store/      # 离线仓储 + 演示数据
├── sos/        # 紧急呼救
└── ui/         # Compose 适老化界面（12 个屏幕）
    ├── DashboardScreen    # 首页：状态横幅+体征卡片+趋势图+快捷操作
    ├── EntryScreen        # 数据录入：类型选择+数值输入+历史记录
    ├── ReportScreen       # 健康报告：趋势图/周报/睡眠三个 Tab
    ├── FamilyScreen       # 远程监护：家人列表+体征状态
    ├── AiReportScreen     # AI 分析：风险评分+发现+建议
    ├── MedicalScreen      # 医疗对接：FHIR 导出+报告分享
    ├── DevicesScreen      # 设备中心：PPG/BLE/IoT 设备管理
    ├── WorkoutScreen      # 运动追踪：GPS 轨迹+实时数据
    ├── GuardianScreen     # 设置：远程同步/联系人/提醒/数据管理
    └── SosScreen          # 紧急呼救：120/家人/短信
```

## 参与贡献

欢迎 PR！特别是：
- 更多设备协议适配（血糖仪、体温计、体重秤）
- 跌倒检测算法
- 多语言支持
- 鸿蒙 ArkTS 版
- 各云厂商 RemoteSync 实现

小步骤：Fork → 分支 → 改动（跑通 `test/` 下算法测试）→ PR。

## 免责声明

本应用基于公开医学共识阈值做健康管理参考，**不能替代医生诊断和正规医疗设备**。紧急情况永远优先拨打 120。

---

<div id="english"></div>

## English

**Are You Dead Yet? (死了吗？)** is an elderly health guardian on Android with a **pluggable remote backend architecture**. Unlike wellness apps that only say "drink more water", every alert answers three questions: *what's wrong, why it's dangerous, and what to do right now*.

### Features

- **Risk engine** — medical-threshold 4-level triage, combo rules (e.g. low BP + racing heart = shock compensation), streak escalation
- **Statistics layer** — personal-baseline z-score anomaly detection, least-squares trend regression, MAP / shock index / pulse pressure
- **Remote monitoring** — family members can view elderly vitals in real-time; alerts pushed automatically (requires remote backend)
- **AI health analysis** — intelligent analysis reports, risk scoring, personalized recommendations (supports Qwen/Zhipu/Wenxin)
- **FHIR R4 export** — standard medical record format compatible with hospital HIS/EHR systems
- **Camera PPG** — real optical heart-rate & HRV (RMSSD) measurement via fingertip + flash, 30 s
- **Bluetooth LE** — standard-profile heart-rate belts (0x180D), BP monitors (0x1810), pulse oximeters (0x1822)
- **Health Connect** — syncs Huawei/Xiaomi wearable data
- **SOS** — full-screen emergency: call 120 / family / SMS with vitals summary
- **Elderly-first UI** — warm family style, huge fonts, high contrast, auto voice announcements
- **Offline-first** — fully functional offline; remote sync is optional and configurable

### Pluggable Architecture

We **do not provide servers**. The app ships with a `RemoteSync` interface and configuration system. Companies plug in their own backend (Alibaba Cloud, Tencent Cloud, or self-hosted). See [Remote Setup Guide](docs/remote-setup.md).

**Download:** [latest release APK](https://github.com/Morningstar202604/areyoudeadyet/releases/latest) · Android 8.0+ · MIT License

> Health-management reference only — not a medical device. In emergencies always call local emergency services.
