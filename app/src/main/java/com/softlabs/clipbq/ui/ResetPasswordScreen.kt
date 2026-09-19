package com.softlabs.clipbq.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    isLinkValidated: Boolean,
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
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isLinkValidated) {
                Text(
                    "Enter your email address to receive a secure recovery link.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                val systemMessage by viewModel.systemMessage.collectAsState()
                SystemMessageDisplay(message = systemMessage)
                Button(
                    onClick = {
                        viewModel.sendPasswordRecoveryLink(
                            email,
                            onSuccess = {},
                            onError = {}
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SEND RECOVERY LINK")
                }
            } else {
                Text(
                    "Recovery link validated! Enter your new password below.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Secure Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                val systemMessage by viewModel.systemMessage.collectAsState()
                SystemMessageDisplay(message = systemMessage)
                Button(
                    onClick = {
                        viewModel.verifyTokenAndResetPassword(
                            accessToken = accessToken!!,
                            newPass = newPassword,
                            onSuccess = {
                                onBackToLogin()
                            })
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CONFIRM NEW PASSWORD")
                }
            }

        }
    }
}