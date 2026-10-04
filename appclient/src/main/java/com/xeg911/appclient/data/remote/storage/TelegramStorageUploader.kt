package com.xeg911.appclient.data.remote.storage

import com.xeg911.appclient.data.remote.channel.telegram.TelegramApi
import com.xeg911.appclient.data.remote.channel.telegram.TelegramBotConfig
import com.xeg911.appclient.data.remote.channel.telegram.TelegramConfigReader
import com.xeg911.appclient.data.remote.channel.telegram.TelegramConstants
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramStorageUploader @Inject constructor(
    private val telegramConfigReader: TelegramConfigReader,
    private val telegramApi: TelegramApi
) {
    suspend fun storageConfig(deviceId: String): TelegramBotConfig =
        telegramConfigReader.read(deviceId, TelegramConstants.CONFIG_STORAGE)

    suspend fun upload(
        deviceId: String,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        caption: String = "",
        captionParseMode: String = "",
        replyMarkup: String? = null
    ): TelegramUploadResult? {
        val config = storageConfig(deviceId)

        if (!config.isReady) return null

        val fileId = telegramApi.sendDocument(
            botToken = config.botToken,
            chatId = config.chatId,
            fileBytes = fileBytes,
            fileName = fileName,
            mimeType = mimeType,
            caption = caption,
            parseMode = captionParseMode,
            replyMarkup = replyMarkup
        ).getOrNull() ?: return null

        val downloadUrl = telegramApi.getFileUrl(
            botToken = config.botToken,
            fileId = fileId
        ).getOrNull() ?: return null

        return TelegramUploadResult(
            fileId = fileId,
            downloadUrl = downloadUrl,
            fileSize = fileBytes.size.toLong()
        )
    }

    /**
     * Streaming upload used by file transfers.
     */
    suspend fun uploadStream(
        config: TelegramBotConfig,
        fileName: String,
        mimeType: String,
        sizeBytes: Long,
        caption: String,
        openStream: () -> InputStream,
        onProgress: (Long) -> Unit,
    ): Result<TelegramUploadResult> = runCatching {
        val sent = telegramApi.sendDocumentStream(
            botToken = config.botToken,
            chatId = config.chatId,
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            caption = caption,
            openStream = openStream,
            onProgress = onProgress,
        ).getOrThrow()
        val downloadUrl = telegramApi.getFileUrl(config.botToken, sent.fileId).getOrDefault("")
        TelegramUploadResult(
            fileId = sent.fileId,
            downloadUrl = downloadUrl,
            fileSize = if (sent.fileSize > 0) sent.fileSize else sizeBytes,
        )
    }
}
