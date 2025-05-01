package com.masum.musicplayer.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.masum.musicplayer.navigation.Screen

@Composable
fun ModernBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text(Screen.Home.title) },
            selected = currentRoute == Screen.Home.route,
            onClick = { onNavigate(Screen.Home.route) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text(Screen.Settings.title) },
            selected = currentRoute == Screen.Settings.route,
            onClick = { onNavigate(Screen.Settings.route) }
        )
    }
} 