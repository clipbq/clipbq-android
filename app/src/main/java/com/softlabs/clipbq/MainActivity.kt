package com.softlabs.clipbq

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.softlabs.clipbq.viewmodel.ClipboardViewModel
import kotlinx.coroutines.flow.MutableStateFlow

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.softlabs.clipbq.data.SupabaseClientProvider
import com.softlabs.clipbq.screen.AppScreen
import com.softlabs.clipbq.screen.ClipboardAppNavigation


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
                val token = fragment.split("access_token=")[1].split("&")[0]
                Log.d("clipBQ-Sync", "Received recovery token: $token")
                recoveryToken.value = token
                targetScreen.value = AppScreen.RESET_PASSWORD
            }
        }
    }
}

@Composable
fun AuthScreen(viewModel: ClipboardViewModel, onNavigateToReset: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = if (isSignUp) "Create Account" else "Welcome Back", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(24.dp))

        if (!isSignUp) {

            TextButton(
                onClick = onNavigateToReset,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Forgot Password?")
            }
        }
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    viewModel.handleAuthAction(email, password, isSignUp, onSuccess = {
                        Log.d("clipBQ-Sync", "Successfully authenticated user to cloud!")
                    },
                        onError = { errorMessage ->
                            Log.e("clipBQ-Sync", "Unable to sync: ${errorMessage}",
                                Exception(errorMessage))
                        })
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSignUp) "REGISTER" else "LOG IN")
            }
            TextButton(onClick = { isSignUp = !isSignUp }) {
                Text(if (isSignUp) "Already have an account? Sign In" else "Need an account? Sign Up")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardHistoryScreen(viewModel: ClipboardViewModel, onNavigateToSettings: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val items by viewModel.clipboardHistory.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(items.size) {
        if (items.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = { TopAppBar(
            title = { Text("📋 clipBQ Cloud") },
            actions = {
            IconButton(onClick = { viewModel.refreshHistory() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh List")
            }
            IconButton(onClick = {
                viewModel.deleteAllHistory {
                    Log.d("clipBQ-Sync", "Successfully cleared history and synced to cloud!")
                }
            }) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All History", tint = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Open Settings")
            }
        }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.captureAndSyncLocalClipboard(context)
            }) {
                Icon(Icons.Default.CloudUpload, contentDescription = "Sync Current Clip")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchClipboard(it)
                },
                label = { Text("Search clipboard history...") },
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                items(items) { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.content, style = MaterialTheme.typography.bodyLarge)
                            }
                            IconButton(onClick = {
                                viewModel.writeToLocalClipboard(context, item.content)
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Content", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun <T> remember(calculation: () -> MutableStateFlow<T>): MutableStateFlow<T> = calculation()