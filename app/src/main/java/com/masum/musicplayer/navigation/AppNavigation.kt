package com.masum.musicplayer.navigation

import androidx.compose.animation.core.EaseInQuart
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.masum.musicplayer.presentation.components.AppDrawer
import com.masum.musicplayer.presentation.components.ModernBottomBar
import com.masum.musicplayer.presentation.components.ModernTopBar
import com.masum.musicplayer.presentation.screens.HomeScreen
import com.masum.musicplayer.presentation.screens.NowPlayingScreen
import com.masum.musicplayer.presentation.screens.SettingsScreen
import com.masum.musicplayer.presentation.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: MusicViewModel) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination
    val currentRoute = currentDestination?.route ?: Screen.Home.route
    
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            AppDrawer(
                currentRoute = currentRoute,
                navigateTo = { route ->
                    scope.launch {
                        drawerState.close()
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                if (currentRoute != Screen.NowPlaying.route) {
                    ModernTopBar(
                        title = when (currentRoute) {
                            Screen.Home.route -> Screen.Home.title
                            Screen.Settings.route -> Screen.Settings.title
                            else -> Screen.Home.title
                        },
                        onMenuClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (currentRoute != Screen.NowPlaying.route) {
                    ModernBottomBar(
                        currentDestination = currentDestination,
                        navController = navController
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(
                    route = Screen.Home.route,
                    enterTransition = { fadeIn(animationSpec = tween(300, easing = EaseInQuart)) },
                    exitTransition = { fadeOut(animationSpec = tween(300, easing = EaseOutQuart)) }
                ) {
                    HomeScreen(
                        viewModel = viewModel,
                        onSongClick = { song ->
                            viewModel.playSong(song)
                            navController.navigate(Screen.NowPlaying.route)
                        },
                        onNavigateToNowPlaying = {
                            navController.navigate(Screen.NowPlaying.route)
                        }
                    )
                }
                
                composable(
                    route = Screen.Settings.route,
                    enterTransition = { fadeIn(animationSpec = tween(300, easing = EaseInQuart)) },
                    exitTransition = { fadeOut(animationSpec = tween(300, easing = EaseOutQuart)) }
                ) {
                    SettingsScreen(
                        navController = navController,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }
                
                composable(
                    route = Screen.NowPlaying.route,
                    enterTransition = {
                        slideInVertically(
                            animationSpec = tween(300, easing = EaseInQuart),
                            initialOffsetY = { it }
                        ) + fadeIn(animationSpec = tween(300, easing = EaseInQuart))
                    },
                    exitTransition = {
                        slideOutVertically(
                            animationSpec = tween(300, easing = EaseOutQuart),
                            targetOffsetY = { it }
                        ) + fadeOut(animationSpec = tween(300, easing = EaseOutQuart))
                    }
                ) {
                    NowPlayingScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
} 