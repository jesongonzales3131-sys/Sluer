package com.sluer.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.sluer.app.ui.screens.ConsoleScreen
import com.sluer.app.ui.screens.HomeScreen
import com.sluer.app.ui.screens.PlaceholderScreen

private enum class Tab(val label: String) {
    HOME("Home"),
    BOOST("Boost"),
    DEBLOAT("Debloat"),
    BATTERY("Battery"),
    TWEAKS("Tweaks"),
}

private fun iconFor(tab: Tab): ImageVector = when (tab) {
    Tab.HOME -> Icons.Default.Home
    Tab.BOOST -> Icons.Default.PlayArrow
    Tab.DEBLOAT -> Icons.Default.Delete
    Tab.BATTERY -> Icons.Default.Star
    Tab.TWEAKS -> Icons.Default.Settings
}

@Composable
fun SluerApp() {
    var tab by remember { mutableStateOf(Tab.HOME) }
    var showConsole by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t && !showConsole,
                        onClick = {
                            tab = t
                            showConsole = false
                        },
                        icon = { Icon(iconFor(t), contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                showConsole -> ConsoleScreen(onBack = { showConsole = false })
                tab == Tab.HOME -> HomeScreen(onOpenConsole = { showConsole = true })
                tab == Tab.BOOST -> PlaceholderScreen(
                    title = "Boost",
                    message = "One-tap memory cleanup and force-stop of a custom app list is coming in the next build.",
                )
                tab == Tab.DEBLOAT -> PlaceholderScreen(
                    title = "Debloat",
                    message = "Disable or uninstall system apps (fully reversible) is coming in the next build.",
                )
                tab == Tab.BATTERY -> PlaceholderScreen(
                    title = "Battery",
                    message = "Per-app standby buckets and Doze whitelist control is coming in the next build.",
                )
                tab == Tab.TWEAKS -> PlaceholderScreen(
                    title = "Tweaks",
                    message = "Animation scales, Private DNS and background process limit tweaks are coming in the next build.",
                )
            }
        }
    }
}
