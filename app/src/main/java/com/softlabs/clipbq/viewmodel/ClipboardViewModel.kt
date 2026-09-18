package com.softlabs.clipbq.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softlabs.clipbq.data.ClipboardItem
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
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime

class ClipboardViewModel : ViewModel() {
    private val client = SupabaseClientProvider.client

    private val _isUserAuthenticated = MutableStateFlow(client.auth.currentSessionOrNull() != null)
    val isUserAuthenticated: StateFlow<Boolean> = _isUserAuthenticated.asStateFlow()

    private val _clipboardHistory = MutableStateFlow<List<ClipboardItem>>(emptyList())
    val clipboardHistory: StateFlow<List<ClipboardItem>> = _clipboardHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        if (_isUserAuthenticated.value) {
            observeRealtimeDatabase()
        }
    }

    fun handleAuthAction(email: String, pass: String, isSignUp: Boolean, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (isSignUp) {
                    // Fix for line 48: The modern unified Sign Up syntax
                    client.auth.signUpWith(Email) {
                        this.email = email
                        this.password = pass
                    }
                    onError("Registration successful! You may log in.")
                } else {
                    // The modern unified Sign In syntax
                    client.auth.signInWith(Email) {
                        this.email = email
                        this.password = pass
                    }
                    _isUserAuthenticated.value = true
                    observeRealtimeDatabase()
                    onSuccess()
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Authentication failure.")
            } finally {
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
                // Retrieve your user's trailing clip entry to prevent duplications
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
                // If the upload still fails, this log statement will reveal the exact cause
                Log.e("clipBQ-Sync", "Unable to sync: ${e.localizedMessage}", e)
            }
        }
    }

    fun searchClipboard(query: String) {
        val currentUserId = client.auth.currentSessionOrNull()?.user?.id ?: return
        if (query.isEmpty()) {
            fetchFullHistory()
            return
        }
        viewModelScope.launch {
            try {
                val results = client.postgrest["clipboard_history"].select {
                    filter {
                        eq("user_id", currentUserId)
                        ilike("content", "%$query%")
                    }
                    order("created_at", Order.DESCENDING)
                }.decodeList<ClipboardItem>()
                _clipboardHistory.value = results
            } catch (_: Exception) {}
        }
    }

    private fun observeRealtimeDatabase() {
        val currentUserId = client.auth.currentSessionOrNull()?.user?.id ?: return
        fetchFullHistory()

        viewModelScope.launch {
            val channel = client.realtime.channel("public:clipboard_history")
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "clipboard_history"
            }

            channel.subscribe()

            changeFlow.collect {
                fetchFullHistory()
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
                _clipboardHistory.value = data
            } catch (_: Exception) {}
        }
    }

    fun writeToLocalClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)
    }

    fun refreshHistory() {
        fetchFullHistory()
    }

    fun deleteAllHistory(onComplete: () -> Unit) {
        val currentUserId = client.auth.currentSessionOrNull()?.user?.id ?: return
        viewModelScope.launch {
            try {
                client.postgrest["clipboard_history"].delete {
                    filter { eq("user_id", currentUserId) }
                }
                fetchFullHistory()
                onComplete()
            } catch (_: Exception) {}
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                client.auth.signOut()
                _isUserAuthenticated.value = false
                _clipboardHistory.value = emptyList()
                onComplete()
            } catch (_: Exception) {}
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
                onComplete()
            } catch (e: Exception) {
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
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to transmit login link.")
            }
        }
    }

    fun verifyTokenAndResetPassword(accessToken: String, newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                client.auth.retrieveUser(accessToken)
                client.auth.updateUser {
                    password = newPass
                }
                client.auth.signOut()
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Invalid verification link sequence.")
            }
        }
    }
}