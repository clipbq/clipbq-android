package com.softlabs.clipbq.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softlabs.clipbq.viewmodel.ClipboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    viewModel: ClipboardViewModel,
    isLinkValidated: Boolean,
    accessToken: String?,
    onBackToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    val systemMessage by viewModel.systemMessage.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Reset Password") }, navigationIcon = {
                IconButton(onClick = onBackToLogin) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            })
        }) { paddingValues ->
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
                    text = "Enter your email address to receive a secure recovery link.",
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

                SystemMessageDisplay(message = systemMessage)

                Button(
                    onClick = {
                        viewModel.sendPasswordRecoveryLink(
                            email = email,
                            onSuccess = {},
                            onError = {})
                    }, enabled = email.isNotBlank(), modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SEND RECOVERY LINK")
                }
            } else {
                Text(
                    text = "Recovery link validated! Enter your new password below.",
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

                SystemMessageDisplay(message = systemMessage)

                Button(
                    onClick = {
                        accessToken?.let { token ->
                            viewModel.verifyTokenAndResetPassword(
                                accessToken = token,
                                newPass = newPassword,
                                onSuccess = onBackToLogin
                            )
                        }
                    },
                    enabled = accessToken != null && newPassword.length >= 6,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CONFIRM NEW PASSWORD")
                }
            }
        }
    }
}