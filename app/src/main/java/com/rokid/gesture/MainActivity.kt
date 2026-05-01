package com.rokid.gesture

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rokid.gesture.ui.HudScreen
import com.rokid.gesture.ui.MenuScreen
import com.rokid.gesture.ui.SettingsScreen
import com.rokid.gesture.viewmodel.GestureViewModel

class MainActivity : ComponentActivity() {

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled by recomposition */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Fullscreen immersive — status/nav bars hidden for clean HUD ──────────────────────
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        // Keep screen on while the HUD is running
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            // HUD palette: black = transparent on OLED HUD display
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary      = Color(0xFFFFD600),   // Rokid gold — readable on see-through
                    secondary    = Color(0xFF80E5FF),   // cyan accent
                    background   = Color.Black,          // transparent on OLED HUD
                    surface      = Color(0xCC000000),   // semi-transparent panels
                    onPrimary    = Color.Black,
                    onBackground = Color.White,
                    onSurface    = Color.White
                )
            ) {
                // Use transparent background so black = see-through on OLED HUD
                Surface(Modifier.fillMaxSize(), color = Color.Transparent) {
                    val vm: GestureViewModel = viewModel()
                    MainNav(vm)
                }
            }
        }
    }
}

@Composable
fun MainNav(vm: GestureViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            // Navigation bar is visible but auto-hides when not needed
            NavigationBar(containerColor = Color(0xDD000000)) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    icon     = { Icon(Icons.Default.Visibility, null) },
                    label    = { Text("HUD") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    icon     = { Icon(Icons.Default.List, null) },
                    label    = { Text("Menu") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick  = { selectedTab = 2 },
                    icon     = { Icon(Icons.Default.Settings, null) },
                    label    = { Text("Settings") }
                )
            }
        }
    ) { _ ->
        when (selectedTab) {
            0 -> HudScreen(vm)
            1 -> MenuScreen(vm)
            2 -> SettingsScreen(vm)
        }
    }
}
