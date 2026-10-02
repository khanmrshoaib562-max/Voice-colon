package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.Screen
import com.example.ui.screens.AlarmScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PinLockScreen
import com.example.ui.screens.PrivacyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TestVoiceScreen
import com.example.ui.theme.VoiceFinderLockTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.checkInitialLockState()
        handleIntent(intent)

        setContent {
            VoiceFinderLockTheme {
                MainAppNavHost(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("EXTRA_ALARM_ACTIVE", false) == true) {
            val source = intent.getStringExtra("EXTRA_TRIGGER_SOURCE") ?: "Background Trigger"
            // Ensure alarm controller reflects this
        }
    }
}

@Composable
fun MainAppNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val isAlarmActive by viewModel.isAlarmActive.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val settings by viewModel.settings.collectAsState()

    // Automatically navigate to Alarm screen if alarm fires
    LaunchedEffect(isAlarmActive) {
        if (isAlarmActive) {
            navController.navigate(Screen.Alarm.route) {
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        val startDestination = if (settings.appLockEnabled && settings.appPin.isNotEmpty() && !isUnlocked) {
            Screen.PinLock.route
        } else {
            Screen.Home.route
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.PinLock.route) {
                PinLockScreen(
                    viewModel = viewModel,
                    onUnlocked = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.PinLock.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToTestVoice = { navController.navigate(Screen.TestVoice.route) },
                    onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToTestVoice = { navController.navigate(Screen.TestVoice.route) },
                    onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) }
                )
            }

            composable(Screen.TestVoice.route) {
                TestVoiceScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Alarm.route) {
                AlarmScreen(
                    viewModel = viewModel,
                    onAlarmStopped = {
                        navController.popBackStack(Screen.Home.route, false)
                    }
                )
            }

            composable(Screen.Privacy.route) {
                PrivacyScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
