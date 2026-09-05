[English](README.md) · [简体中文](README.zh-CN.md) · [日本語](README.ja-JP.md)

<div align="center">

<img src="docs/images/logo.png" width="120" alt="Silema Are You Dead Yet? Logo"/>

# Silema · Are You Dead Yet?

**Elderly Health Guardian · Life Risk Warning · One-Tap SOS**

[![CI](https://github.com/Morningstar202604/areyoudeadyet/actions/workflows/ci.yml/badge.svg)](https://github.com/Morningstar202604/areyoudeadyet/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/Morningstar202604/areyoudeadyet?color=B71C1C&label=Release)](https://github.com/Morningstar202604/areyoudeadyet/releases)
[![Platform](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Wear OS](https://img.shields.io/badge/Wear%20OS-4.0%2B-4285F4?logo=wearos&logoColor=white)](https://developer.android.com/wear)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/github/license/Morningstar202604/areyoudeadyet?color=blue)](LICENSE)
[![Stars](https://img.shields.io/github/stars/Morningstar202604/areyoudeadyet?style=social)](https://github.com/Morningstar202604/areyoudeadyet/stargazers)

*No more vague "drink water, sleep early" advice — we warn you when it's dangerous and tell you exactly what to do.*

**[Download Latest APK](https://github.com/Morningstar202604/areyoudeadyet/releases/latest)** · [Algorithm Details](#algorithms--models) · [Remote Setup](docs/remote-setup.md) · [Contribute](#contributing)

<img src="docs/images/banner.png" width="720" alt="Silema Banner"/>

</div>

---

## Why This App Exists

Most elderly health apps suffer from the same flaw: **no sense of crisis**. When vitals go out of range, they just say "rest more, drink water" — never telling you *how dangerous it is* or *what to do right now*. We flip that:

> Every alert must answer three questions: **What's wrong? Why is it dangerous? What to do NOW?**  
> Better over-warn than pretend everything's fine.

---

## Core Features

| Feature | Description |
|---------|-------------|
| 🚨 **Risk Engine** | Medical-threshold 4-level triage (Normal/Attention/Warning/Danger). Combo rules detect hidden dangers: normal individually but dangerous together (e.g., low BP + racing heart = shock compensation). |
| 📈 **Statistical Models** | Personal-baseline z-score anomaly detection, least-squares trend regression, MAP, Shock Index (SI), Pulse Pressure (PP) — all formulas public. |
| 👨‍👩‍👧 **Local Data Export** | Export vitals as FHIR R4 health-record files to share with family/doctors (via LocalExportSync). Remote real-time monitoring requires enterprise backend deployment. |
| 🤖 **On-Device AI Analysis** | Runs fully offline using RiskEngine rule-based inference (risk score / findings / recommendations). No cloud model needed (implemented by LocalAiAnalyzer). |
| 🏥 **Medical Integration** | FHIR R4 standard export compatible with hospital HIS/EHR systems. One-tap health report generation. |
| ❤️ **Camera PPG** | Real optical heart-rate & HRV (RMSSD) measurement via fingertip + flash for 30 seconds. Rejects poor signal quality. |
| ⌚ **Bluetooth LE** | Standard-profile devices: heart-rate belts (0x180D), BP monitors (0x1810), pulse oximeters (0x1822). IEEE-11073 SFLOAT parsing. |
| 📲 **Health Connect Sync** | Integrates Huawei/Xiaomi wearable data via Health Connect API. |
| 🏃 **GPS Activity Tracking** | Real-time walking/running trajectory, distance, pace, calories (foreground service + notification control, offline trajectory rendering). |
| 📊 **Weekly Health Report** | This week vs last week comparison per metric + one-sentence summary + one-tap share to family. |
| 😴 **Sleep Tracking** | Manual sleep/wake recording with automatic duration calculation, included in weekly stats. |
| 😮‍💨 **Stress Index** | Log-linear estimation based on PPG-measured HRV (RMSSD), scale 0–100. |
| ⏰ **Smart Reminders** | WorkManager measurement reminders (auto-mute when daily measurements complete) + sedentary alerts (hourly 9AM–9PM). |
| 🆘 **One-Tap SOS** | Full-screen emergency call: dial 120 / family contacts / send SMS with vitals summary. |
| 🗣️ **Elderly-First UI** | Large fonts, high contrast, 76dp+ big buttons, auto voice announcements for danger levels. |
| 🔒 **Offline-First** | Fully functional offline; remote sync is optional. User controls data privacy. |

### Dual-End Architecture

| End | Role | Target User | Module | minSdk | Status |
|-----|------|-------------|--------|--------|--------|
| **Wear OS Watch** | **Primary Product** | Elderly wearer | `:wear` | 30 | ✅ 5 screens calling `:core` |
| **Android Phone** | **Guardian Only** | Family caregivers | `:app` | 26 | ✅ Modern design, no self-use screens |

Shared: `com.silema.app.core` (Kotlin-JVM pure library) — `RiskEngine` / `Stats` / `HealthReport` / `FhirExporter` / data models, shared between phone and watch, no duplicate rule engine.

---

## Pluggable Backend Architecture

Designed for enterprise deployment:

```
┌─────────────────────────────────────────────┐
│              App Client                      │
├─────────────┬───────────────┬───────────────┤
│ Offline Mode│ Remote Sync   │ AI Analysis   │
│ (Default)   │ Layer         │ Layer         │
│             │ RemoteSync    │ AiAnalyzer    │
│             │ Interface     │ Interface     │
│             │ (Pluggable)   │ (Pluggable)   │
├─────────────┴───────────────┴───────────────┤
│        Configuration: remote_config.json     │
│  enabled | provider | baseUrl | aiProvider   │
└─────────────────────────────────────────────┘
```

- **We do NOT provide servers.** We only provide the App + interface specs + configuration docs.
- Enterprises integrate their own backend (Alibaba Cloud, Tencent Cloud, or self-hosted).
- See [Remote Sync Setup Guide](docs/remote-setup.md).

---

## Algorithms & Models

```
Rule Layer    Medical threshold rules + combo rules + auto-escalation after 3 consecutive breaches
Stat Layer    z-score = (x-μ)/σ          Personal baseline anomaly (14-day window)
              Least-squares regression slope   Trend warning (21-day daily average)
              MAP = DBP + (SBP-DBP)/3     <65 organ hypoperfusion
              SI  = HR / SBP              ≥1.0 overt shock evidence
              PP  = SBP - DBP             ≥65 arteriosclerosis signal
Signal Layer  PPG: sliding mean detrend → adaptive peak detection (local μ+0.6σ, 280ms refractory)
                  → HR = 60000/median(IBI), RMSSD = √mean(ΔIBI²)
Protocol      IEEE-11073 16-bit SFLOAT: value = m(12-bit two's complement) × 10^e(4-bit two's complement)
```

---

## Quick Start

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK Platform 34 (Android 14)

### Build & Run

```bash
git clone https://github.com/Morningstar202604/areyoudeadyet.git
cd areyoudeadyet

# Open in Android Studio and click Run, or build from command line:
./gradlew :app:assembleDebug    # Phone guardian app
./gradlew :wear:assembleDebug   # Wear OS main product
```

### First-Time Setup
1. Tap **"Load 7-Day Demo Data"** on the Guardian screen to experience all features (can be cleared with one tap).
2. For Huawei/Xiaomi bands: Enable Health Connect sync in their Sports Health app, then pull data on the Guardian screen.
3. For remote sync: Edit `app/src/main/assets/remote_config.json`, see [Setup Guide](docs/remote-setup.md).

---

## Project Structure

```
├── app/                    # Phone guardian app (Android 8.0+)
│   └── src/main/java/com/silema/app/
│       ├── engine/         # RiskEngine rule engine + Stats/VitalsMath statistical models
│       ├── ppg/            # PpgAnalyzer signal processing + camera2 acquisition
│       ├── ble/            # BleVitals GATT client + BleCodec protocol parsing
│       ├── hc/             # Health Connect synchronization
│       ├── remote/         # RemoteSync pluggable remote sync interface
│       ├── ai/             # AiAnalyzer pluggable AI analysis interface
│       ├── medical/        # FHIR R4 export + health report generation
│       ├── store/          # Offline storage + demo data
│       ├── sos/            # Emergency SOS
│       └── ui/             # Jetpack Compose elderly-friendly UI (11 screens)
│           ├── DashboardScreen    # Home: status banner + vital cards + trends + quick actions
│           ├── EntryScreen        # Data entry: type selection + value input + history
│           ├── ReportScreen       # Health report: trends/weekly/sleep tabs
│           ├── FamilyScreen       # Remote monitoring: family list + vital status
│           ├── AiReportScreen     # AI analysis: risk score + findings + recommendations
│           ├── MedicalScreen      # Medical integration: FHIR export + report sharing
│           ├── DevicesScreen      # Device center: PPG/BLE/IoT device management
│           ├── WorkoutScreen      # Activity tracking: GPS trajectory + real-time data
│           ├── GuardianScreen     # Settings: remote sync/contacts/reminders/data management
│           ├── MoreScreen         # Function hub: unified entry for detection/exercise/AI/medical/settings
│           └── SosScreen          # Emergency SOS: call 120/family/SMS
│
├── wear/                   # Wear OS main product (Wear OS 4.0+)
│   └── src/main/java/com/silema/app/wear/
│       ├── ui/
│       │   ├── HomeScreen         # Health home: real assessment + recent records
│       │   ├── EntryScreen        # Vital entry: picker-based value selection
│       │   ├── SosScreen          # SOS emergency: 120dp red button
│       │   ├── WorkoutScreen      # Activity: steps + weekly count + duration picker
│       │   └── AiBriefScreen      # AI brief: weekly report + risk level
│       └── ...
│
├── core/                   # Shared Kotlin-JVM pure library
│   └── src/main/java/com/silema/app/
│       ├── engine/         # RiskEngine, Stats, VitalsMath, HealthReport
│       ├── data/           # Data models: VitalRecord, VitalType, Workout, etc.
│       └── ...
│
└── test/                   # Algorithm tests (34/34 passing)
```

---

## Contributing

PRs welcome! Especially looking for:
- More device protocol support (glucose meters, thermometers, smart scales)
- Fall detection algorithms
- Additional language support (Korean, Spanish, French, etc.)
- HarmonyOS ArkTS version
- Cloud provider RemoteSync implementations (AWS, Azure, GCP)

Small steps: Fork → Branch → Changes (pass `test/` algorithm tests) → PR.

---

## Disclaimer

This app provides health-management reference based on publicly available medical consensus thresholds. **It cannot replace professional medical diagnosis or certified medical devices.** In emergencies, always prioritize calling local emergency services (120 in China, 911 in US, 119 in Japan, etc.).

---

<div align="center">

**Made with ❤️ for elderly care** · [GitHub](https://github.com/Morningstar202604/areyoudeadyet) · [Issues](https://github.com/Morningstar202604/areyoudeadyet/issues) · [Releases](https://github.com/Morningstar202604/areyoudeadyet/releases)

**Keywords:** elderly health monitor, senior care app, wearable health tracker, blood pressure warning, heart rate monitoring, SOS emergency button, FHIR R4 export, Bluetooth medical devices, PPG heart rate, Health Connect sync, offline health app, pluggable backend, Android Wear OS, Jetpack Compose, Kotlin multiplatform, medical alert system, caregiver dashboard, vital signs tracker, health risk assessment, senior safety app, Chinese health app, 老人健康监测, 血压预警, 心率监测, SOS求救, FHIR导出, 蓝牙医疗设备, 可穿戴健康, 离线健康应用

</div>