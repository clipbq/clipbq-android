package com.softlabs.clipbq


import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.softlabs.clipbq.data.SupabaseClientProvider
import com.softlabs.clipbq.screen.AppScreen
import com.softlabs.clipbq.screen.ClipboardAppNavigation
import com.softlabs.clipbq.viewmodel.ClipboardViewModel
import kotlinx.coroutines.flow.MutableStateFlow


class MainActivity : ComponentActivity() {
    private var recoveryToken = mutableStateOf<String?>(null)
    private var targetScreen = mutableStateOf<AppScreen?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseClientProvider.initialize(applicationContext)
        handleDeepLinkIntent(intent)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: ClipboardViewModel = viewModel()
                    ClipboardAppNavigation(viewModel,
                        recoveryToken.value, targetScreen.value) {
                        recoveryToken.value = null
                        targetScreen.value = null
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "clipbq" && data.host == "reset-password") {
            val fragment = data.fragment ?: ""
            if (fragment.contains("access_token=")) {
                val token = fragment.substringAfter("access_token=").substringBefore("&")
                Log.d("clipBQ-Sync", "Received recovery token: $token")
                recoveryToken.value = token
                targetScreen.value = AppScreen.RESET_PASSWORD
            }
        }
    }
}

private fun <T> remember(calculation: () -> MutableStateFlow<T>): MutableStateFlow<T> = calculation()