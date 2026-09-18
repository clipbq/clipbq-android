package com.softlabs.clipbq.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.softlabs.clipbq.viewmodel.ClipboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ClipboardViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Account Actions", style = MaterialTheme.typography.titleMedium)

            // Standard Logout Execution Option
            Button(
                onClick = {
                    viewModel.logout {
                        Toast.makeText(context, "Logged out safely", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("LOG OUT")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()

            Text(text = "Danger Zone", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)

            // Hard delete profile activation button layout widget trigger
            Button(
                onClick = { showDeleteConfirmation = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("DELETE USER ACCOUNT PERMANENTLY")
            }
        }
    }

    // Modal Confirmation Dialog to safeguard against accidental clicks
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Account permanently?") },
            text = { Text("This will completely clear your user registration identity along with your matched clipboard cloud database syncing data across all devices. This choice cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteUserAccount(
                            onComplete = { Toast.makeText(context, "Account destroyed.", Toast.LENGTH_LONG).show() },
                            onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                        )
                    }
                ) {
                    Text("YES, CONFIRM DELETION", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}