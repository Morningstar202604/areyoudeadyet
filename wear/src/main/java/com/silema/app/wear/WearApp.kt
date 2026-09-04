package com.silema.app.wear

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.silema.app.wear.theme.WearTheme
import com.silema.app.wear.ui.AiBriefScreen
import com.silema.app.wear.ui.EntryScreen
import com.silema.app.wear.ui.WearHomeScreenV3
import com.silema.app.wear.ui.WearSettingsScreenV3
import com.silema.app.wear.ui.WearSosScreenV3
import com.silema.app.wear.ui.WearWorkoutScreenV3

enum class Screen { Home, Entry, Sos, Workout, Ai, Settings }

@Composable
fun WearApp() {
    WearTheme {
        val navStack = remember { mutableStateListOf(Screen.Home) }
        val currentScreen = navStack.lastOrNull() ?: Screen.Home

        fun navigate(screen: Screen) {
            navStack.add(screen)
        }

        fun popBack(): Boolean {
            if (navStack.size > 1) {
                navStack.removeLast()
                return true
            }
            return false
        }

        Scaffold(
            timeText = { TimeText() },
            vignette = {
                Vignette(vignettePosition = VignettePosition.TopAndBottom)
            },
        ) {
            when (currentScreen) {
                Screen.Home ->
                    WearHomeScreenV3(
                        onGoMeasure = { navigate(Screen.Entry) },
                        onGoWorkout = { navigate(Screen.Workout) },
                        onGoSos = { navigate(Screen.Sos) },
                        onGoSettings = { navigate(Screen.Settings) },
                        onGoAi = { navigate(Screen.Ai) },
                    )
                Screen.Entry ->
                    EntryScreen(
                        onNavigate = { navigate(it) },
                        onBack = { popBack() },
                    )
                Screen.Sos ->
                    WearSosScreenV3(
                        onSosTriggered = { /* SOS triggered */ },
                        onBack = { popBack() },
                    )
                Screen.Workout ->
                    WearWorkoutScreenV3(
                        onBack = { popBack() },
                    )
                Screen.Ai ->
                    AiBriefScreen(
                        onNavigate = { navigate(it) },
                        onBack = { popBack() },
                    )
                Screen.Settings ->
                    WearSettingsScreenV3(
                        onBack = { popBack() },
                    )
            }
        }
    }
}
