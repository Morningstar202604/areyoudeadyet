package com.silema.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silema.app.ui.theme.AppShapes

/**
 * 现代底部导航栏组件（参考 Keep 设计风格）。
 *
 * 特点：
 * - 浮动卡片式设计，带圆角和阴影
 * - 选中项有渐变背景高亮
 * - 图标+文字，清晰易识别
 * - 平滑的颜色过渡动画
 * - 适老化大图标大文字
 */
@Composable
fun ModernBottomNav(
    items: List<BottomNavItem>,
    currentRoute: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(AppShapes.banner)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 300),
                    label = "navIconColor",
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 300),
                    label = "navTextColor",
                )

                Column(
                    modifier =
                        Modifier
                            .clip(AppShapes.card)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onItemClick(item.route) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // 选中项的渐变背景
                    if (isSelected) {
                        Box(
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            ),
                                        ),
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = iconColor,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    } else {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

/**
 * 底部导航项数据类。
 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)
