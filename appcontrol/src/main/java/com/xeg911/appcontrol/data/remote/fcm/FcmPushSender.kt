package com.xeg911.appcontrol.data.remote.fcm

import com.xeg911.appcontrol.domain.repository.PushSender
import com.xeg911.shared.data.model.notification.FcmNotificationPayload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resumeWithException

class FcmSendException(
    val httpCode: Int,
    message: String,
    val errorCode: String? = null,
) : Exception(message)

/**
 * Sends data-only messages through the FCM HTTP v1 API. The payload is placed
 * in the "payload" data key exactly as AppClient's FCM service expects.
 */
@Singleton
class FcmPushSender @Inject constructor(
    private val credentials: FcmCredentialsProvider,
    private val json: Json,
    private val client: OkHttpClient,
) : PushSender {

    override suspend fun send(token: String, payload: FcmNotificationPayload): Result<Unit> =
        try {
            val accessToken = withContext(Dispatchers.IO) { credentials.accessToken() }

            val request = Request.Builder()
                .url(ENDPOINT.format(credentials.projectId))
                .header("Authorization", "Bearer $accessToken")
                .post(buildEnvelope(token, payload).toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).await().use { response ->
                if (!response.isSuccessful) {
                    val raw = response.body?.string().orEmpty()
                    throw parseError(raw, response.code)
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun buildEnvelope(token: String, payload: FcmNotificationPayload) = buildJsonObject {
        putJsonObject("message") {
            put("token", token)
            putJsonObject("data") {
                put(KEY_PAYLOAD, json.encodeToString(payload))
                if (payload.startMonitoring) put(KEY_START_MONITORING, "true")
            }
            putJsonObject("android") {
                put("priority", "HIGH")
            }
        }
    }

    private fun parseError(raw: String, code: Int): FcmSendException {
        val error =
            runCatching { json.parseToJsonElement(raw).jsonObject["error"]?.jsonObject }.getOrNull()
        val message =
            error?.get("message")?.jsonPrimitive?.content ?: "FCM request failed (HTTP $code)"
        val errorCode = error?.get("details")?.jsonArray
            ?.firstNotNullOfOrNull { it.jsonObject["errorCode"]?.jsonPrimitive?.content }
            ?: error?.get("status")?.jsonPrimitive?.content
        return FcmSendException(code, message, errorCode)
    }

    private companion object {
        const val ENDPOINT = "https://fcm.googleapis.com/v1/projects/%s/messages:send"
        const val KEY_PAYLOAD = "payload"
        const val KEY_START_MONITORING = "startMonitoring"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            cont.resume(response) { _, value, _ -> value.close() }
        }

        override fun onFailure(call: Call, e: IOException) {
            if (!cont.isCancelled) cont.resumeWithException(e)
        }
    })
}