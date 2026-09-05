[English](README.md) · [简体中文](README.zh-CN.md) · [日本語](README.ja-JP.md)

<div align="center">

<img src="docs/images/logo.png" width="120" alt="Silema Are You Dead Yet? Logo"/>

# Silema · Are You Dead Yet?

**老人健康监测 · 生命风险预警 · 一键 SOS**

[![CI](https://github.com/Morningstar202604/areyoudeadyet/actions/workflows/ci.yml/badge.svg)](https://github.com/Morningstar202604/areyoudeadyet/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/Morningstar202604/areyoudeadyet?color=B71C1C&label=Release)](https://github.com/Morningstar202604/areyoudeadyet/releases)
[![License](https://img.shields.io/github/license/Morningstar202604/areyoudeadyet?color=blue)](LICENSE)

*拒绝「多喝水早睡觉」式空泛建议 —— 有危险就大声警告，并告诉你现在该做什么。*

**[下载最新 APK](https://github.com/Morningstar202604/areyoudeadyet/releases/latest)** · [远程同步配置](docs/remote-setup.md) · [参与贡献](#参与贡献)

</div>

---

## 为什么做这个 App

市面上的老人健康 App 有个通病：**没有危机意识**。数据超标了只会说「注意休息、多喝水」，从不告诉你危险不危险、该干什么。这个 App 反过来：

> 每条预警强制回答三个问题：**是什么问题 / 为什么危险 / 现在就做什么。**  
> 宁可多提醒，绝不装没事。

---

## 核心能力

| 功能 | 说明 |
|---------|-------------|
| 🚨 **风险引擎** | 医学阈值 4 级分诊（正常/注意/警告/危险）。组合规则发现隐藏危险：单看都正常、合起来却危险（如低血压＋心率过快＝休克代偿）。 |
| 📈 **统计模型** | 个人基线 z-score 异常检测、最小二乘趋势回归、MAP、休克指数（SI）、脉压（PP），公式全部公开。 |
| 👨‍👩‍👧 **本地数据导出** | 把健康数据导出为 FHIR R4 电子档案文件，分享给家人/医生（LocalExportSync）。远程实时监控需企业后端部署。 |
| 🤖 **端侧 AI 分析** | 基于 RiskEngine 规则的完全离线推理（风险评分/发现/建议），不依赖云端模型（LocalAiAnalyzer 实现）。 |
| 🏥 **医疗集成** | FHIR R4 标准导出，兼容医院 HIS/EHR 系统，一键生成健康报告。 |
| ❤️ **相机 PPG** | 指腹＋闪光灯光学实测心率与 HRV（RMSSD）30 秒，信号质量差时不出数值。 |
| ⌚ **蓝牙低功耗** | 标准协议设备：心率带（0x180D）、血压计（0x1810）、血氧仪（0x1822），IEEE-11073 SFLOAT 解析。 |
| 📲 **Health Connect 同步** | 通过 Health Connect 集成华为/小米手环数据。 |
| 🏃 **GPS 运动记录** | 实时步行/跑步轨迹、里程、配速、卡路里（前台服务＋通知控制、离线轨迹渲染）。 |
| 📊 **周健康报告** | 每周与上周逐项对比＋一句话总结＋一键分享家人。 |
| 😴 **睡眠记录** | 手动记录入睡/醒来，自动计算时长，纳入周统计。 |
| 😮‍💨 **压力指数** | 基于 PPG 测得的 HRV（RMSSD）对数线性估算，0–100 分。 |
| ⏰ **智能提醒** | WorkManager 测量提醒（当天测完自动静音）＋久坐提醒（每天 9:00–21:00 整点）。 |
| 🆘 **一键 SOS** | 全屏紧急呼救：拨号 120 / 家属 / 发送含健康数据摘要的短信。 |
| 🗣️ **适老化 UI** | 大字体、高对比度、76dp+ 大按钮，危险等级自动语音播报。 |
| 🔒 **离线优先** | 离线完全可用，远程同步可选，数据隐私由用户掌控。 |

### 双端架构

| 端 | 角色 | 目标用户 | 模块 | minSdk | 状态 |
|-----|------|-------------|--------|--------|--------|
| **Wear OS 手表** | **主产品** | 老年佩戴者 | `:wear` | 30 | ✅ 5 屏调用 `:core` |
| **Android 手机** | **仅守护端** | 家属照护者 | `:app` | 26 | ✅ 全新设计，无自用屏 |

共享：`com.silema.app.core`（Kotlin-JVM 纯库）—— `RiskEngine` / `Stats` / `HealthReport` / `FhirExporter` / 数据模型，双端共用，规则引擎不重复实现。

---

## 可插拔后端架构

面向企业部署：

- **我们不提供服务器**，只提供 App + 接口规范 + 配置文档。
- 企业自行对接后端（阿里云、腾讯云或自建）。
- 详见[远程同步配置指南](docs/remote-setup.md)。

---

## 算法与模型

```
规则层   医学阈值判定 + 组合规则 + 连续 3 次超标自动升级
统计层   z-score = (x-μ)/σ          个人基线异常检测（14 天窗口）
         最小二乘回归斜率            趋势预警（21 天日均）
         MAP = DBP + (SBP-DBP)/3    <65 器官灌注不足
         SI  = HR / SBP             ≥1.0 显性休克证据
         PP  = SBP - DBP            ≥65 动脉硬化信号
信号层   PPG：滑动均值去趋势 → 自适应波峰检测（局部 μ+0.6σ，280ms 不应期）
         → HR = 60000/median(IBI)，RMSSD = √mean(ΔIBI²)
协议     IEEE-11073 16 位 SFLOAT：value = m(12 位补码) × 10^e(4 位补码)
```

---

## 快速开始

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更新
- JDK 17
- Android SDK Platform 34（Android 14）

### 构建与运行

```bash
git clone https://github.com/Morningstar202604/areyoudeadyet.git
cd areyoudeadyet

# 用 Android Studio 打开点击运行，或命令行构建：
./gradlew :app:assembleDebug    # 手机守护端
./gradlew :wear:assembleDebug   # Wear OS 主产品
```

### 首次使用
1. 在守护页点「**加载 7 天演示数据**」即可体验全部功能（可一键清空）。
2. 华为/小米手环：在运动健康 App 开启 Health Connect 同步后，到守护页拉取数据。
3. 远程同步：编辑 `app/src/main/assets/remote_config.json`，详见[配置指南](docs/remote-setup.md)。

---

## 项目结构

```
├── app/                    # 手机守护端（Android 8.0+）
│   └── src/main/java/com/silema/app/
│       ├── engine/         # RiskEngine 规则引擎 + Stats/VitalsMath 统计模型
│       ├── ppg/            # PpgAnalyzer 信号处理 + camera2 采集
│       ├── ble/            # BleVitals GATT 客户端 + BleCodec 协议解析
│       ├── hc/             # Health Connect 同步
│       ├── remote/         # RemoteSync 可插拔远程同步接口
│       ├── ai/             # AiAnalyzer 可插拔 AI 分析接口
│       ├── medical/        # FHIR R4 导出 + 健康报告生成
│       ├── store/          # 离线存储 + 演示数据
│       ├── sos/            # 紧急 SOS
│       └── ui/             # Jetpack Compose 适老化 UI（11 屏）
│           ├── DashboardScreen    # 首页：状态横幅 + 指标卡片 + 趋势 + 快捷操作
│           ├── EntryScreen        # 测量入口：类型选择 + 数值输入 + 历史
│           ├── ReportScreen       # 健康报告：趋势/周报/睡眠标签页
│           ├── FamilyScreen       # 家人：家人列表 + 健康状态
│           ├── AiReportScreen     # AI 分析：风险评分 + 发现 + 建议
│           ├── MedicalScreen      # 医疗：FHIR 导出 + 报告分享
│           ├── DevicesScreen      # 设备中心：PPG/BLE/IoT 设备管理
│           ├── WorkoutScreen      # 运动：GPS 轨迹 + 实时数据
│           ├── GuardianScreen     # 守护：远程同步/联系人/提醒/数据管理
│           ├── MoreScreen         # 更多：检测/运动/AI/医疗/设置统一入口
│           └── SosScreen          # 紧急 SOS：呼救 120/家属/短信
│
├── wear/                   # Wear OS 主产品（Wear OS 4.0+）
│   └── src/main/java/com/silema/app/wear/
│       ├── ui/
│       │   ├── HomeScreen         # 健康首页：实时评估 + 最近记录
│       │   ├── EntryScreen        # 测量记录：选择器式数值录入
│       │   ├── SosScreen          # SOS 紧急呼救：120dp 红色按钮
│       │   ├── WorkoutScreen      # 运动：步数 + 周统计 + 时长选择
│       │   └── AiBriefScreen      # AI 简报：周报告 + 风险等级
│       └── ...
│
├── core/                   # 共享 Kotlin-JVM 纯库
│   └── src/main/java/com/silema/app/
│       ├── engine/         # RiskEngine, Stats, VitalsMath, HealthReport
│       ├── data/           # 数据模型：VitalRecord, VitalType, Workout 等
│       └── ...
│
└── test/                   # 算法测试（34/34 通过）
```

---

## UI 设计系统 v3

### 设计理念
参考 Keep 等运动应用和现代养生应用，采用**清新健康活力风**设计语言，兼顾美观与适老化。

### 配色系统
| 角色 | 颜色 | 用途 |
|------|------|------|
| **主品牌色** | 薄荷绿 `#00A86B` | 健康/自然/活力，主按钮、选中态 |
| **强调色** | 活力橙 `#FF6D00` | 温暖/能量/CTA，重要操作、提醒 |
| **辅助色** | 深邃蓝 `#1A237E` | 专业/信任/医疗，次要信息、设备 |
| **危险色** | 珊瑚红 `#E53935` | SOS、风险警告、错误 |
| **背景** | 浅薄荷绿渐变 `#F1F8E9 → #E8F5E9` | 页面背景，清新自然 |

### 数据可视化色板
- 心率：粉红 `#E91E63`　血压：靛蓝 `#3F51B5`　血氧：青色 `#00BCD4`
- 体温：橙红 `#FF5722`　步数：浅绿 `#8BC34A`　睡眠：蓝紫 `#7986CB`

### 适老化规范
- 正文字号 16-17sp，标题 20-30sp
- 对比度 ≥ 4.5:1（WCAG AA）
- 点击区域 ≥ 48dp
- 大按钮、大图标、清晰视觉层次

---

## 功能模块

### 手机端（守护端）
| 模块 | 功能 |
|------|------|
| **Dashboard 首页** | 健康概览、风险评估、核心数据、快速操作、SOS |
| **Devices 设备** | PPG相机测量、蓝牙设备连接、Health Connect同步 |
| **Report 报告** | 健康评分、指标分析、导出PDF/JSON、AI分析 |
| **Guardian 守护** | SOS设置、守护功能开关、紧急联系人、通知方式 |
| **Family 家人** | 快捷联系、家人列表、家庭健康共享 |
| **Medical 医疗** | 用药提醒、就诊记录、医疗档案、健康知识 |
| **More 更多** | 数据管理、个性化设置、安全隐私、关于 |
| **AiReport AI分析** | AI智能分析、健康建议、趋势预测 |
| **Entry 测量入口** | 心率/血压/血氧/体温测量、数据来源选择 |

### 手表端（Wear OS）
| 模块 | 功能 |
|------|------|
| **WearHome 首页** | 心率大数字、风险等级、血氧/步数、SOS按钮 |
| **WearSos 紧急呼救** | 倒计时防误触、一键呼救、发送位置 |
| **WearWorkout 运动** | 步数/心率/卡路里/时长、开始/暂停 |
| **WearSettings 设置** | 通知、深色模式、健康追踪、关于 |

### 华为手表适配
- 支持圆形屏幕（Watch GT 系列）和方形屏幕（Watch Fit 系列）
- 华为 BLE 设备过滤（血压表、手环、体脂秤）
- Wear OS 版本可在华为 Wear OS 手表上运行
- HarmonyOS 版本需单独 ArkTS 项目开发

---

## 参与贡献

欢迎 PR！特别是：
- 更多设备协议适配（血糖仪、体温计、体重秤）
- 跌倒检测算法
- 更多语言支持（韩语、西班牙语、法语等）
- 鸿蒙 ArkTS 版本
- 各云厂商 RemoteSync 实现（AWS、Azure、GCP）

小步骤：Fork → 分支 → 改动（跑通 `test/` 下算法测试）→ PR。

---

## 免责声明

本应用基于公开医学共识阈值做健康管理参考，**不能替代医生诊断和正规医疗设备**。紧急情况永远优先拨打当地急救电话（中国 120，美国 911，日本 119 等）。

---

<div align="center">

**为老人健康而作** · [GitHub](https://github.com/Morningstar202604/areyoudeadyet) · [Issues](https://github.com/Morningstar202604/areyoudeadyet/issues) · [Releases](https://github.com/Morningstar202604/areyoudeadyet/releases)

**关键词：** 老人健康监测、血压预警、心率监测、SOS求救、FHIR导出、蓝牙医疗设备、可穿戴健康、离线健康应用

</div>