package com.xeg911.appclient.transfer

import android.net.Uri
import android.util.Log
import com.xeg911.appclient.core.device.DeviceIdentifier
import com.xeg911.appclient.data.remote.channel.telegram.TelegramBotConfig
import com.xeg911.appclient.data.remote.storage.TelegramStorageDownloader
import com.xeg911.appclient.data.remote.storage.TelegramStorageUploader
import com.xeg911.appclient.transfer.model.TransferError
import com.xeg911.appclient.transfer.model.TransferException
import com.xeg911.appclient.transfer.model.TransferRequest
import com.xeg911.appclient.transfer.model.TransferState
import com.xeg911.appclient.transfer.model.toTransferError
import com.xeg911.appclient.transfer.source.UriFileResolver
import com.xeg911.appclient.transfer.storage.DownloadStorage
import com.xeg911.shared.data.model.transfer.FileTransferLimits
import com.xeg911.shared.data.model.transfer.FileTransferMeta
import com.xeg911.shared.data.model.transfer.FileTransferParams
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.security.DigestOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

sealed interface TransferOutcome {
    data class Success(val results: List<Map<String, String>>) : TransferOutcome
    data class Failure(val error: TransferError) : TransferOutcome
    data object Cancelled : TransferOutcome
}

@Singleton
class TransferWorker @Inject constructor(
    private val deviceIdentifier: DeviceIdentifier,
    private val uploader: TelegramStorageUploader,
    private val downloader: TelegramStorageDownloader,
    private val fileResolver: UriFileResolver,
    private val downloadStorage: DownloadStorage,
) {
    suspend fun execute(
        request: TransferRequest,
        onProgress: (TransferState.Running) -> Unit,
    ): TransferOutcome {
        var attempt = 1
        while (true) {
            val result = runCatching {
                val config = storageConfig()
                when (request) {
                    is TransferRequest.Download -> listOf(
                        download(
                            request,
                            config,
                            attempt,
                            onProgress
                        )
                    )

                    is TransferRequest.Upload -> upload(request, config, attempt, onProgress)
                }
            }
            result.onSuccess { return TransferOutcome.Success(it) }
            val throwable = result.exceptionOrNull() ?: return TransferOutcome.Cancelled
            if (throwable is CancellationException) return TransferOutcome.Cancelled
            val error = throwable.toTransferError()
            Log.w(TAG, "Transfer ${request.transferId} attempt $attempt failed: ${error.message}")
            if (!error.retryable || attempt >= MAX_ATTEMPTS) return TransferOutcome.Failure(error)
            delay(RETRY_BASE_DELAY_MS * (1L shl (attempt - 1)))
            attempt++
        }
    }

    fun cleanup(request: TransferRequest) {
        if (request is TransferRequest.Upload) fileResolver.clearStaging(request.transferId)
    }

    private suspend fun storageConfig(): TelegramBotConfig {
        val deviceId = deviceIdentifier.resolveDeviceId()
        val config = uploader.storageConfig(deviceId)
        if (!config.isReady) {
            throw TransferException(
                TransferError(
                    TransferError.Code.NOT_CONFIGURED,
                    "Telegram Storage is not configured for this device"
                )
            )
        }
        return config
    }

    private suspend fun download(
        request: TransferRequest.Download,
        config: TelegramBotConfig,
        attempt: Int,
        onProgress: (TransferState.Running) -> Unit,
    ): Map<String, String> {
        val info = downloader.fileInfo(config, request.fileId).getOrThrow()
        val total = if (info.fileSize > 0) info.fileSize else request.expectedSize
        if (total > FileTransferLimits.MAX_DOWNLOAD_BYTES) {
            throw TransferException(
                TransferError(
                    TransferError.Code.FILE_TOO_LARGE,
                    "File is larger than the 20 MB Bot API download limit"
                )
            )
        }
        downloadStorage.ensureFreeSpace(total)
        onProgress(TransferState.Running(request, 0L, total, attempt = attempt))

        val target =
            downloadStorage.createPending(request.fileName, request.mimeType, request.subDir)
        val digest = MessageDigest.getInstance("SHA-256")
        val written = runCatching {
            DigestOutputStream(downloadStorage.openOutput(target), digest).use { out ->
                downloader.download(
                    config,
                    info,
                    out,
                    FileTransferLimits.MAX_DOWNLOAD_BYTES
                ) { received ->
                    onProgress(TransferState.Running(request, received, total, attempt = attempt))
                }.getOrThrow()
            }
        }.onFailure { downloadStorage.discard(target) }.getOrThrow()

        val sha256 = digest.digest().toHex()
        if (request.sha256.isNotBlank() && !request.sha256.equals(sha256, ignoreCase = true)) {
            downloadStorage.discard(target)
            throw TransferException(
                TransferError(
                    TransferError.Code.CHECKSUM_MISMATCH,
                    "Downloaded file checksum does not match"
                )
            )
        }
        downloadStorage.publish(target)

        return FileTransferMeta(
            transferId = request.transferId,
            fileId = request.fileId,
            fileName = request.fileName,
            mimeType = request.mimeType,
            sizeBytes = written,
            sha256 = sha256,
        ).toParams() + mapOf(
            FileTransferParams.SAVED_URI to target.toString(),
            FileTransferParams.SAVED_PATH to downloadStorage.displayPath(
                target,
                request.fileName,
                request.subDir
            ),
        )
    }

    private suspend fun upload(
        request: TransferRequest.Upload,
        config: TelegramBotConfig,
        attempt: Int,
        onProgress: (TransferState.Running) -> Unit,
    ): List<Map<String, String>> {
        if (request.uris.isEmpty()) {
            throw TransferException(
                TransferError(
                    TransferError.Code.INVALID_REQUEST,
                    "No file selected"
                )
            )
        }
        val files = request.uris.map { raw ->
            val uri = fileResolver.parse(raw) ?: throw TransferException(
                TransferError(TransferError.Code.INVALID_REQUEST, "Invalid URI: $raw")
            )
            fileResolver.describe(uri).also { file ->
                fileResolver.validate(file, request.allowedMime, request.maxSizeBytes)
                    ?.let { throw TransferException(it) }
            }
        }
        return files.mapIndexed { index, file ->
            currentCoroutineContext().ensureActive()
            onProgress(
                TransferState.Running(
                    request,
                    0L,
                    file.sizeBytes,
                    index,
                    files.size,
                    attempt
                )
            )
            val sha256 = sha256Of(file.uri)
            val result = uploader.uploadStream(
                config = config,
                fileName = file.displayName,
                mimeType = file.mimeType,
                sizeBytes = file.sizeBytes,
                caption = request.caption,
                openStream = { fileResolver.open(file.uri) },
                onProgress = { sent ->
                    onProgress(
                        TransferState.Running(
                            request,
                            sent,
                            file.sizeBytes,
                            index,
                            files.size,
                            attempt
                        )
                    )
                },
            ).getOrThrow()
            FileTransferMeta(
                transferId = request.transferId,
                fileId = result.fileId,
                fileName = file.displayName,
                mimeType = file.mimeType,
                sizeBytes = result.fileSize,
                sha256 = sha256,
                downloadUrl = result.downloadUrl,
            ).toParams() + mapOf(
                FileTransferParams.SOURCE to request.source,
                FileTransferParams.FILE_INDEX to index.toString(),
                FileTransferParams.FILE_COUNT to files.size.toString(),
            )
        }
    }

    private suspend fun sha256Of(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fileResolver.open(uri).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private companion object {
        const val TAG = "TransferWorker"
        const val MAX_ATTEMPTS = 3
        const val RETRY_BASE_DELAY_MS = 2_000L
    }
}
