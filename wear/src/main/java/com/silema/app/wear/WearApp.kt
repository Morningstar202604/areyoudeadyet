package com.silema.app.wear

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.silema.app.wear.theme.WearTheme
import com.silema.app.wear.ui.AiBriefScreen
import com.silema.app.wear.ui.EntryScreen
import com.silema.app.wear.ui.HomeScreen
import com.silema.app.wear.ui.SosScreen
import com.silema.app.wear.ui.WorkoutScreen

enum class Screen { Home, Entry, Sos, Workout, Ai }

@Composable
fun WearApp() {
    WearTheme {
        // 手表端屏幕少、切换直接，用最简化的状态机导航（不引 wear-navigation）。
        var screen by remember { mutableStateOf(Screen.Home) }
        Scaffold(
            timeText = { TimeText() },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
        ) {
            when (screen) {
                Screen.Home -> HomeScreen(onNavigate = { screen = it })
                Screen.Entry -> EntryScreen(onNavigate = { screen = it })
                Screen.Sos -> SosScreen(onNavigate = { screen = it })
                Screen.Workout -> WorkoutScreen(onNavigate = { screen = it })
                Screen.Ai -> AiBriefScreen(onNavigate = { screen = it })
            }
        }
    }
}
