package com.xeg911.appcontrol.data.remote.telegram

import com.xeg911.appcontrol.domain.model.TelegramChannelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
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

class TelegramApiException(val httpCode: Int, val description: String) :
    IOException("Telegram HTTP $httpCode: $description") {
    val isRetryable: Boolean get() = httpCode == 429 || httpCode >= 500
}

data class TelegramStoredFile(val fileId: String, val fileSize: Long, val downloadUrl: String)

data class TelegramFileInfo(val fileId: String, val fileSize: Long, val filePath: String)

@Singleton
class TelegramStorageClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    suspend fun upload(
        config: TelegramChannelConfig,
        fileName: String,
        mimeType: String,
        sizeBytes: Long,
        caption: String,
        openStream: () -> InputStream,
        onProgress: (Long) -> Unit,
    ): Result<TelegramStoredFile> = withContext(Dispatchers.IO) {
        runCatching {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", config.chatId)
                .addFormDataPart(
                    "document", fileName,
                    StreamRequestBody(
                        mimeType.toMediaTypeOrOctet(),
                        sizeBytes,
                        openStream,
                        onProgress
                    ),
                )
                .apply { if (caption.isNotBlank()) addFormDataPart("caption", caption) }
                .build()
            val request = Request.Builder().url(endpoint(config, "sendDocument")).post(body).build()
            val sent = okHttpClient.newCall(request).await().use { response ->
                response.requireOk()
                val document = JSONObject(response.body!!.string()).getJSONObject("result")
                    .getJSONObject("document")
                document.getString("file_id") to document.optLong("file_size", sizeBytes)
            }
            val url =
                fileInfo(config, sent.first).map { fileUrl(config, it.filePath) }.getOrDefault("")
            TelegramStoredFile(fileId = sent.first, fileSize = sent.second, downloadUrl = url)
        }
    }

    suspend fun fileInfo(config: TelegramChannelConfig, fileId: String): Result<TelegramFileInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request =
                    Request.Builder().url("${endpoint(config, "getFile")}?file_id=$fileId").get()
                        .build()
                okHttpClient.newCall(request).await().use { response ->
                    response.requireOk()
                    val result = JSONObject(response.body!!.string()).getJSONObject("result")
                    TelegramFileInfo(
                        fileId = result.getString("file_id"),
                        fileSize = result.optLong("file_size", -1L),
                        filePath = result.getString("file_path"),
                    )
                }
            }
        }

    suspend fun download(
        config: TelegramChannelConfig,
        info: TelegramFileInfo,
        sink: OutputStream,
        maxBytes: Long,
        onProgress: (Long) -> Unit,
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(fileUrl(config, info.filePath)).get().build()
            okHttpClient.newCall(request).await().use { response ->
                response.requireOk()
                val input = response.body!!.byteStream()
                val buffer = ByteArray(BUFFER_SIZE)
                var total = 0L
                while (true) {
                    coroutineContext.ensureActive()
                    val read = input.read(buffer)
                    if (read == -1) break
                    total += read
                    if (total > maxBytes) throw TelegramApiException(
                        413,
                        "File exceeds the $maxBytes byte limit"
                    )
                    sink.write(buffer, 0, read)
                    onProgress(total)
                }
                sink.flush()
                total
            }
        }
    }

    private fun endpoint(config: TelegramChannelConfig, method: String) =
        "$API_BASE_URL${config.botToken}/$method"

    private fun fileUrl(config: TelegramChannelConfig, filePath: String) =
        "$FILE_BASE_URL${config.botToken}/$filePath"

    private fun Response.requireOk() {
        if (isSuccessful) return
        val raw = runCatching { body!!.string() }.getOrDefault("")
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
        const val API_BASE_URL = "https://api.telegram.org/bot"
        const val FILE_BASE_URL = "https://api.telegram.org/file/bot"
        const val BUFFER_SIZE = 64 * 1024
    }
}
