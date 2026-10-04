package com.xeg911.appcontrol.ui.navigation

import android.net.Uri
import androidx.annotation.StringRes
import com.xeg911.appcontrol.R

object Screen {
    const val ARG_DEVICE_ID = "deviceId"
    const val ARG_NOTIFICATION_ID = "notificationId"
    const val ARG_PACKAGE_NAME = "packageName"
    const val ARG_APP_NAME = "appName"
    const val ARG_PERMISSION = "permission"
    const val ARG_TEMPLATE_ID = "templateId"

    const val AUTH = "auth"
    const val HOME = "home"
    const val DEVICE_LIST = "deviceList"
    const val ALL_TOOLS = "allTools"
    const val SETTINGS = "settings"
    const val DEFAULT_CONFIG = "defaultConfig"
    const val REQUIRED_PERMISSIONS = "requiredPermissions"
    const val TRANSFER_HISTORY = "transferHistory"
    const val TEMPLATES = "templates"
    const val RULES = "rules"

    const val NOTIFICATION_FILTERS = "notificationFilters"

    const val FILES = "files"
    const val FILES_ROUTE = "$FILES?$ARG_DEVICE_ID={$ARG_DEVICE_ID}"
    fun filesRoute(deviceId: String = "") = "$FILES?$ARG_DEVICE_ID=$deviceId"

    const val DEVICE_HUB = "deviceHub"
    const val DEVICE_HUB_ROUTE = "$DEVICE_HUB?$ARG_DEVICE_ID={$ARG_DEVICE_ID}"
    fun deviceHubRoute(deviceId: String) = "$DEVICE_HUB?$ARG_DEVICE_ID=$deviceId"

    const val COMPOSER = "composer"
    const val COMPOSER_ROUTE = "$COMPOSER?$ARG_DEVICE_ID={$ARG_DEVICE_ID}" +
            "&$ARG_NOTIFICATION_ID={$ARG_NOTIFICATION_ID}" +
            "&$ARG_PACKAGE_NAME={$ARG_PACKAGE_NAME}" +
            "&$ARG_APP_NAME={$ARG_APP_NAME}" +
            "&$ARG_PERMISSION={$ARG_PERMISSION}" +
            "&$ARG_TEMPLATE_ID={$ARG_TEMPLATE_ID}"

    fun composerRoute(
        deviceId: String,
        notificationId: String? = null,
        packageName: String? = null,
        appName: String? = null,
        permission: String? = null,
        templateId: String? = null,
    ): String = buildString {
        append("$COMPOSER?$ARG_DEVICE_ID=$deviceId")
        if (!notificationId.isNullOrBlank()) append("&$ARG_NOTIFICATION_ID=$notificationId")
        if (!packageName.isNullOrBlank()) append("&$ARG_PACKAGE_NAME=$packageName")
        if (!appName.isNullOrBlank()) append("&$ARG_APP_NAME=${Uri.encode(appName)}")
        if (!permission.isNullOrBlank()) append("&$ARG_PERMISSION=${Uri.encode(permission)}")
        if (!templateId.isNullOrBlank()) append("&$ARG_TEMPLATE_ID=${Uri.encode(templateId)}")
    }
}

enum class DeviceHubTab(@param:StringRes val labelRes: Int) {
    OVERVIEW(R.string.tab_overview),
    LOCATION(R.string.tab_location),
    NOTIFICATIONS(R.string.tab_notifications),
    OUTBOX(R.string.tab_outbox),
    CALLBACKS(R.string.tab_callbacks),
    APPLICATIONS(R.string.tab_apps),
    CONTACTS(R.string.tab_contacts),
    USAGE(R.string.tab_usage),
    PERMISSIONS(R.string.tab_permissions),
    HARDWARE(R.string.tab_hardware),
    CAPABILITIES(R.string.tab_capabilities),
    CONFIG(R.string.tab_config),
}
