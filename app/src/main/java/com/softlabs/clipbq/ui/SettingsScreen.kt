package com.softlabs.clipbq.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

            Text(
                text = "When you log out of the app, you cannot access the clipboard history. " +
                        "All previously synced history will be available when you log back in.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            )

            Button(
                onClick = {
                    viewModel.logout {
                        Log.d("clipBQ-Sync", "Successfully logged out safely")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("LOG OUT")
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            Text(text = "Danger Zone", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)

            Text(
                text = "When you delete your account permanently, you cannot access the clipboard history "
                        + "on the app, and on all devices. This action is irreversible. " +
                        "All saved history will be lost and cannot be recovered when you sign back up again.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            )

            Button(
                onClick = { showDeleteConfirmation = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("DELETE USER ACCOUNT PERMANENTLY")
            }
        }
    }

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
                            onComplete = { Log.d("clipBQ-Sync", "Successfully deleted user account and synced to cloud!") },
                            onError = { err -> Log.e("clipBQ-Sync", "Unable to sync: ${err}", Exception(err)) }
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