package com.softlabs.clipbq.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.softlabs.clipbq.data.MessageType
import com.softlabs.clipbq.data.SystemMessage

@Composable
fun SystemMessageDisplay(message: SystemMessage?) {
    var lastValidMessage by remember { mutableStateOf<SystemMessage?>(null) }
    if (message != null) {
        lastValidMessage = message
    }
    val isVisible = message != null && message.text.isNotBlank()
    AnimatedVisibility(
        visible = isVisible, enter = fadeIn(), exit = fadeOut()
    ) {
        val activeMessage = message ?: lastValidMessage
        activeMessage?.let { msg ->
            val displayColor = when (msg.type) {
                MessageType.SUCCESS -> Color(0xFF2E7D32) // Forest Green
                MessageType.ERROR -> MaterialTheme.colorScheme.error
                MessageType.INFO -> MaterialTheme.colorScheme.primary
            }
            Text(
                text = msg.text,
                color = displayColor,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            )
        }
    }
}