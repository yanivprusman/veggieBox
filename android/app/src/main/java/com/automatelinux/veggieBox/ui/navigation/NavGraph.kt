package com.automatelinux.veggieBox.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.automatelinux.veggieBox.BuildConfig
import com.automatelinux.veggieBox.ui.MainViewModel
import com.automatelinux.veggieBox.ui.detail.StopDetailScreen
import com.automatelinux.veggieBox.ui.greeting.GreetingScreen
import com.automatelinux.veggieBox.ui.map.MapScreen
import com.automatelinux.veggieBox.ui.route.RouteScreen
import com.automatelinux.feedbacklib.ui.chat.FeedbackChatScreen
import com.automatelinux.feedbacklib.ui.issues.FeedbackIssuesScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("route", "מסלול", Icons.AutoMirrored.Filled.List),
    Tab("map", "מפה", Icons.Filled.Map),
    Tab("greeting", "חסרי פרטים", Icons.Filled.MarkChatUnread),
)

@Composable
fun AppNavGraph() {
    val nav = rememberNavController()
    // Activity-scoped VM shared across all tabs + the detail screen.
    val vm: MainViewModel = hiltViewModel()

    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination
    val showBottomBar = tabs.any { t -> current?.hierarchy?.any { it.route == t.route } == true }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = current?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = "route", modifier = Modifier.padding(pad)) {
            composable("route") {
                RouteScreen(
                    vm = vm,
                    onOpenStop = { nav.navigate("stop/$it") },
                    onOpenMap = { nav.navigate("map") },
                    onOpenFeedback = { nav.navigate("feedback") },
                    showFeedback = !BuildConfig.IS_PROD,
                )
            }
            composable("map") { MapScreen(vm) }
            composable("greeting") { GreetingScreen(vm) }
            composable("stop/{id}") { entry ->
                StopDetailScreen(
                    vm = vm,
                    stopId = entry.arguments?.getString("id")?.toIntOrNull() ?: 0,
                    onBack = { nav.popBackStack() },
                )
            }
            composable("feedback") {
                FeedbackChatScreen(
                    onNavigateBack = { nav.popBackStack() },
                    onNavigateToIssues = { nav.navigate("issues") },
                    isProd = BuildConfig.IS_PROD,
                )
            }
            composable("issues") {
                FeedbackIssuesScreen(
                    onNavigateBack = { nav.popBackStack() },
                    isProd = BuildConfig.IS_PROD,
                )
            }
        }
    }
}
