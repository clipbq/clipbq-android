package com.softlabs.clipbq.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softlabs.clipbq.data.ClipboardItem
import com.softlabs.clipbq.data.MessageType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP


import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.softlabs.clipbq.data.SupabaseClientProvider
import com.softlabs.clipbq.data.SystemMessage
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class ClipboardViewModel : ViewModel() {
    private val client = SupabaseClientProvider.client

    private val _isUserAuthenticated = MutableStateFlow(client.auth.currentSessionOrNull() != null)
    val isUserAuthenticated: StateFlow<Boolean> = _isUserAuthenticated.asStateFlow()

    private val _clipboardHistory = MutableStateFlow<List<ClipboardItem>>(emptyList())
    val clipboardHistory: StateFlow<List<ClipboardItem>> = _clipboardHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _systemMessage = MutableStateFlow<SystemMessage?>(null)
    val systemMessage: StateFlow<SystemMessage?> = _systemMessage.asStateFlow()

    private val _masterHistory = MutableStateFlow<List<ClipboardItem>>(emptyList())
    private var currentSearchQuery = ""

    fun setSystemMessage(text: String, type: MessageType) {
        val newMessage = SystemMessage(text, type)
        _systemMessage.value = newMessage
        viewModelScope.launch {
            delay(5000.milliseconds)
            if (_systemMessage.value == newMessage) _systemMessage.value = null
        }
    }

    fun handleAuthAction(email: String, pass: String, isSignUp: Boolean, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _systemMessage.value = null
            try {
                if (isSignUp) {
                    client.auth.signUpWith(Email) {
                        this.email = email
                        this.password = pass
                    }
                    setSystemMessage("Registration successful! Check your inbox for confirmation.",
                        MessageType.SUCCESS)
                } else {
                    client.auth.signInWith(Email) {
                        this.email = email
                        this.password = pass
                    }
                    _isUserAuthenticated.value = true
                    setSystemMessage("Login successful!", MessageType.SUCCESS)
                    onSuccess()
                }
            } catch (e: Exception) {
                if(e.localizedMessage!!.contains("Unable to resolve host")) {
                    setSystemMessage("Please check your Internet connection and try again.",
                        MessageType.INFO)
                } else {
                setSystemMessage(e.localizedMessage ?: "Authentication failure.", MessageType.ERROR)
                onError(e.localizedMessage ?: "Authentication failure.")
            }} finally {
                _isLoading.value = false
            }
        }
    }

    fun captureAndSyncLocalClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val textClip = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: return
        if (textClip.trim().isEmpty()) return

        val currentUserId = client.auth.currentSessionOrNull()?.user?.id ?: return

        viewModelScope.launch {
            try {
                val history = client.postgrest["clipboard_history"].select {
                    filter { eq("user_id", currentUserId) }
                    order("created_at", Order.DESCENDING)
                    limit(1)
                }.decodeList<ClipboardItem>()

                if (history.isEmpty() || history.first().content != textClip) {
                    val newItem = ClipboardItem(
                        userId = currentUserId,
                        content = textClip
                    )
                    client.postgrest["clipboard_history"].insert(newItem)
                    fetchFullHistory()
                    Log.d("clipBQ-Sync", "Successfully synced new clip to cloud!")
                }
            } catch (e: Exception) {
                Log.e("clipBQ-Sync", "Unable to sync: ${e.localizedMessage}", e)
            }
        }
    }

    fun searchClipboard(query: String) {
        currentSearchQuery = query
        if (query.isBlank()) {
            _clipboardHistory.value = _masterHistory.value
        } else {
            _clipboardHistory.value = _masterHistory.value.filter { item ->
                item.content.contains(query, ignoreCase = true)
            }
        }
    }

    private fun fetchFullHistory() {
        val currentUserId = client.auth.currentSessionOrNull()?.user?.id ?: return
        viewModelScope.launch {
            try {
                val data = client.postgrest["clipboard_history"].select {
                    filter { eq("user_id", currentUserId) }
                    order("created_at", Order.DESCENDING)
                }.decodeList<ClipboardItem>()
                _masterHistory.value = data
                if (currentSearchQuery.isEmpty()) {
                    _clipboardHistory.value = data
                } else {
                    searchClipboard(currentSearchQuery)
                }
                Log.d("clipBQ-Sync", "Successfully fetched full history!")
            } catch (e: Exception) {Log.e("clipBQ-Sync", "Unable to sync: ${e.localizedMessage}")}
        }
    }

    fun writeToLocalClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)
    }

    fun refreshHistory() {
        if (currentSearchQuery.isEmpty()) {
            fetchFullHistory()
        }
    }

    fun deleteAllHistory(onComplete: () -> Unit) {
        val currentUserId = client.auth.currentSessionOrNull()?.user?.id ?: return
        viewModelScope.launch {
            try {
                client.postgrest["clipboard_history"].delete {
                    filter { eq("user_id", currentUserId) }
                }
                fetchFullHistory()
                Log.d("clipBQ-Sync", "Successfully cleared history and synced to cloud!")
                onComplete()
            } catch (e: Exception) {
                Log.e("clipBQ-Sync", "Unable to sync: ${e.localizedMessage}")
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                client.auth.signOut()
                _isUserAuthenticated.value = false
                _clipboardHistory.value = emptyList()
                Log.d("clipBQ-Sync", "Successfully logged out safely")
                onComplete()
            } catch (e: Exception) {
                Log.e("clipBQ-Sync", "Unable to log out safely: ${e.localizedMessage}")
            }
        }
    }

    fun deleteUserAccount(onComplete: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val currentUserId = client.auth.currentSessionOrNull()?.user?.id
                if (currentUserId != null) {
                    client.postgrest["clipboard_history"].delete {
                        filter { eq("user_id", currentUserId) }
                    }
                }
                client.postgrest.rpc("delete_authenticated_user")
                client.auth.signOut()
                _isUserAuthenticated.value = false
                _clipboardHistory.value = emptyList()
                Log.d("clipBQ-Sync", "Successfully deleted user account and synced to cloud!")
                onComplete()
            } catch (e: Exception) {
                Log.e("clipBQ-Sync", "Could not complete account deletion: ${e.localizedMessage}", e)
                onError(e.localizedMessage ?: "Could not complete account deletion.")
            }
        }
    }

    fun sendPasswordRecoveryLink(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                client.auth.signUpWith(OTP) {
                    this.email = email
                    this.createUser = false
                }
                Log.d("clipBQ-Sync", "Successfully sent recovery link!")
                setSystemMessage("Recovery link sent! Check your inbox.", MessageType.SUCCESS)
                onSuccess()
            } catch (e: Exception) {
                setSystemMessage(
                    e.localizedMessage ?: "Failed to transmit login link.",
                    MessageType.ERROR
                )
                Log.e("clipBQ-Sync", "Failed to transmit login link: ${e.localizedMessage}", e)
                onError(e.localizedMessage ?: "Failed to transmit login link.")
            }

        }
    }

    fun verifyTokenAndResetPassword(accessToken: String, newPass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                client.auth.importAuthToken(
                    accessToken = accessToken,
                    retrieveUser = true
                )
                client.auth.updateUser {
                    password = newPass
                }
                client.auth.signOut()
                Log.d("clipBQ-Sync", "Successfully updated password!")
                setSystemMessage("Password updated! Please log in with your new credentials.",
                    MessageType.SUCCESS)
                onSuccess()
            } catch (e: Exception) {
                if(e.localizedMessage!!.contains("Unable to resolve host")) {
                    setSystemMessage("Please check your Internet connection and try again.",
                        MessageType.INFO)
                } else {
                    setSystemMessage(e.localizedMessage ?: "Invalid verification link sequence.",
                        MessageType.ERROR)
                    Log.e("clipBQ-Sync",
                        "Invalid verification link sequence: ${e.localizedMessage}", e)
                }
            }
        }
    }
}