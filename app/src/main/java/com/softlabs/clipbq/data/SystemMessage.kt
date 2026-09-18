package com.softlabs.clipbq.data

enum class MessageType { SUCCESS, ERROR, INFO }

data class SystemMessage(
    val text: String,
    val type: MessageType
)