package com.softlabs.clipbq.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.softlabs.clipbq.AuthScreen
import com.softlabs.clipbq.ClipboardHistoryScreen
import com.softlabs.clipbq.ui.ResetPasswordScreen
import com.softlabs.clipbq.ui.SettingsScreen
import com.softlabs.clipbq.viewmodel.ClipboardViewModel

enum class AppScreen { AUTH, HISTORY, SETTINGS, RESET_PASSWORD }
@Composable
fun ClipboardAppNavigation(
    viewModel: ClipboardViewModel,
    deeplinkToken: String?,
    forcedScreen: AppScreen?,
    onClearDeeplink: () -> Unit
) {
    val isAuth by viewModel.isUserAuthenticated.collectAsState()

    var loggedInScreen by remember { mutableStateOf(AppScreen.HISTORY) }
    var loggedOutScreen by remember { mutableStateOf(AppScreen.AUTH) }

    // Intercept deep link events natively
    LaunchedEffect(deeplinkToken, forcedScreen) {
        if (deeplinkToken != null && forcedScreen == AppScreen.RESET_PASSWORD) {
            loggedOutScreen = AppScreen.RESET_PASSWORD
            onClearDeeplink()
        }
    }

    if (!isAuth && loggedOutScreen == AppScreen.RESET_PASSWORD) {
        ResetPasswordScreen(
            viewModel = viewModel,
            accessToken = deeplinkToken,
            onBackToLogin = { loggedOutScreen = AppScreen.AUTH }
        )
    } else if (!isAuth) {
        AuthScreen(
            viewModel = viewModel,
            onNavigateToReset = { loggedOutScreen = AppScreen.RESET_PASSWORD }
        )
    } else {
        LaunchedEffect(Unit) { loggedOutScreen = AppScreen.AUTH }
        when (loggedInScreen) {
            AppScreen.SETTINGS -> SettingsScreen(viewModel, onBack = { loggedInScreen = AppScreen.HISTORY })
            else -> ClipboardHistoryScreen(viewModel, onNavigateToSettings = { loggedInScreen = AppScreen.SETTINGS })
        }
    }
}