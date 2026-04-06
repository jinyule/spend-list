package com.spendlist.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Stats : Screen("stats")
    data object Settings : Screen("settings")
    data object AddSubscription : Screen("subscription/add")
    data object CategoryManage : Screen("categories")

    companion object {
        const val EDIT_SUBSCRIPTION_ROUTE = "subscription/edit/{id}"
        const val SUBSCRIPTION_DETAIL_ROUTE = "subscription/{id}"

        fun editSubscription(id: Long) = "subscription/edit/$id"
        fun subscriptionDetail(id: Long) = "subscription/$id"
    }
}

enum class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val labelResId: Int
) {
    HOME(Screen.Home, Icons.Default.Home, com.spendlist.app.R.string.nav_home),
    STATS(Screen.Stats, Icons.Default.BarChart, com.spendlist.app.R.string.nav_stats),
    SETTINGS(Screen.Settings, Icons.Default.Settings, com.spendlist.app.R.string.nav_settings)
}
