package com.xeg911.appclient.transfer.model

import com.xeg911.appclient.data.remote.channel.telegram.TelegramApiException
import kotlinx.coroutines.CancellationException
import java.io.FileNotFoundException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class TransferError(
    val code: Code,
    val message: String,
    val retryable: Boolean = false,
) {
    enum class Code {
        NOT_CONFIGURED,
        INVALID_REQUEST,
        FILE_TOO_LARGE,
        FILE_NOT_FOUND,
        PERMISSION_DENIED,
        INSUFFICIENT_STORAGE,
        NETWORK,
        TELEGRAM_API,
        STORAGE,
        CHECKSUM_MISMATCH,
        UNKNOWN,
    }
}

class TransferException(val error: TransferError) : Exception(error.message)

fun Throwable.toTransferError(): TransferError = when (this) {
    is TransferException -> error
    is CancellationException -> throw this
    is TelegramApiException -> TransferError(
        code = if (httpCode == 413) TransferError.Code.FILE_TOO_LARGE else TransferError.Code.TELEGRAM_API,
        message = description,
        retryable = isRetryable,
    )

    is UnknownHostException, is SocketTimeoutException ->
        TransferError(
            TransferError.Code.NETWORK,
            "Network unavailable: ${message ?: javaClass.simpleName}",
            retryable = true
        )

    is FileNotFoundException ->
        TransferError(TransferError.Code.FILE_NOT_FOUND, message ?: "File not found")

    is SecurityException ->
        TransferError(
            TransferError.Code.PERMISSION_DENIED,
            message ?: "No permission to access the file"
        )

    is IOException ->
        TransferError(TransferError.Code.NETWORK, message ?: "I/O error", retryable = true)

    else -> TransferError(TransferError.Code.UNKNOWN, message ?: javaClass.simpleName)
}

sealed interface TransferState {
    val request: TransferRequest

    data class Queued(override val request: TransferRequest) : TransferState

    data class Running(
        override val request: TransferRequest,
        val bytesDone: Long,
        val bytesTotal: Long,
        val fileIndex: Int = 0,
        val fileCount: Int = 1,
        val attempt: Int = 1,
    ) : TransferState {
        val percent: Int?
            get() = if (bytesTotal > 0) ((bytesDone * 100) / bytesTotal).toInt()
                .coerceIn(0, 100) else null
    }

    data class Succeeded(
        override val request: TransferRequest,
        /** One entry per transferred file, becomes the event data. */
        val results: List<Map<String, String>>,
    ) : TransferState

    data class Failed(override val request: TransferRequest, val error: TransferError) :
        TransferState

    data class Cancelled(override val request: TransferRequest) : TransferState
}
