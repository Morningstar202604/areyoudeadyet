package com.silema.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// V3 界面通用条目模型：屏幕里的静态列表（功能开关、设备、用药等）
// 统一用这些 data class 描述，替代借用 Triple 凑数导致超出三元限制的写法。

/** 图标 + 文案 + 单色：守护功能开关、可连接设备等。 */
data class IconItem(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
)

/** 图标 + 文案 + 渐变背景：通知方式、已连接设备、快捷功能、设置项等。 */
data class GradientItem(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val gradient: List<Color>,
)

/** 单色 + 渐变双属性：测量类型卡片（入口屏幕）。 */
data class MeasureTypeItem(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val gradient: List<Color>,
)

/** 用药提醒条目。 */
data class MedicationItem(
    val name: String,
    val dosage: String,
    val time: String,
    val isTaken: Boolean,
)
