package com.xeg911.appclient.data.remote.storage

import com.xeg911.appclient.data.remote.channel.telegram.TelegramApi
import com.xeg911.appclient.data.remote.channel.telegram.TelegramBotConfig
import com.xeg911.appclient.data.remote.channel.telegram.TelegramFileInfo
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramStorageDownloader @Inject constructor(
    private val telegramApi: TelegramApi,
) {
    suspend fun fileInfo(config: TelegramBotConfig, fileId: String): Result<TelegramFileInfo> =
        telegramApi.getFile(config.botToken, fileId)

    suspend fun download(
        config: TelegramBotConfig,
        info: TelegramFileInfo,
        sink: OutputStream,
        maxBytes: Long,
        onProgress: (Long) -> Unit,
    ): Result<Long> = telegramApi.downloadFile(
        botToken = config.botToken,
        filePath = info.filePath,
        sink = sink,
        maxBytes = maxBytes,
        onProgress = onProgress,
    )
}
