package com.softlabs.clipbq.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.softlabs.clipbq.data.MessageType
import com.softlabs.clipbq.data.SystemMessage

@Composable
fun SystemMessageDisplay(message: SystemMessage?) {
    AnimatedVisibility(visible = message != null) {
        message?.let {
            val displayColor = when (it.type) {
                MessageType.SUCCESS -> Color(0xFF2E7D32)
                MessageType.ERROR -> MaterialTheme.colorScheme.error
                MessageType.INFO -> MaterialTheme.colorScheme.primary
            }

            Text(
                text = it.text,
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