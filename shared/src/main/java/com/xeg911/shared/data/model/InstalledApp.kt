package com.xeg911.shared.data.model

/**
 * Represents a single non system app installed on the device.
 */
data class InstalledApp(
    val packageName: String = "",
    val appName: String = "",
    val versionName: String = "",
    val versionCode: Long = 0L,
    val firstInstallTime: Long = 0L,
    val lastUpdateTime: Long = 0L,
    /**
     * Lower cased [appName]
     */
    val sortKey: String = ""
)
