package com.softlabs.clipbq.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClipboardItem(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("content") val content: String,
    @SerialName("created_at") val createdAt: String? = null
) {
    constructor(userId: String, content: String) : this(
        id = null, userId = userId, content = content, createdAt = null
    )
}