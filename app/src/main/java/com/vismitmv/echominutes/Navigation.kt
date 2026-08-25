package com.vismitmv.echominutes

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.vismitmv.echominutes.ui.history.HistoryScreen
import com.vismitmv.echominutes.ui.record.RecordScreen
import com.vismitmv.echominutes.ui.result.ResultScreen
import com.vismitmv.echominutes.ui.settings.SettingsScreen

private data class BottomNavItem(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(RecordRoute, "Record", Icons.Filled.Mic, Icons.Outlined.Mic),
    BottomNavItem(HistoryRoute, "History", Icons.Filled.History, Icons.Outlined.History),
    BottomNavItem(SettingsRoute, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun EchoMinutesApp() {
    val backStack = remember { mutableStateListOf<Any>(RecordRoute) }
    val currentRoute = backStack.lastOrNull()

    val showBottomNav = currentRoute is RecordRoute ||
            currentRoute is HistoryRoute ||
            currentRoute is SettingsRoute

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute != null &&
                                currentRoute::class == item.route::class
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    backStack.clear()
                                    backStack.add(item.route)
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(innerPadding),
            onBack = { backStack.removeLastOrNull() },
            entryProvider = { route ->
                when (route) {
                    is RecordRoute -> NavEntry(route) {
                        RecordScreen(
                            onNavigateToResult = { id -> backStack.add(ResultRoute(id)) },
                            onNavigateToSettings = { backStack.add(SettingsRoute) }
                        )
                    }
                    is ResultRoute -> NavEntry(route) {
                        ResultScreen(
                            meetingId = route.meetingId,
                            onNavigateBack = { backStack.removeLastOrNull() }
                        )
                    }
                    is HistoryRoute -> NavEntry(route) {
                        HistoryScreen(
                            onNavigateToResult = { id -> backStack.add(ResultRoute(id)) }
                        )
                    }
                    is SettingsRoute -> NavEntry(route) {
                        SettingsScreen()
                    }
                    else -> NavEntry(route) { /* fallback */ }
                }
            }
        )
    }
}
