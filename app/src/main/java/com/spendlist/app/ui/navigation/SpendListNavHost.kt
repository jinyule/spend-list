package com.spendlist.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.spendlist.app.ui.screen.addEdit.AddEditScreen
import com.spendlist.app.ui.screen.category.CategoryManageScreen
import com.spendlist.app.ui.screen.detail.SubscriptionDetailScreen
import com.spendlist.app.ui.screen.home.HomeScreen
import com.spendlist.app.ui.screen.settings.SettingsScreen
import com.spendlist.app.ui.screen.stats.StatsScreen

@Composable
fun SpendListNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onAddClick = { navController.navigate(Screen.AddSubscription.route) },
                onSubscriptionClick = { id ->
                    navController.navigate(Screen.subscriptionDetail(id))
                }
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onCategoryManageClick = {
                    navController.navigate(Screen.CategoryManage.route)
                }
            )
        }

        composable(Screen.AddSubscription.route) {
            AddEditScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EDIT_SUBSCRIPTION_ROUTE,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) {
            AddEditScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SUBSCRIPTION_DETAIL_ROUTE,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) {
            SubscriptionDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onEditClick = { id ->
                    navController.navigate(Screen.editSubscription(id))
                }
            )
        }

        composable(Screen.CategoryManage.route) {
            CategoryManageScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
