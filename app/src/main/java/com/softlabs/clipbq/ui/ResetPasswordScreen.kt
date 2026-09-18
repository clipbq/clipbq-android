package com.softlabs.clipbq.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.softlabs.clipbq.viewmodel.ClipboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    viewModel: ClipboardViewModel,
    accessToken: String?,
    onBackToLogin: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password") },
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (accessToken == null) {
                Text("Enter your email address to receive a secure recovery link.", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        viewModel.sendPasswordRecoveryLink(email,
                            onSuccess = { Log.d("clipBQ-Sync", "Successfully sent recovery link!") },
                            onError = { err -> Log.e("clipBQ-Sync", "Unable to send recovery link: ${err}", Exception(err)) }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SEND RECOVERY LINK")
                }
            } else {
                Text("Recovery Link Validated! Enter your new password below.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = newPassword, onValueChange = { newPassword = it }, label = { Text("New Secure Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        viewModel.verifyTokenAndResetPassword(accessToken!!, newPassword,
                            onSuccess = {
                                Log.d("clipBQ-Sync", "Successfully updated password!")
                                onBackToLogin()
                            },
                            onError = { err -> Log.e("clipBQ-Sync", "Unable to update password: ${err}", Exception(err)) }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CONFIRM NEW PASSWORD")
                }
            }
        }
    }
}