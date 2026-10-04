package com.xeg911.shared.data.model

data class WallpaperSnapshot(
    val telegramFileId: String = "",
    val downloadUrl: String = "",
    val widthPx: Int = 0,
    val heightPx: Int = 0,
    val sizeBytes: Long = 0L,
    val capturedAt: Long = 0L
)