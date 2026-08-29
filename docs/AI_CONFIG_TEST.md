# AI 健康分析配置测试指南

## 已完成的工作

### 1. 新增文件
- `app/src/main/java/com/silema/app/ai/CloudAiAnalyzer.kt` - 云端 AI 分析器

### 2. 修改文件
- `app/src/main/java/com/silema/app/ai/AiAnalyzerProvider.kt` - 支持动态切换云端/本地模式
- `app/src/main/java/com/silema/app/ui/GuardianScreen.kt` - 增加 AI 配置 UI
- `app/src/main/java/com/silema/app/ui/AiReportScreen.kt` - 增加自动降级逻辑

## 测试步骤

### 第一步：启动 Android 模拟器
1. 打开 Android Studio
2. Tools → Device Manager → Create Device（或选择已有模拟器）
3. 推荐配置：Pixel 6, API 33 (Android 13)
4. 点击 ▶ 启动模拟器

### 第二步：安装 APK
```bash
cd D:\00000\app2
.\gradlew.bat :app:installDebug
```

### 第三步：测试本地模式（默认）
1. 打开 App → 底部导航"更多" → "AI 健康分析"
2. 观察：应显示基于规则引擎的分析结果（无 API Key 时自动使用 LocalAiAnalyzer）
3. 验证点：
   - 风险评分环形图正常显示
   - 发现列表有内容（心率、血压等指标状态）
   - 建议列表有 4 条通用建议

### 第四步：配置云端 API
1. 返回主界面 → 底部导航"守护" → 滚动到"AI 健康分析"卡片
2. 填入测试配置：
   - **API Key**: `sk-j4TEjjV0fKgqvliSXc8jko2EHzBmXnazsVaGCUa0sxSmZAH7`
   - **API 地址**: `https://api.hcnsec.cn/v1`
   - **模型名称**: `qwen-plus`
3. 点击"保存配置"按钮
4. 观察：按钮变为绿色"✓ 已保存"，下方出现提示"AI 配置已保存..."

### 第五步：测试云端模式
1. 再次进入"AI 健康分析"页
2. 点击"重新分析"按钮（或等待自动触发）
3. 观察：
   - 加载动画出现（"正在分析您的健康数据…"）
   - 几秒后显示新的分析结果（来自云端 LLM）
   - 总结文字更自然、建议更个性化

### 第六步：测试降级逻辑
1. 断开网络（模拟器设置 → 飞行模式）
2. 再次点击"重新分析"
3. 观察：
   - 短暂加载后显示提示："云端 API 不可用，已自动切换到本地规则引擎"
   - 仍能看到分析结果（来自 LocalAiAnalyzer）
   - App 不崩溃、功能完整

## 预期行为对照表

| 场景 | API Key 状态 | 网络状态 | 使用的分析器 | 用户看到的结果 |
|------|-------------|---------|------------|--------------|
| 首次打开 | 未配置 | 任意 | LocalAiAnalyzer | 规则引擎分析 |
| 配置后 | 已配置 | 正常 | CloudAiAnalyzer | 云端 LLM 分析 |
| 配置后 | 已配置 | 断开 | Cloud → Local 降级 | 提示 + 规则引擎分析 |
| 清除配置 | 清空 | 任意 | LocalAiAnalyzer | 规则引擎分析 |

## 常见问题

### Q: 编译报错 `Unresolved reference 'jsonObject'`
A: 确保 `app/build.gradle.kts` 中有 `kotlinx-serialization-json` 依赖（本项目已包含）。

### Q: API 请求失败
A: 检查：
1. API Key 是否正确（你提供的 key 是 `sk-j4TE...`）
2. 模拟器能否访问外网（ping api.hcnsec.cn）
3. NewAPI 中转站是否正常运行

### Q: 云端分析很慢
A: 可能原因：
1. 网络延迟（国内访问某些模型接口较慢）
2. 模型本身推理时间长（Qwen-Plus 通常 3-5 秒）
3. 超时时间设为 30 秒，超过会触发降级

## 后续优化建议

1. **持久化配置**：当前 API Key 仅保存在内存中，重启 App 后丢失。建议改用 DataStore 或 SharedPreferences 持久化。
2. **模型选择下拉框**：UI 中改为下拉选择常见模型（qwen-plus / deepseek-chat / glm-4），减少用户输入错误。
3. **测试连接按钮**：增加"测试 API 连接"按钮，发送一条简单请求验证配置是否正确。
4. **用量统计**：显示本月已调用次数/剩余额度（如果 NewAPI 提供查询接口）。

## 代码审查要点

- [x] CloudAiAnalyzer 正确处理 JSON 解析异常
- [x] AiAnalyzerProvider 支持动态切换（apiKey 变化时重建实例）
- [x] AiReportScreen 自动降级逻辑覆盖网络异常和 API Key 缺失
- [x] GuardianScreen UI 清晰引导用户配置
- [ ] TODO: 配置持久化（DataStore/SharedPreferences）
- [ ] TODO: 敏感信息加密存储（API Key 不应明文保存）
