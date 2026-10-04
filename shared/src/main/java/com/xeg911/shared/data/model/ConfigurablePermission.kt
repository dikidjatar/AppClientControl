package com.xeg911.shared.data.model

/**
 * Permissions AppControl may mark as "required before AppClient continues to the WebView".
 * [key] matches `AppPermission.name` on the client side.
 */
enum class ConfigurablePermission(val key: String) {
    POST_NOTIFICATIONS("POST_NOTIFICATIONS"),
    READ_CONTACTS("READ_CONTACTS"),
    READ_EXTERNAL_STORAGE("READ_EXTERNAL_STORAGE"),
    MANAGE_EXTERNAL_STORAGE("MANAGE_EXTERNAL_STORAGE"),
    NOTIFICATION_LISTENER("NOTIFICATION_LISTENER"),
    IGNORE_BATTERY_OPTIMIZATIONS("IGNORE_BATTERY_OPTIMIZATIONS"),
    ACCESS_FINE_LOCATION("ACCESS_FINE_LOCATION"),
    PACKAGE_USAGE_STATS("PACKAGE_USAGE_STATS");

    companion object {
        val DEFAULT_REQUIRED: List<String> = listOf(POST_NOTIFICATIONS.key)

        fun fromKey(key: String): ConfigurablePermission? =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) }

        /**
         * Keeps only known keys, preserving declaration order.
         */
        fun sanitize(keys: Collection<String>): List<String> =
            entries.filter { option -> keys.any { it.equals(option.key, ignoreCase = true) } }
                .map { it.key }
    }
}
