package com.nichita.swearingtaskapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nichita.swearingtaskapp.ui.settings.SettingsScreen
import com.nichita.swearingtaskapp.ui.tasks.TasksScreen
import kotlinx.serialization.Serializable

@Serializable
object TasksRoute

@Serializable
object SettingsRoute

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = TasksRoute) {
        composable<TasksRoute> {
            TasksScreen(onOpenSettings = { navController.navigate(SettingsRoute) { launchSingleTop = true } })
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
