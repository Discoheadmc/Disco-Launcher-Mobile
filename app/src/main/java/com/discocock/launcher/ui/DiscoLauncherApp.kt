package com.discocock.launcher.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.discocock.launcher.DiscoApplication
import com.discocock.launcher.ui.screens.AccountsScreen
import com.discocock.launcher.ui.screens.HomeScreen
import com.discocock.launcher.ui.screens.RuntimeScreen
import com.discocock.launcher.ui.screens.SettingsScreen
import com.discocock.launcher.ui.screens.StoreScreen
import com.discocock.launcher.ui.screens.VersionsScreen

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun DiscoLauncherApp() {
    val context = LocalContext.current
    val container = (context.applicationContext as DiscoApplication).container
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        container.accounts.load()
        container.instances.load()
        container.runtimes.load()
    }

    val launchButtonColorHex by container.settings.launchButtonColor.collectAsState(initial = "")
    val launchButtonColor =
        if (launchButtonColorHex.isBlank()) Color.Unspecified else parseHexColor(launchButtonColorHex)

    val items = listOf(
        BottomItem("home", "Ana Ekran", Icons.Filled.Home),
        BottomItem("store", "Mağaza", Icons.Filled.Storefront),
        BottomItem("accounts", "Hesaplar", Icons.Filled.Person),
        BottomItem("settings", "Ayarlar", Icons.Filled.Settings),
    )

    staticCompositionLocalProvider(LocalLaunchButtonColor provides launchButtonColor) {
        Scaffold(
            bottomBar = {
                if (items.any { it.route == currentRoute }) {
                    NavigationBar {
                        items.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.route,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(padding),
            ) {
                composable("home") {
                    HomeScreen(
                        onOpenVersions = { navController.navigate("versions") },
                        onOpenRuntime = { navController.navigate("runtime") },
                    )
                }
                composable("store") { StoreScreen() }
                composable("accounts") { AccountsScreen() }
                composable("settings") { SettingsScreen() }
                composable("versions") { VersionsScreen(onBack = { navController.popBackStack() }) }
                composable("runtime") { RuntimeScreen(onBack = { navController.popBackStack() }) }
            }
        }
    }
}

private fun parseHexColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFF7C8CF8))
