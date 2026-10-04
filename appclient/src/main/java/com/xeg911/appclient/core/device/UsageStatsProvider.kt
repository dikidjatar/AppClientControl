package com.xeg911.appclient.core.device

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import com.xeg911.appclient.core.device.UsageStatsProvider.Companion.KEY_UNLOCKS
import com.xeg911.shared.data.model.usage.AppUsageEntry
import com.xeg911.shared.data.model.usage.UsageStatsSnapshot
import com.xeg911.shared.data.model.usage.UsageSummary
import com.xeg911.shared.firebase.FirebasePaths.Usage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val usageStatsManager get() = context.getSystemService(UsageStatsManager::class.java)
    private val packageManager: PackageManager get() = context.packageManager

    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode =
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun collect(
        windowMs: Long = DEFAULT_WINDOW_MS,
        now: Long = System.currentTimeMillis()
    ): UsageStatsSnapshot {
        val start = now - windowMs
        if (!hasPermission()) {
            return UsageStatsSnapshot(
                UsageSummary(
                    windowStartAt = start,
                    windowEndAt = now,
                    capturedAt = now
                )
            )
        }
        val launches = launchCounts(start, now)
        val entries = usageStatsManager.queryAndAggregateUsageStats(start, now).values
            .filter { it.totalTimeInForeground > 0 }
            .map { stats ->
                AppUsageEntry(
                    packageName = stats.packageName,
                    appName = labelOf(stats.packageName),
                    foregroundMs = stats.totalTimeInForeground,
                    lastTimeUsed = stats.lastTimeUsed,
                    launchCount = launches[stats.packageName] ?: 0,
                )
            }
            .map { it.copy(sortKey = it.appName.ifBlank { it.packageName }.lowercase()) }
            .sortedByDescending { it.foregroundMs }
        val top = entries.firstOrNull()
        return UsageStatsSnapshot(
            summary = UsageSummary(
                windowStartAt = start,
                windowEndAt = now,
                capturedAt = now,
                totalForegroundMs = entries.sumOf { it.foregroundMs },
                appCount = entries.size,
                topPackageName = top?.packageName.orEmpty(),
                topAppName = top?.appName.orEmpty(),
                topForegroundMs = top?.foregroundMs ?: 0L,
                screenUnlockCount = launches[KEY_UNLOCKS] ?: 0,
                permissionGranted = true,
            ),
            apps = entries.take(Usage.MAX_APPS),
        )
    }

    /**
     * Foreground transitions per package plus device unlock count under [KEY_UNLOCKS].
     */
    private fun launchCounts(start: Long, end: Long): Map<String, Int> {
        val counts = HashMap<String, Int>()
        val events = runCatching {
            usageStatsManager.queryEvents(start, end)
        }.getOrNull() ?: return counts
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> counts.merge(event.packageName, 1, Int::plus)
                UsageEvents.Event.KEYGUARD_HIDDEN -> counts.merge(KEY_UNLOCKS, 1, Int::plus)
            }
        }
        return counts
    }

    private fun labelOf(packageName: String): String = runCatching {
        packageManager
            .getApplicationLabel(packageManager.getApplicationInfo(packageName, 0))
            .toString()
    }.getOrDefault("")

    companion object {
        const val DEFAULT_WINDOW_MS = 24 * 60 * 60_000L
        private const val KEY_UNLOCKS = "__unlocks"
    }
}
