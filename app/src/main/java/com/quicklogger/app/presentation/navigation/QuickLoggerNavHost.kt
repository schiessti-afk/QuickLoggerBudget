package com.quicklogger.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quicklogger.app.presentation.categories.ManageCategoriesDialog
import com.quicklogger.app.presentation.expenseedit.ExpenseEditScreen
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
            var showManageCategories by remember { mutableStateOf(false) }

            HistoryScreen(
                onNavigateUp = { navController.popBackStack() },
                onEditExpense = { id -> navController.navigate(Routes.expenseEdit(id)) },
                onManageCategories = { showManageCategories = true },
            )

            if (showManageCategories) {
                ManageCategoriesDialog(onDismiss = { showManageCategories = false })
            }
        }
        composable(
            route = Routes.EXPENSE_EDIT,
            arguments = listOf<NamedNavArgument>(navArgument("id") { type = NavType.LongType }),
        ) {
            ExpenseEditScreen(
                onNavigateUp = { navController.popBackStack() },
            )
        }
    }
}
