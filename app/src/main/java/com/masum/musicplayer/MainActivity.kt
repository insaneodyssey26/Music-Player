package com.masum.musicplayer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.masum.musicplayer.navigation.AppNavigation
import com.masum.musicplayer.presentation.viewmodel.MusicViewModel
import com.masum.musicplayer.presentation.viewmodel.MusicViewModelFactory
import com.masum.musicplayer.theme.ThemeManager

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MusicViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Initialize ViewModel after permission is granted
            initializeViewModel()
        } else {
            // Handle permission denied
            // You might want to show a dialog explaining why the permission is needed
            // or disable functionality that depends on this permission
        }
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(
            this,
            MusicViewModelFactory(applicationContext)
        )[MusicViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request permission based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        setContent {
            ThemeManager.MusicPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (::viewModel.isInitialized) {
                        AppNavigation(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
