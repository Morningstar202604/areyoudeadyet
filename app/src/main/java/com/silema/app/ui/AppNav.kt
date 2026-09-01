package com.silema.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.silema.app.data.Contact
import com.silema.app.data.VitalRecord
import com.silema.app.util.TtsController

object Routes {
    const val HOME = "home"
    const val ENTRY = "entry"
    const val REPORT = "report"
    const val WORKOUT = "workout"
    const val GUARDIAN = "guardian"
    const val DEVICES = "devices"
    const val SOS = "sos"
    const val FAMILY = "family"
    const val AI_REPORT = "ai_report"
    const val MEDICAL = "medical"
    const val MORE = "more"
}

private data class TabSpec(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppRoot(records: List<VitalRecord>, contacts: List<Contact>, tts: TtsController) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // 纯守护端底部 5 项：监护看板 / 连接手表 / 健康报告 / 远程家人 / 更多功能
    val tabs = listOf(
        TabSpec(Routes.HOME, "监护", Icons.Filled.Home),
        TabSpec(Routes.DEVICES, "设备", Icons.Filled.Build),
        TabSpec(Routes.REPORT, "报告", Icons.Filled.DateRange),
        TabSpec(Routes.FAMILY, "家人", Icons.Filled.Star),
        TabSpec(Routes.MORE, "更多", Icons.Filled.Menu)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.HOME) {
                DashboardScreenV3(
                    records = records,
                    tts = tts,
                    onGoSos = { navController.navigate(Routes.SOS) { launchSingleTop = true } },
                    onGoEntry = { navController.navigate(Routes.ENTRY) { launchSingleTop = true } },
                    onGoDevices = { navController.navigate(Routes.DEVICES) { launchSingleTop = true } },
                    onGoWorkout = { navController.navigate(Routes.WORKOUT) { launchSingleTop = true } },
                    onGoGuardian = { navController.navigate(Routes.GUARDIAN) { launchSingleTop = true } },
                    onGoFamily = { navController.navigate(Routes.FAMILY) { launchSingleTop = true } },
                    onGoAi = { navController.navigate(Routes.AI_REPORT) { launchSingleTop = true } },
                    onGoMedical = { navController.navigate(Routes.MEDICAL) { launchSingleTop = true } }
                )
            }
            composable(Routes.REPORT) { ReportScreen(records = records) }
            composable(Routes.GUARDIAN) {
                GuardianScreen(
                    records = records,
                    onGoMedical = { navController.navigate(Routes.MEDICAL) { launchSingleTop = true } }
                )
            }
            composable(Routes.DEVICES) {
                DevicesScreen(onClose = { navController.popBackStack() })
            }
            composable(Routes.FAMILY) { FamilyScreen() }
            composable(Routes.AI_REPORT) { AiReportScreen(records = records) }
            composable(Routes.MEDICAL) { MedicalScreen(records = records) }
            composable(Routes.MORE) {
                MoreScreen(onNav = { route -> navController.navigate(route) { launchSingleTop = true } })
            }
        }
    }
}
