package com.xeg911.shared.data.model

data class RamInfo(
    val totalBytes: Long = 0L,
    val availableBytes: Long = 0L,
    val lowMemory: Boolean = false,
    val lowMemoryThresholdBytes: Long = 0L
)

data class StorageInfo(
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L
)

data class DisplayInfo(
    val widthPx: Int = 0,
    val heightPx: Int = 0,
    val densityDpi: Int = 0,
    val refreshRate: Float = 0f,
    val fontScaleFactor: Float = 1f
)

data class HardwareInfo(
    val ram: RamInfo = RamInfo(),
    val internalStorage: StorageInfo = StorageInfo(),
    val externalStorage: StorageInfo? = null,
    val display: DisplayInfo = DisplayInfo(),
    val processorAbi: String = "",
    val cpuCoreCount: Int = 0,
    val locale: String = "",
    val timezone: String = "",
    val rooted: Boolean = false
)
