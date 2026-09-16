package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.decode.VideoFrameDecoder
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.player.PlaybackService
import com.example.ui.AppNavigation
import com.example.ui.MediaViewModel
import com.example.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {
    private val viewModel: MediaViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val serviceIntent = Intent(this, PlaybackService::class.java)
        startService(serviceIntent)

        setContent {
            val imageLoader = ImageLoader.Builder(this)
                .components {
                    add(VideoFrameDecoder.Factory())
                }
                .crossfade(true)
                .build()
                
            CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                val currentThemeState by viewModel.themeState.collectAsStateWithLifecycle()
                MyApplicationTheme(appThemeState = currentThemeState) {
                val permissions = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
                    permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }

                val permissionState = rememberMultiplePermissionsState(permissions = permissions)

                if (permissionState.allPermissionsGranted) {
                    AppNavigation(viewModel = viewModel)
                } else {
                    PermissionRequestScreen(permissionState)
                }
            }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestScreen(permissionState: MultiplePermissionsState) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
            Text("Grant Media Permissions")
        }
    }
}
