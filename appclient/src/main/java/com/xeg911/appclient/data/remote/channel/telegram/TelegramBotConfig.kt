package com.xeg911.appclient.data.remote.channel.telegram

data class TelegramBotConfig(
    val enabled: Boolean = false,
    val botToken: String = "",
    val chatId: String = ""
) {
    val isReady: Boolean
        get() = enabled && botToken.isNotBlank() && chatId.isNotBlank()
}