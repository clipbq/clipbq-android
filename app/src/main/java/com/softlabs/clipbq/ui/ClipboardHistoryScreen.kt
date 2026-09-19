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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softlabs.clipbq.viewmodel.ClipboardViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "clipBQ-Sync"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardHistoryScreen(
    viewModel: ClipboardViewModel, onNavigateToSettings: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val items by viewModel.clipboardHistory.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(items) {
        if (items.isNotEmpty()) {
            val isNotAtTop =
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
            if (isNotAtTop) {
                listState.animateScrollToItem(index = 0, scrollOffset = 0)
            }
        } else {
            viewModel.refreshHistory()
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            delay(300.milliseconds)
        }
        viewModel.searchClipboard(searchQuery)
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("📋 clipBQ Cloud") }, actions = {
            IconButton(onClick = { viewModel.refreshHistory() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh List")
            }
            IconButton(onClick = {
                viewModel.deleteAllHistory {
                    Log.d(TAG, "Successfully cleared history!")
                }
            }) {
                Icon(
                    Icons.Default.DeleteSweep,
                    contentDescription = "Clear All History",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Open Settings")
            }
        })
    }, floatingActionButton = {
        FloatingActionButton(onClick = {
            viewModel.captureAndSyncLocalClipboard(context)
        }) {
            Icon(Icons.Default.CloudUpload, contentDescription = "Sync Current Clip")
        }
    }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it }, // Triggers the debounced LaunchedEffect
                label = { Text("Search clipboard history...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )

            LazyColumn(
                state = listState, modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {

                items(
                    items = items, key = { item -> item.id!! }) { item ->
                    ClipboardItemCard(
                        content = item.content,
                        modifier = Modifier.animateItem(),
                        onCopyClicked = { viewModel.writeToLocalClipboard(context, item.content) })
                }
            }
        }
    }
}

@Composable
private fun ClipboardItemCard(
    content: String, onCopyClicked: () -> Unit, modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = content, style = MaterialTheme.typography.bodyLarge)
            }
            IconButton(
                onClick = onCopyClicked, modifier = Modifier.align(Alignment.Top)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy Content",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}