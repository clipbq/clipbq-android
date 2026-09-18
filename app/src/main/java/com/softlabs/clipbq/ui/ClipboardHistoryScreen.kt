package com.softlabs.clipbq.ui

import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.softlabs.clipbq.viewmodel.ClipboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardHistoryScreen(viewModel: ClipboardViewModel, onNavigateToSettings: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val items by viewModel.clipboardHistory.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(items.size) {
        viewModel.refreshHistory()
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)
            .pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
            })
        }) {
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