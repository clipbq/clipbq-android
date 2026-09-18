package com.softlabs.clipbq

import android.os.Bundle
import android.widget.Toast
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
import com.softlabs.clipbq.ui.SettingsScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseClientProvider.initialize(applicationContext)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ClipboardAppNavigation()
                }
            }
        }
    }
}

enum class AppScreen { AUTH, HISTORY, SETTINGS }
@Composable
fun ClipboardAppNavigation(viewModel: ClipboardViewModel = viewModel()) {
    val isAuth by viewModel.isUserAuthenticated.collectAsState()
    var currentScreen by remember { mutableStateOf(AppScreen.HISTORY) }

    when (if (isAuth) currentScreen else AppScreen.AUTH) {
        AppScreen.AUTH -> AuthScreen(viewModel)
        AppScreen.HISTORY -> ClipboardHistoryScreen(
            viewModel = viewModel,
            onNavigateToSettings = { currentScreen = AppScreen.SETTINGS }
        )
        AppScreen.SETTINGS -> SettingsScreen(
            viewModel = viewModel,
            onBack = { currentScreen = AppScreen.HISTORY }
        )
    }
}

@Composable
fun AuthScreen(viewModel: ClipboardViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

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

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    viewModel.handleAuthAction(email, password, isSignUp, onSuccess = {
                        Toast.makeText(context, "Authentication successful!", Toast.LENGTH_SHORT).show()
                    },
                        onError = { errorMessage ->
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
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
            title = { Text("📋 Clipboard Sync History") },
            actions = {
            // Refresh action icon button
            IconButton(onClick = { viewModel.refreshHistory() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh List")
            }
            // Delete all clipboard records action button
            IconButton(onClick = {
                viewModel.deleteAllHistory {
                    Toast.makeText(context, "History cleared!", Toast.LENGTH_SHORT).show()
                }
            }) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All History", tint = MaterialTheme.colorScheme.error)
            }
            // Navigation gear shortcut to settings panel view
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