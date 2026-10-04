package com.xeg911.appcontrol.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import com.xeg911.appcontrol.R
import kotlin.math.log10
import kotlin.math.pow

@Composable
fun formatLastSeen(timestamp: Long): String {
    if (timestamp <= 0L) return stringResource(R.string.status_never_seen)

    val diffMs = System.currentTimeMillis() - timestamp
    val diffSec = diffMs / 1_000
    val diffMin = diffSec / 60
    val diffHour = diffMin / 60
    val diffDay = diffHour / 24

    return when {
        diffSec < 60 -> stringResource(R.string.status_just_now)
        diffMin < 60 -> stringResource(R.string.status_minutes_ago, diffMin)
        diffHour < 24 -> stringResource(R.string.status_hours_ago, diffHour)
        else -> stringResource(R.string.status_days_ago, diffDay)
    }
}

/**
 * Formats bytes to a human-readable string, e.g. "3.5 GB".
 */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        .coerceAtMost(units.lastIndex)
    val value = bytes / 1024.0.pow(digitGroups.toDouble())
    return "%.1f %s".format(value, units[digitGroups])
}

@Composable
fun formatRelativeTime(timestamp: Long): String {
    if (timestamp <= 0L) return "—"
    val diffMs = System.currentTimeMillis() - timestamp
    val diffSec = diffMs / 1_000
    val diffMin = diffSec / 60
    val diffHour = diffMin / 60
    val diffDay = diffHour / 24

    return when {
        diffSec < 60 -> stringResource(R.string.status_just_now)
        diffMin < 60 -> stringResource(R.string.status_minutes_ago, diffMin)
        diffHour < 24 -> stringResource(R.string.status_hours_ago, diffHour)
        diffDay < 7 -> stringResource(R.string.status_days_ago, diffDay)
        else -> {
            val sdf = java.text.SimpleDateFormat("dd MMM", LocalLocale.current.platformLocale)
            sdf.format(java.util.Date(timestamp))
        }
    }
}

/**
 * Absolute timestamp, e.g. "24 Jun 2026, 14:05:32". Returns "—" for 0.
 */
@Composable
fun formatDateTime(timestamp: Long): String {
    if (timestamp <= 0L) return "—"
    val sdf = java.text.SimpleDateFormat(
        "dd MMM yyyy, HH:mm:ss",
        LocalLocale.current.platformLocale
    )
    return sdf.format(java.util.Date(timestamp))
}

/**
 * Relative time followed by the absolute timestamp, e.g. "3 min ago · 24 Jun 2026, 14:05:32".
 */
@Composable
fun formatRelativeWithAbsolute(timestamp: Long): String {
    if (timestamp <= 0L) return stringResource(R.string.status_never_seen)
    return "${formatRelativeTime(timestamp)} · ${formatDateTime(timestamp)}"
}

/**
 * Compact duration, e.g. "2h 05m", "45s".
 */
fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "0s"
    val totalSec = durationMs / 1_000
    val hours = totalSec / 3_600
    val minutes = (totalSec % 3_600) / 60
    val seconds = totalSec % 60
    return when {
        hours > 0 -> "%dh %02dm".format(hours, minutes)
        minutes > 0 -> "%dm %02ds".format(minutes, seconds)
        else -> "${seconds}s"
    }
}
