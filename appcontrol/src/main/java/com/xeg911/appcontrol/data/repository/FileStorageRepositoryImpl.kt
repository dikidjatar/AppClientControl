package com.xeg911.appcontrol.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.xeg911.appcontrol.core.util.TelegramConfigField
import com.xeg911.appcontrol.data.remote.telegram.TelegramStorageClient
import com.xeg911.appcontrol.domain.model.LocalFileInfo
import com.xeg911.appcontrol.domain.model.StorageEvent
import com.xeg911.appcontrol.domain.model.TelegramChannelConfig
import com.xeg911.appcontrol.domain.repository.DeviceConfigRepository
import com.xeg911.appcontrol.domain.repository.FileStorageRepository
import com.xeg911.shared.data.model.transfer.FileTransferLimits
import com.xeg911.shared.data.model.transfer.FileTransferMeta
import com.xeg911.shared.data.model.transfer.FileTransferParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorageRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val deviceConfigRepository: DeviceConfigRepository,
    private val client: TelegramStorageClient,
) : FileStorageRepository {

    override suspend fun describe(uri: Uri): Result<LocalFileInfo> = withContext(Dispatchers.IO) {
        runCatching {
            var name: String? = null
            var size = -1L
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        .takeIf { it >= 0 }
                        ?.let { name = cursor.getString(it) }
                    cursor.getColumnIndex(OpenableColumns.SIZE)
                        .takeIf { it >= 0 && !cursor.isNull(it) }
                        ?.let { size = cursor.getLong(it) }
                }
            }
            if (size < 0) {
                size = context.contentResolver
                    .openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            }
            if (size <= 0) throw FileNotFoundException("Cannot read the selected file")
            val mime = context.contentResolver.getType(uri)
                ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    name?.substringAfterLast('.', "").orEmpty().lowercase()
                )
                ?: FileTransferParams.DEFAULT_MIME
            LocalFileInfo(
                uri = uri,
                name = sanitize(name ?: uri.lastPathSegment ?: "file"),
                mimeType = mime,
                sizeBytes = size,
            )
        }
    }

    override suspend fun isStorageConfigured(deviceId: String): Boolean =
        runCatching { storageConfig(deviceId) }.isSuccess

    override fun upload(
        deviceId: String,
        file: LocalFileInfo,
        caption: String
    ): Flow<StorageEvent> = channelFlow {
        val config = storageConfig(deviceId)
        if (file.sizeBytes > FileTransferLimits.MAX_UPLOAD_BYTES) {
            throw IllegalArgumentException("File is larger than the 50 MB Bot API upload limit")
        }
        send(StorageEvent.Progress(0L, file.sizeBytes))
        val stored = client.upload(
            config = config,
            fileName = file.name,
            mimeType = file.mimeType,
            sizeBytes = file.sizeBytes,
            caption = caption,
            openStream = {
                context.contentResolver.openInputStream(file.uri)
                    ?: throw FileNotFoundException("Cannot open ${file.name}")
            },
            onProgress = { trySend(StorageEvent.Progress(it, file.sizeBytes)) },
        ).getOrThrow()
        send(
            StorageEvent.Uploaded(
                FileTransferMeta(
                    fileId = stored.fileId,
                    fileName = file.name,
                    mimeType = file.mimeType,
                    sizeBytes = stored.fileSize,
                    downloadUrl = stored.downloadUrl,
                )
            )
        )
    }.flowOn(Dispatchers.IO)

    override fun download(deviceId: String, meta: FileTransferMeta): Flow<StorageEvent> =
        channelFlow {
            val config = storageConfig(deviceId)
            val info = client.fileInfo(config, meta.fileId).getOrThrow()
            val total = if (info.fileSize > 0) info.fileSize else meta.sizeBytes
            if (total > FileTransferLimits.MAX_DOWNLOAD_BYTES) {
                throw IllegalArgumentException("File is larger than the 20 MB Bot API download limit")
            }
            send(StorageEvent.Progress(0L, total))
            val target = createPending(meta.fileName, meta.mimeType)
            runCatching {
                context.contentResolver.openOutputStream(target, "w")
                    ?.use { out ->
                        client.download(
                            config = config,
                            info = info,
                            sink = out,
                            maxBytes = FileTransferLimits.MAX_DOWNLOAD_BYTES,
                            onProgress = { trySend(StorageEvent.Progress(it, total)) }
                        ).getOrThrow()
                    } ?: throw IllegalStateException("Cannot write to Downloads")
            }
                .onFailure { context.contentResolver.delete(target, null, null) }
                .getOrThrow()
            context.contentResolver.update(
                target,
                ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                null,
                null
            )
            send(StorageEvent.Saved(target))
        }.flowOn(Dispatchers.IO)

    private suspend fun storageConfig(deviceId: String): TelegramChannelConfig {
        val config = deviceConfigRepository
            .observeConfig(deviceId)
            .first()
            .getOrThrow()
            .telegram[TelegramConfigField.PURPOSE_STORAGE]
        if (
            config == null ||
            !config.enabled ||
            config.botToken.isBlank() ||
            config.chatId.isBlank()
        ) {
            throw IllegalStateException("Telegram Storage is not configured for this device (Config tab)")
        }
        return config
    }

    private fun createPending(fileName: String, mimeType: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, sanitize(fileName))
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOAD_DIR"
            )
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        return context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("MediaStore refused to create the download entry")
    }

    private fun sanitize(name: String): String =
        name.substringAfterLast('/').replace(Regex("[\\x00-\\x1F\"*:<>?|]"), "_").trim('.', ' ')
            .ifBlank { "file_${System.currentTimeMillis()}" }.take(120)

    private companion object {
        const val DOWNLOAD_DIR = "AppControl"
    }
}
