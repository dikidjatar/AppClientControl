package com.xeg911.shared.data.model.usage

/**
 * Foreground usage of one package inside the collection window.
 */
data class AppUsageEntry(
    val packageName: String = "",
    val appName: String = "",
    val foregroundMs: Long = 0L,
    val lastTimeUsed: Long = 0L,
    val launchCount: Int = 0,
    /**
     * Lower cased app name
     */
    val sortKey: String = "",
)

data class UsageSummary(
    val windowStartAt: Long = 0L,
    val windowEndAt: Long = 0L,
    val capturedAt: Long = 0L,
    val totalForegroundMs: Long = 0L,
    val appCount: Int = 0,
    val topPackageName: String = "",
    val topAppName: String = "",
    val topForegroundMs: Long = 0L,
    val screenUnlockCount: Int = 0,
    val permissionGranted: Boolean = false,
)

data class UsageStatsSnapshot(
    val summary: UsageSummary = UsageSummary(),
    val apps: List<AppUsageEntry> = emptyList(),
)
