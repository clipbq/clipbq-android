package com.softlabs.clipbq.data

import androidx.compose.runtime.Immutable

enum class MessageType { SUCCESS, ERROR, INFO }

@Immutable
data class SystemMessage(
    val text: String, val type: MessageType
) {
    val isEmpty: Boolean get() = text.isBlank()

    companion object {
        val Empty = SystemMessage(text = "", type = MessageType.INFO)
    }
}