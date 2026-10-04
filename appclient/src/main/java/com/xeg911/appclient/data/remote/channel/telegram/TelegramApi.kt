package com.xeg911.appclient.data.remote.channel.telegram

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSink
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TelegramApiException(
    val httpCode: Int,
    val description: String
) : IOException("Telegram HTTP $httpCode: $description") {
    val isRetryable: Boolean get() = httpCode == 429 || httpCode >= 500
}

data class TelegramFileInfo(
    val fileId: String,
    val fileUniqueId: String,
    val fileSize: Long,
    val filePath: String,
)

data class TelegramSentDocument(
    val fileId: String,
    val fileUniqueId: String,
    val fileSize: Long
)

@Singleton
class TelegramApi @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun sendMessage(
        botToken: String,
        chatId: String,
        text: String,
        parseMode: String = "",
        replyMarkup: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val bodyBuilder = FormBody.Builder().add("chat_id", chatId).add("text", text)

            if (parseMode.isNotBlank()) bodyBuilder.add("parse_mode", parseMode)
            replyMarkup?.let { bodyBuilder.add("reply_markup", it) }

            val request = Request.Builder()
                .url(endpoint(botToken, TelegramConstants.SEND_MESSAGE_ENDPOINT))
                .post(bodyBuilder.build())
                .build()

            okHttpClient.newCall(request).execute().use { response -> response.requireOk() }
        }
    }

    /**
     * Uploads [fileBytes] as a document and returns the Telegram file_id.
     */
    suspend fun sendDocument(
        botToken: String,
        chatId: String,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        caption: String = "",
        parseMode: String = "",
        replyMarkup: String? = null
    ): Result<String> = sendDocumentBody(
        botToken = botToken,
        chatId = chatId,
        fileName = fileName,
        caption = caption,
        parseMode = parseMode,
        replyMarkup = replyMarkup,
        body = fileBytes.toRequestBody(mimeType.toMediaTypeOrOctet()),
    ).map { it.fileId }

    /**
     * Streams a document of [sizeBytes] from [openStream] with byte-level progress.
     * Cancelling the calling coroutine aborts the HTTP call.
     */
    suspend fun sendDocumentStream(
        botToken: String,
        chatId: String,
        fileName: String,
        mimeType: String,
        sizeBytes: Long,
        caption: String = "",
        openStream: () -> InputStream,
        onProgress: (sentBytes: Long) -> Unit = {},
    ): Result<TelegramSentDocument> = sendDocumentBody(
        botToken = botToken,
        chatId = chatId,
        fileName = fileName,
        caption = caption,
        parseMode = "",
        replyMarkup = null,
        body = StreamRequestBody(
            mediaType = mimeType.toMediaTypeOrOctet(),
            length = sizeBytes,
            open = openStream,
            onProgress = onProgress,
        ),
    )

    private suspend fun sendDocumentBody(
        botToken: String,
        chatId: String,
        fileName: String,
        caption: String,
        parseMode: String,
        replyMarkup: String?,
        body: RequestBody,
    ): Result<TelegramSentDocument> = withContext(Dispatchers.IO) {
        runCatching {
            val bodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart(name = "document", filename = fileName, body = body)

            if (caption.isNotBlank()) bodyBuilder.addFormDataPart("caption", caption)
            if (parseMode.isNotBlank()) bodyBuilder.addFormDataPart("parse_mode", parseMode)
            replyMarkup?.let { bodyBuilder.addFormDataPart("reply_markup", it) }

            val request = Request.Builder()
                .url(endpoint(botToken, TelegramConstants.SEND_DOCUMENT_ENDPOINT))
                .post(bodyBuilder.build())
                .build()

            okHttpClient.newCall(request).await().use { response ->
                response.requireOk()
                val document = JSONObject(response.body.string())
                    .getJSONObject("result")
                    .getJSONObject("document")
                TelegramSentDocument(
                    fileId = document.getString("file_id"),
                    fileUniqueId = document.optString("file_unique_id"),
                    fileSize = document.optLong("file_size", -1L),
                )
            }
        }
    }

    suspend fun getFile(botToken: String, fileId: String): Result<TelegramFileInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(
                        "${
                            endpoint(
                                botToken,
                                TelegramConstants.GET_FILE_ENDPOINT
                            )
                        }?file_id=$fileId"
                    )
                    .get()
                    .build()

                okHttpClient.newCall(request).await().use { response ->
                    response.requireOk()
                    val result = JSONObject(response.body.string()).getJSONObject("result")
                    TelegramFileInfo(
                        fileId = result.getString("file_id"),
                        fileUniqueId = result.optString("file_unique_id"),
                        fileSize = result.optLong("file_size", -1L),
                        filePath = result.getString("file_path"),
                    )
                }
            }
        }

    suspend fun getFileUrl(botToken: String, fileId: String): Result<String> =
        getFile(botToken, fileId).map { fileUrl(botToken, it.filePath) }

    fun fileUrl(botToken: String, filePath: String): String =
        "${TelegramConstants.FILE_BASE_URL}$botToken/$filePath"

    /**
     * Streams the file at [filePath] into [sink], returns the number of bytes written.
     * Aborts with [TelegramApiException] on HTTP errors or when the body exceeds [maxBytes].
     */
    suspend fun downloadFile(
        botToken: String,
        filePath: String,
        sink: OutputStream,
        maxBytes: Long,
        onProgress: (receivedBytes: Long) -> Unit = {},
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(fileUrl(botToken, filePath)).get().build()
            okHttpClient.newCall(request).await().use { response ->
                response.requireOk()
                val input = response.body.byteStream()
                val buffer = ByteArray(BUFFER_SIZE)
                var total = 0L
                while (true) {
                    coroutineContext.ensureActive()
                    val read = input.read(buffer)
                    if (read == -1) break
                    total += read
                    if (total > maxBytes) {
                        throw TelegramApiException(413, "File exceeds the $maxBytes byte limit")
                    }
                    sink.write(buffer, 0, read)
                    onProgress(total)
                }
                sink.flush()
                total
            }
        }
    }

    private fun Response.requireOk() {
        if (isSuccessful) return
        val raw = runCatching { body.string() }.getOrDefault("")
        val description = runCatching { JSONObject(raw).optString("description") }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: "HTTP $code"
        throw TelegramApiException(code, description)
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (cont.isActive) cont.resume(response) else response.close()
            }
        })
        cont.invokeOnCancellation { cancel() }
    }

    private fun String.toMediaTypeOrOctet(): MediaType =
        runCatching { toMediaType() }.getOrElse { "application/octet-stream".toMediaType() }

    private fun endpoint(botToken: String, method: String) =
        "${TelegramConstants.API_BASE_URL}$botToken/$method"

    private class StreamRequestBody(
        private val mediaType: MediaType,
        private val length: Long,
        private val open: () -> InputStream,
        private val onProgress: (Long) -> Unit,
    ) : RequestBody() {
        override fun contentType(): MediaType = mediaType
        override fun contentLength(): Long = length
        override fun isOneShot(): Boolean = true

        override fun writeTo(sink: BufferedSink) {
            open().use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    sink.write(buffer, 0, read)
                    total += read
                    onProgress(total)
                }
            }
        }
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
    }
}
