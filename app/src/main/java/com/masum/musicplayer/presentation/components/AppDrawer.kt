package com.masum.musicplayer.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.masum.musicplayer.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    currentRoute: String,
    navigateTo: (String) -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // App Title
            Text(
                text = "Music Player",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )
            
            // Signature
            Text(
                text = "Made by Masum :)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // Navigation Items
            val screens = listOf(Screen.Home, Screen.Settings)
            screens.forEach { screen ->
                NavigationDrawerItem(
                    icon = { Icon(screen.icon, contentDescription = null) },
                    label = { Text(screen.title) },
                    selected = currentRoute == screen.route,
                    onClick = { navigateTo(screen.route) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Version Info
            Text(
                text = "Version Beta 2.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
} 