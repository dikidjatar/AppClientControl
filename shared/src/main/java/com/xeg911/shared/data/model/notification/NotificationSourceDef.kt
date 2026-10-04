package com.xeg911.shared.data.model.notification

import kotlinx.serialization.Serializable

@Serializable
data class NotificationSourceDef(
    val id: String = "",
    val label: String = "",
    val enabled: Boolean = true,
    val headerEmoji: String = "🔔",
    val fromEmoji: String = "👤",
    val packages: List<String> = emptyList(),
    val usesDefaultSms: Boolean = false,
    val noisePatterns: List<String> = emptyList()
)