package com.xeg911.shared.firebase

/**
 * Single source of truth for the Realtime Database schema.
 */
object FirebasePaths {
    const val DEVICES = "devices"

    const val DEVICE_INDEX = "deviceIndex"
    const val AUTOMATION_RULES = "automationRules"
    const val DEFAULT_CONFIG = "defaultConfig"
    const val NOTIFICATION_FILTER_CONFIG = "notificationFilterConfig"
    const val NOTIFICATION_TEMPLATES = "notificationTemplates"
    const val NOTIFICATION_TEMPLATES_META = "notificationTemplatesMeta"
    const val INFO_CONNECTED = ".info/connected"

    object Device {
        const val INFO = "info"
        const val STATUS = "status"
        const val CONNECTIVITY = "connectivity"
        const val BATTERY = "battery"
        const val HARDWARE = "hardware"
        const val WALLPAPER = "wallpaper"
        const val APPS = "apps"
        const val CONTACTS = "contacts"
        const val PERMISSIONS = "permissions"
        const val CAPABILITIES = "capabilities"
        const val EVENTS = "events"
        const val NOTIFICATIONS = "notifications"
        const val CONFIG = "config"
        const val OUTBOX = "outbox"
        const val LOCATION = "location"
        const val USAGE = "usage"
        const val RULE_STATE = "ruleState"
    }

    object Index {
        const val FCM_TOKEN = "fcmToken"
        const val MISSING_REQUIRED = "missingRequired"
    }

    object Location {
        const val LATEST = "latest"
        const val STATUS = "status"
        const val HISTORY = "history"
        const val REQUEST = "request"
        const val MAX_HISTORY = 50
    }

    object Usage {
        const val SUMMARY = "summary"
        const val APPS = "apps"
        const val MAX_APPS = 60
    }

    object Status {
        const val ONLINE = "online"
        const val APP_IN_FOREGROUND = "appInForeground"
        const val MONITORING_RUNNING = "monitoringRunning"
        const val LAST_APP_OPENED_AT = "lastAppOpenedAt"
        const val LAST_APP_CLOSED_AT = "lastAppClosedAt"
        const val LAST_MONITORING_HEARTBEAT_AT = "lastMonitoringHeartbeatAt"
        const val LAST_SEEN = "lastSeen"
    }

    object Connectivity {
        const val TRANSPORT = "transportType"
        const val IP = "ipAddress"
        const val LAST_UPDATED = "lastUpdated"
        const val PING_REQUEST = "pingRequest"
        const val PING_RESPONSE = "pingResponse"
        const val PING_LATENCY_MS = "pingLatencyMs"
    }

    object Permissions {
        const val ITEMS = "items"
        const val UPDATED_AT = "updatedAt"
        const val GRANTED_COUNT = "grantedCount"
        const val TOTAL_COUNT = "totalCount"
    }

    object Capabilities {
        const val NOTIFICATION = "notification"
        const val FCM_TOKEN = "fcmToken"
    }

    object Event {
        const val TYPE = "type"
        const val NOTIFICATION_ID = "notificationId"
    }

    object Rules {
        const val SCOPE = "scope"
        const val ENABLED = "enabled"
    }

    object Config {
        const val WEBVIEW_URL = "webviewUrl"
        const val LOCATION_INTERVAL_MS = "locationIntervalMs"

        /**
         * 0 disables usage collection.
         */
        const val USAGE_INTERVAL_MS = "usageIntervalMs"

        /**
         * List of ConfigurablePermission keys. Missing on a device = inherit /defaultConfig.
         */
        const val REQUIRED_PERMISSIONS = "requiredPermissions"
        const val TELEGRAM = "telegram"
        const val TELEGRAM_ENABLED = "enabled"
        const val TELEGRAM_BOT_TOKEN = "botToken"
        const val TELEGRAM_CHAT_ID = "chatId"
    }

    object NotificationFilter {
        const val SOURCES = "sources"

        const val LABEL = "label"
        const val ENABLED = "enabled"
        const val HEADER_EMOJI = "headerEmoji"
        const val FROM_EMOJI = "fromEmoji"
        const val PACKAGES = "packages"
        const val USES_DEFAULT_SMS = "usesDefaultSms"
        const val NOISE_PATTERNS = "noisePatterns"
    }

    object Sort {
        const val SORT_KEY = "sortKey"
        const val TIMESTAMP = "timestamp"
    }
}
