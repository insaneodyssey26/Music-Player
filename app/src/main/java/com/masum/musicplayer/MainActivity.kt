package com.masum.musicplayer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import com.masum.musicplayer.navigation.AppNavigation
import com.masum.musicplayer.presentation.viewmodel.MusicViewModel
import com.masum.musicplayer.presentation.viewmodel.MusicViewModelFactory
import com.masum.musicplayer.theme.ThemeManager

class MainActivity : ComponentActivity() {
    private var viewModel: MusicViewModel? = null

    // Compose state for permission and loading
    private val permissionState = mutableStateOf<Boolean?>(null)
    private val loadingState = mutableStateOf(true)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            permissionState.value = isGranted
            if (isGranted) {
                viewModel = ViewModelProvider(
                    this,
                    MusicViewModelFactory(applicationContext)
                )[MusicViewModel::class.java]
                loadingState.value = false
            } else {
                loadingState.value = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PermissionHandlerContent()
        }
        requestPermission()
    }

    private fun requestPermission() {
        loadingState.value = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    @Composable
    fun PermissionHandlerContent() {
        ThemeManager.MusicPlayerTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                when {
                    loadingState.value -> {
                        CircularProgressIndicator()
                    }
                    permissionState.value == true && viewModel != null -> {
                        AppNavigation(viewModel = viewModel!!)
                    }
                    permissionState.value == false -> {
                        PermissionDeniedScreen {
                            requestPermission()
                        }
                    }
                }
            }
        }
    }



    // Composable for permission denied UI
    @Composable
    fun PermissionDeniedScreen(onRequestPermission: () -> Unit) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Permission denied. Please grant storage permission to use the app.")
                Button(onClick = onRequestPermission) {
                    Text("Grant Permission")
                }
            }
        }
    }
}
