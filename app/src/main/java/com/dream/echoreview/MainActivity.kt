package com.dream.echoreview

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dream.echoreview.ui.screen.DetailScreen
import com.dream.echoreview.ui.screen.HomeScreen
import com.dream.echoreview.ui.screen.RecordingScreen
import com.dream.echoreview.ui.screen.SettingsScreen
import com.dream.echoreview.ui.theme.EchoReviewTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val permissionsToRequest = mutableListOf(
        Manifest.permission.RECORD_AUDIO
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            EchoReviewTheme {
                val navController = rememberNavController()
                
                LaunchedEffect(Unit) {
                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                }

                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        HomeScreen(
                            onNavigateToRecording = { navController.navigate("recording") },
                            onNavigateToDetail = { id -> navController.navigate("detail/$id") },
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("recording") {
                        RecordingScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("detail/{sessionId}") { backStackEntry ->
                        val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                        DetailScreen(
                            sessionId = sessionId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
