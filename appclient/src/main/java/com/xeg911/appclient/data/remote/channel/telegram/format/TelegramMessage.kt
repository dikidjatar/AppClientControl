package com.xeg911.appclient.data.remote.channel.telegram.format

data class TelegramMessage(
    val text: String,
    val parseMode: String = "HTML",
    val replyMarkup: String? = null
)