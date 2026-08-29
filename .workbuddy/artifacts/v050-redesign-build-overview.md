# 「死了吗？」v0.5.0 — 视觉重设计 + 修复包构建总览

## 一、本轮完成内容

### 1. 前端设计系统重做（温暖医疗专业风）
- **Theme.kt**：所有品牌色/风险色改为深饱和、白字对比度达标的取值（BrandWarm=0xFFD84315、BrandBlue=0xFF1565C0、BrandGreen=0xFF2E7D32、BrandSoftRed=0xFFC62828 等）；新增尺寸令牌对象 `AppShapes`(banner/card/button/chip/small)、`AppSpacing`(xs~xxl/screenPad)、`AppSize`(bigButtonHeight=76dp / sosButtonHeight=76dp / listItemMin=72dp)；Typography 适老化大字号重载（displayLarge=56sp）；新增 `riskSoft()` / `SosGradient`。
- **Widgets.kt**：10 个组件全部改用令牌，消除 Magic Number。

### 2. 全屏可达性重构（修复"五屏无导航入口"）
- **AppNav.kt**：底栏改为 5 项主入口（首页/录入/健康/家人/更多），SOS 改为常驻红色 `FloatingActionButton`（FabPosition.End）；新增 `Routes.MORE` 与 `MoreScreen`。
- **MoreScreen.kt**（新建）：功能中心屏，统一收容 检测中心/运动追踪/AI分析/医疗对接/设置。

### 3. 各屏应用新设计 + 修 Bug（任务 #5 收尾）
| 屏幕 | 处理 |
|------|------|
| DashboardScreen | 移除问候语 emoji（适老医疗场景显随意）；`RoundedCornerShape` 全令牌化；`items` 补 key |
| EntryScreen | 形状令牌化；`items(recent)` 补 key |
| ReportScreen | 形状令牌化；`items(history)` 补 key |
| FamilyScreen | 形状令牌化（原已带 key） |
| GuardianScreen | 形状令牌化；`items(contactsList)` 补 key |
| MedicalScreen | 形状令牌化 |
| Devices/Workout/Sos/AiReport | 上一轮已完成令牌化/key/硬编码色修复 |
| **Previews.kt**（新建） | 10 个主屏 `@Preview`，在 `SilemaTheme` 下渲染，便于 Android Studio 核对设计系统 |

### 4. 编译验证 + 出包（任务 #7）
- 修复 4 个编译错误：`AppNav` 缺 `dp` 导入；`DashboardScreen`/`GuardianScreen` 的 `items` key lambda 引用了内容 lambda 参数（作用域错误）。
- `./gradlew :app:assembleRelease` **BUILD SUCCESSFUL**（EXIT=0）。
- 产物：`app/build/outputs/apk/release/app-release.apk`（12.9 MB），**apksigner 验证通过**，签名证书 `CN=Silema AreYouDead`（即 release keystore）。
- 已复制到 `dist/silema-v0.5.0-release.apk`，补齐此前滞后的 v0.4.0 产物。

## 二、关键决策
- 保留全部旧导出名（避免 40+ 文件引用崩溃），仅改色值与新增令牌对象。
- 受限编辑环境下，将 10 个 `@Preview` 收敛进单一 `Previews.kt`（新建文件，规避逐文件编辑被格式化器锁定的问题）。
- `@Preview` 不参与 release 产物；本次优先级为跑通 v0.5.0 修复包，运行期 UI 即"重设计后的成品"。

## 三、交付物
- `dist/silema-v0.5.0-release.apk` — 已签名 release 包（v0.5.0 / versionCode 5）
- `app/src/main/java/com/silema/app/ui/Previews.kt` — 设计系统预览
- `app/src/main/java/com/silema/app/ui/theme/Theme.kt`、`ui/components/Widgets.kt` — 设计系统 v2 令牌

## 四、遗留 / 后续
- **AiAnalyzerProvider / RemoteSyncProvider** 仍永远返回 Mock（架构占位，README 宣称的"通义/智谱/文心真接入""远程同步"未实现）——属后端缺口，非前端。
- `dist/` 仍保留 v0.3.0/v0.4.0 历史包（未删，非破坏性）；如需清理可单独确认。
- README 功能宣称与实现仍有偏差，建议后续单独对齐（不在本轮 UI 重设计范围）。
