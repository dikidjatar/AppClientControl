package com.xeg911.appcontrol.domain.model

import com.xeg911.shared.data.model.DeviceConfig

data class TelegramChannelConfig(
    val enabled: Boolean = false,
    val botToken: String = "",
    val chatId: String = "",
)

data class DeviceRemoteConfig(
    val webviewUrl: String = "",
    val telegram: Map<String, TelegramChannelConfig> = emptyMap(),
    val locationIntervalMs: Long = DeviceConfig.DEFAULT_LOCATION_INTERVAL_MS,
    val usageIntervalMs: Long = DeviceConfig.DEFAULT_USAGE_INTERVAL_MS,
    val requiredPermissions: List<String>? = null,
)

/**
 * Global defaults stored at /defaultConfig and inherited by every device.
 */
data class DefaultRemoteConfig(
    val webviewUrl: String = "",
    val locationIntervalMs: Long = DeviceConfig.DEFAULT_LOCATION_INTERVAL_MS,
    val usageIntervalMs: Long = DeviceConfig.DEFAULT_USAGE_INTERVAL_MS,
    val requiredPermissions: List<String> = emptyList(),
)
