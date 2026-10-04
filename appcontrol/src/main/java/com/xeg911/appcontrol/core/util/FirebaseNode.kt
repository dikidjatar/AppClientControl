package com.xeg911.appcontrol.core.util

import com.xeg911.shared.firebase.FirebasePaths

object FirebaseNode {
    const val NODE_DEVICES = FirebasePaths.DEVICES
    const val NODE_DEVICE_INDEX = FirebasePaths.DEVICE_INDEX
    const val NODE_AUTOMATION_RULES = FirebasePaths.AUTOMATION_RULES
    const val NODE_DEFAULT_CONFIG = FirebasePaths.DEFAULT_CONFIG
    const val NODE_NOTIFICATION_FILTER_CONFIG = FirebasePaths.NOTIFICATION_FILTER_CONFIG
    const val NODE_NOTIFICATION_TEMPLATES = FirebasePaths.NOTIFICATION_TEMPLATES
    const val NODE_NOTIFICATION_TEMPLATES_META = FirebasePaths.NOTIFICATION_TEMPLATES_META
}

object DeviceNode {
    const val NODE_INFO = FirebasePaths.Device.INFO
    const val NODE_STATUS = FirebasePaths.Device.STATUS
    const val NODE_NOTIFICATIONS = FirebasePaths.Device.NOTIFICATIONS
    const val NODE_APPS = FirebasePaths.Device.APPS
    const val NODE_CONTACTS = FirebasePaths.Device.CONTACTS
    const val NODE_PERMISSIONS = FirebasePaths.Device.PERMISSIONS
    const val NODE_BATTERY = FirebasePaths.Device.BATTERY
    const val NODE_HARDWARE = FirebasePaths.Device.HARDWARE
    const val NODE_WALLPAPER = FirebasePaths.Device.WALLPAPER
    const val NODE_CONNECTIVITY = FirebasePaths.Device.CONNECTIVITY
    const val NODE_EVENTS = FirebasePaths.Device.EVENTS
    const val NODE_CONFIG = FirebasePaths.Device.CONFIG
    const val NODE_CAPABILITIES = FirebasePaths.Device.CAPABILITIES
    const val NODE_NOTIFICATION_CAPABILITY = FirebasePaths.Capabilities.NOTIFICATION
    const val NODE_OUTBOX = FirebasePaths.Device.OUTBOX
    const val NODE_LOCATION = FirebasePaths.Device.LOCATION
    const val NODE_USAGE = FirebasePaths.Device.USAGE
    const val NODE_RULE_STATE = FirebasePaths.Device.RULE_STATE
}

object UsageField {
    const val NODE_SUMMARY = FirebasePaths.Usage.SUMMARY
    const val NODE_APPS = FirebasePaths.Usage.APPS
    const val FIELD_FOREGROUND_MS = "foregroundMs"
}

object EventField {
    const val FIELD_TYPE = FirebasePaths.Event.TYPE
    const val FIELD_NOTIFICATION_ID = FirebasePaths.Event.NOTIFICATION_ID
}

object IndexField {
    const val FIELD_FCM_TOKEN = FirebasePaths.Index.FCM_TOKEN
    const val FIELD_MISSING_REQUIRED = FirebasePaths.Index.MISSING_REQUIRED
}

object LocationField {
    const val NODE_LATEST = FirebasePaths.Location.LATEST
    const val NODE_STATUS = FirebasePaths.Location.STATUS
    const val NODE_HISTORY = FirebasePaths.Location.HISTORY
    const val FIELD_REQUEST = FirebasePaths.Location.REQUEST
    const val MAX_HISTORY = FirebasePaths.Location.MAX_HISTORY
}

object StatusField {
    const val FIELD_ONLINE = FirebasePaths.Status.ONLINE
    const val FIELD_LAST_SEEN = FirebasePaths.Status.LAST_SEEN
    const val FIELD_APP_IN_FOREGROUND = FirebasePaths.Status.APP_IN_FOREGROUND
    const val FIELD_MONITORING_RUNNING = FirebasePaths.Status.MONITORING_RUNNING
    const val FIELD_LAST_MONITORING_HEARTBEAT_AT = FirebasePaths.Status.LAST_MONITORING_HEARTBEAT_AT
}

object NotificationFilterField {
    const val NODE_SOURCES = FirebasePaths.NotificationFilter.SOURCES
    const val FIELD_LABEL = FirebasePaths.NotificationFilter.LABEL
    const val FIELD_ENABLED = FirebasePaths.NotificationFilter.ENABLED
    const val FIELD_HEADER_EMOJI = FirebasePaths.NotificationFilter.HEADER_EMOJI
    const val FIELD_FROM_EMOJI = FirebasePaths.NotificationFilter.FROM_EMOJI
    const val FIELD_PACKAGES = FirebasePaths.NotificationFilter.PACKAGES
    const val FIELD_USES_DEFAULT_SMS = FirebasePaths.NotificationFilter.USES_DEFAULT_SMS
    const val FIELD_NOISE_PATTERNS = FirebasePaths.NotificationFilter.NOISE_PATTERNS
}

object PermissionsField {
    const val NODE_ITEMS = FirebasePaths.Permissions.ITEMS
}

object SortField {
    const val SORT_KEY = FirebasePaths.Sort.SORT_KEY
    const val TIMESTAMP = FirebasePaths.Sort.TIMESTAMP
}

object ConnectivityField {
    const val FIELD_TRANSPORT = FirebasePaths.Connectivity.TRANSPORT
    const val FIELD_IP = FirebasePaths.Connectivity.IP
    const val FIELD_LAST_UPDATED = FirebasePaths.Connectivity.LAST_UPDATED
    const val FIELD_PING_REQUEST = FirebasePaths.Connectivity.PING_REQUEST
    const val FIELD_PING_RESPONSE = FirebasePaths.Connectivity.PING_RESPONSE
    const val FIELD_PING_LATENCY_MS = FirebasePaths.Connectivity.PING_LATENCY_MS
}

object DeviceConfigField {
    const val FIELD_WEBVIEW_URL = FirebasePaths.Config.WEBVIEW_URL
    const val FIELD_LOCATION_INTERVAL_MS = FirebasePaths.Config.LOCATION_INTERVAL_MS
    const val FIELD_USAGE_INTERVAL_MS = FirebasePaths.Config.USAGE_INTERVAL_MS
    const val FIELD_REQUIRED_PERMISSIONS = FirebasePaths.Config.REQUIRED_PERMISSIONS
    const val NODE_TELEGRAM = FirebasePaths.Config.TELEGRAM
}

object TelegramConfigField {
    const val PURPOSE_NOTIFICATION = "notification"
    const val PURPOSE_STORAGE = "storage"
    val PURPOSES = listOf(PURPOSE_NOTIFICATION, PURPOSE_STORAGE)

    const val FIELD_ENABLED = FirebasePaths.Config.TELEGRAM_ENABLED
    const val FIELD_BOT_TOKEN = FirebasePaths.Config.TELEGRAM_BOT_TOKEN
    const val FIELD_CHAT_ID = FirebasePaths.Config.TELEGRAM_CHAT_ID
}
