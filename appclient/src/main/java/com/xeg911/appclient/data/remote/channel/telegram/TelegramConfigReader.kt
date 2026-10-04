package com.xeg911.appclient.data.remote.channel.telegram

import com.xeg911.appclient.core.firebase.valueOrNull
import com.xeg911.appclient.data.remote.firebase.DeviceFirebaseDataSource
import com.xeg911.shared.firebase.FirebasePaths.Config
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramConfigReader @Inject constructor(
    private val dataSource: DeviceFirebaseDataSource
) {
    suspend fun read(deviceId: String, purpose: String): TelegramBotConfig = runCatching {
        val snapshot = dataSource.readTelegramConfig(deviceId, purpose)
        TelegramBotConfig(
            enabled = snapshot.child(Config.TELEGRAM_ENABLED).valueOrNull<Boolean>() ?: false,
            botToken = snapshot.child(Config.TELEGRAM_BOT_TOKEN).valueOrNull<String>().orEmpty(),
            chatId = snapshot.child(Config.TELEGRAM_CHAT_ID).value?.toString().orEmpty(),
        )
    }.getOrDefault(TelegramBotConfig())
}
