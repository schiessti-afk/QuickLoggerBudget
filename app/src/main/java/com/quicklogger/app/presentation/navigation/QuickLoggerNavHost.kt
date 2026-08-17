package com.quicklogger.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.quicklogger.app.presentation.history.HistoryScreen
import com.quicklogger.app.presentation.log.LogScreen

@Composable
fun QuickLoggerNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.LOG,
        modifier = modifier,
    ) {
        composable(Routes.LOG) {
            LogScreen(
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onNavigateUp = { navController.popBackStack() },
            )
        }
    }
}
