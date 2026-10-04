package com.xeg911.appcontrol.data.model

data class OutboxEntity(
    val notificationId: String = "",
    val deviceId: String = "",
    val payloadJson: String = "",
    val status: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val sendCount: Int = 0,
    val lastError: String? = null,
)