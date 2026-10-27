package com.softlabs.clipbq

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.softlabs.clipbq.data.SupabaseClientProvider
import com.softlabs.clipbq.screen.AppScreen
import com.softlabs.clipbq.screen.ClipboardAppNavigation
import com.softlabs.clipbq.viewmodel.ClipboardViewModel

private const val TAG = "clipBQ-Sync"

class MainActivity : ComponentActivity() {
    private var recoveryToken by mutableStateOf<String?>(null)
    private var targetScreen by mutableStateOf<AppScreen?>(null)
    private val viewModel: ClipboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SupabaseClientProvider.initialize()

        handleDeepLinkIntent(intent)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ClipboardAppNavigation(
                        viewModel = viewModel,
                        deeplinkToken = recoveryToken,
                        forcedScreen = targetScreen,
                        onClearDeeplink = {
                            targetScreen = null
                            recoveryToken = null
                        })
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
            val fragment = data.fragment.orEmpty()
            if (fragment.contains("access_token=")) {
                val token = fragment.substringAfter("access_token=").substringBefore("&")
                Log.d(TAG, "Received recovery token structure safely.")

                recoveryToken = token
                targetScreen = AppScreen.RESET_PASSWORD
            }
        }
    }
}
