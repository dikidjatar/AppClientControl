package com.xeg911.appclient.data.remote.storage

data class TelegramUploadResult(
    val fileId: String,
    val downloadUrl: String,
    val fileSize: Long = -1L,
)
