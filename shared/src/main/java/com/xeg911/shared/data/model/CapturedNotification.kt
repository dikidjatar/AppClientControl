package com.xeg911.shared.data.model

data class CapturedNotification(
    val id: String = "",
    val deviceId: String = "",
    val source: String = "",
    val packageName: String = "",
    val title: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)
