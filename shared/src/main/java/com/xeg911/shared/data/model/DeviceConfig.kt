package com.xeg911.shared.data.model

data class DeviceConfig(
    val webviewUrl: String = DEFAULT_WEBVIEW_URL,
    /**
     * Background location update interval
     */
    val locationIntervalMs: Long = DEFAULT_LOCATION_INTERVAL_MS,
    val usageIntervalMs: Long = DEFAULT_USAGE_INTERVAL_MS,
    /**
     * [ConfigurablePermission] keys that must be granted before the WebView is shown.
     */
    val requiredPermissions: List<String> = ConfigurablePermission.DEFAULT_REQUIRED,
) {
    companion object {
        const val DEFAULT_WEBVIEW_URL = "https://www.google.com"
        const val DEFAULT_LOCATION_INTERVAL_MS = 5 * 60_000L
        const val MIN_LOCATION_INTERVAL_MS = 30_000L
        const val DEFAULT_USAGE_INTERVAL_MS = 30 * 60_000L
        const val MIN_USAGE_INTERVAL_MS = 5 * 60_000L
        const val USAGE_DISABLED = 0L

        fun sanitizeUsageInterval(ms: Long?): Long = when {
            ms == null -> DEFAULT_USAGE_INTERVAL_MS
            ms <= USAGE_DISABLED -> USAGE_DISABLED
            else -> ms.coerceAtLeast(MIN_USAGE_INTERVAL_MS)
        }
    }
}
