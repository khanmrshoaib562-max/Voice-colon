package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object Alarm : Screen("alarm")
    data object TestVoice : Screen("test_voice")
    data object PinLock : Screen("pin_lock")
    data object Privacy : Screen("privacy")
}
