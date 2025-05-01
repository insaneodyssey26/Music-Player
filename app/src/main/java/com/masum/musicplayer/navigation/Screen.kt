package com.masum.musicplayer.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object NowPlaying : Screen("now_playing", "Now Playing", Icons.Default.MusicNote)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
} 