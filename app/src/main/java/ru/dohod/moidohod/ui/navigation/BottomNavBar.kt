package ru.dohod.moidohod.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

data class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

val navItems = listOf(
    NavItem(Screen.Dashboard.route, "Главная", Icons.Default.Home),
    NavItem(Screen.Calendar.route, "Календарь", Icons.Default.DateRange),
    NavItem(Screen.Tasks.route, "Заявки", Icons.Default.List),
    NavItem(Screen.Payments.route, "Выплаты", Icons.Default.Payment),
    NavItem(Screen.Settings.route, "Настройки", Icons.Default.Settings)
)

@Composable
fun BottomNavBar(navController: NavHostController) {
    val backStackEntry = navController.currentBackStackEntryAsState()
    val destination = backStackEntry.value?.destination

    NavigationBar {
        navItems.forEach { item ->
            val selected = destination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        // Очищаем стек до нужного пункта
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) }
            )
        }
    }
}