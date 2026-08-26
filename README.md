# 死了吗？—— 健康监测 · 生命管理

给老人的健康监测 App。与市面上「多喝水早睡觉」式空泛健康应用的区别：
**只做分级、具体、可执行的风险预警** —— 每条告警都回答三件事：

1. **是什么问题**（实测数值对比医学阈值）
2. **为什么危险**（不吓唬也不粉饰）
3. **现在就做什么**（拨120 / 复测 / 当天就医，明确到动作）

## 当前版本：Android v0.3.0

| 能力 | 说明 |
|------|------|
| 规则预警引擎 | 医学阈值四级判定 + 组合规则 + 连续超标升级，阈值全部公开 |
| **统计模型层** | 个人基线 z-score 异常检测（14天窗口）、最小二乘回归趋势分析（21天日均值）、平均动脉压 MAP=舒张压+(收缩压-舒张压)/3、休克指数 SI=心率/收缩压、脉压差 PP |
| **摄像头 PPG 实测** | camera2 采集脉搏波（指尖+闪光灯），去趋势滤波→自适应峰值检测→IBI 中值→心率 + HRV(RMSSD)，置信度不足时拒绝出数 |
| **蓝牙 BLE 直连** | 标准协议设备：心率带 0x180D / 血压计 0x1810 / 血氧仪 0x1822，IEEE-11073 SFLOAT 解析，实时入库 |
| Health Connect | 华为/小米等穿戴设备数据同步（私有协议设备的官方接入路径） |
| SOS 呼救 | 拨打120 / 家人电话 / 自动附体征摘要的求救短信 |
| 适老化 | 大字号高对比、76dp+ 按钮、危险自动语音播报 |

## 联网说明：完全离线运行

APK 清单中**没有申请 INTERNET 权限**（可用 aapt2 dump badging 验证），
所有算法、存储、设备连接全部在本机完成，不需要任何服务器。
将来做「家属远程查看」才需要引入后端。

## 构建

环境要求：JDK 17 + Android SDK（platform android-35、build-tools 34+）。

```powershell
$env:JAVA_HOME = "D:\android-env\jdk-17.0.20+8"
$env:ANDROID_HOME = "D:\android-env\android-sdk"
.\gradlew.bat :app:assembleDebug
# 产物：app\build\outputs\apk\debug\app-debug.apk
```

首次使用：首页点「加载7天演示数据」体验 → 守护页可一键清空。

## 算法验证

`test/TestEngine.java` 与 `test/TestStats.java` 共 23 个用例
（数学原语、MAP/SI 公式、z-score、趋势回归、PPG 合成信号60bpm、SFLOAT 协议向量），当前全部通过：

```powershell
java -cp "app\build\tmp\kotlin-classes\debug;<kotlin-stdlib>;<serialization>;<coroutines>" test\TestStats.java
```

## 技术栈

Kotlin 2.0.21 · Jetpack Compose (BOM 2024.09.03) · Material 3 · Navigation Compose ·
Health Connect 1.1.0-alpha07 · Android camera2（框架内置，无第三方相机库）· kotlinx-serialization

## 路线图

- [ ] Android：每日打卡提醒（WorkManager）、跌倒检测（加速度计）、HRV 长期基线
- [ ] 云端同步与家属远程查看（需后端，可选）
- [ ] **鸿蒙 HarmonyOS NEXT 版**（ArkTS 原生开发，复用同一套风险规则引擎逻辑）
- [ ] 正式签名 keystore 与上架

## 免责声明

本应用的预警基于公开医学共识阈值与健康统计方法，用于健康管理参考，不能替代医生诊断和正规医疗设备（PPG 测量精度亦低于医用指夹式血氧仪/心电）。紧急情况永远优先拨打 120。

