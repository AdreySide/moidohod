package ru.dohod.moidohod.ui.navigation

import ru.dohod.moidohod.ui.screens.calendar.CalendarScreen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ru.dohod.moidohod.ui.screens.dashboard.DashboardScreen
import ru.dohod.moidohod.ui.screens.payments.PaymentsScreen
import ru.dohod.moidohod.ui.screens.settings.SettingsScreen
import ru.dohod.moidohod.ui.screens.tasks.TasksScreen

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Calendar : Screen("calendar")
    data object Tasks : Screen("tasks")
    data object Payments : Screen("payments")
    data object Settings : Screen("settings")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen()
        }
        composable(Screen.Calendar.route) {
            CalendarScreen()
        }
        composable(Screen.Tasks.route) {
            TasksScreen()
        }
        composable(Screen.Payments.route) {
            PaymentsScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
        composable(Screen.Payments.route) {
            PaymentsScreen()
        }
    }
}