package com.softlabs.clipbq.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.softlabs.clipbq.ui.AuthScreen
import com.softlabs.clipbq.ui.ClipboardHistoryScreen
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

    var loggedInScreen by rememberSaveable { mutableStateOf(AppScreen.HISTORY) }
    var loggedOutScreen by rememberSaveable { mutableStateOf(AppScreen.AUTH) }
    var activeRecoveryToken by rememberSaveable { mutableStateOf<String?>(null) }
    val currentOnClearDeeplink by rememberUpdatedState(onClearDeeplink)

    LaunchedEffect(deeplinkToken, forcedScreen) {
        if (!deeplinkToken.isNullOrBlank() && forcedScreen == AppScreen.RESET_PASSWORD) {
            activeRecoveryToken = deeplinkToken
            currentOnClearDeeplink() // MainActivity can now clear safely without breaking this screen
        }
    }
    when {
        activeRecoveryToken != null -> {
            ResetPasswordScreen(
                viewModel = viewModel,
                isLinkValidated = true,
                accessToken = activeRecoveryToken.orEmpty(),
                onBackToLogin = {
                    activeRecoveryToken = null
                    loggedOutScreen = AppScreen.AUTH
                    loggedInScreen = AppScreen.HISTORY
                })
        }

        !isAuth -> {
            when (loggedOutScreen) {
                AppScreen.RESET_PASSWORD -> {
                    ResetPasswordScreen(
                        viewModel = viewModel,
                        accessToken = null,
                        isLinkValidated = false,
                        onBackToLogin = { loggedOutScreen = AppScreen.AUTH })
                }

                else -> {
                    AuthScreen(
                        viewModel = viewModel,
                        onNavigateToReset = { loggedOutScreen = AppScreen.RESET_PASSWORD })
                }
            }
        }

        else -> {
            SideEffect {
                if (loggedOutScreen != AppScreen.AUTH) {
                    loggedOutScreen = AppScreen.AUTH
                }
            }

            when (loggedInScreen) {
                AppScreen.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel, onBack = { loggedInScreen = AppScreen.HISTORY })
                }

                else -> {
                    ClipboardHistoryScreen(
                        viewModel = viewModel,
                        onNavigateToSettings = { loggedInScreen = AppScreen.SETTINGS })
                }
            }
        }
    }
}