package com.xeg911.appclient.notification.icon

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.core.graphics.drawable.IconCompat
import com.xeg911.appclient.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves notification icons from three server-provided source formats:
 *
 * - **Drawable name** ("round_warning_24", "alert", …)
 *
 * - **Remote URL** ("https://cdn.example.com/icon.png")
 *
 * - **Base64 image** ("data:image/png;base64…")
 *
 * - **Default** (null / blank)
 */
@Singleton
class NotificationIconResolver @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private class CacheEntry(val bitmap: Bitmap?)

    private val bitmapCache = ConcurrentHashMap<String, CacheEntry>()

    // One mutex per URL prevents concurrent downloads of the same resource.
    private val mutexMap = ConcurrentHashMap<String, Mutex>()

    suspend fun resolveSmallIcon(source: String?): IconCompat {
        return when (val spec = NotificationIconSpec.parse(source)) {
            is NotificationIconSpec.Default ->
                defaultSmallIcon()

            is NotificationIconSpec.DrawableName ->
                resolveDrawableAsIcon(spec.name) ?: defaultSmallIcon()

            is NotificationIconSpec.RemoteUrl -> {
                val bitmap = resolveCachedBitmap(spec.url)
                if (bitmap != null) IconCompat.createWithBitmap(bitmap)
                else defaultSmallIcon()
            }

            is NotificationIconSpec.Base64Image -> {
                val bitmap = decodeBase64(spec.raw)
                if (bitmap != null) IconCompat.createWithBitmap(bitmap)
                else defaultSmallIcon()
            }
        }
    }

    /**
     * Resolves a large icon [Bitmap] (shown in the notification body).
     * Suspend, may download.
     */
    suspend fun resolveLargeIcon(source: String?): Bitmap? = resolveBitmap(source)

    /**
     * Resolves an image [Bitmap] for BigPictureStyle.
     * Suspend, may download.
     */
    suspend fun resolveImage(source: String?): Bitmap? = resolveBitmap(source)

    private suspend fun resolveBitmap(source: String?): Bitmap? {
        return when (val spec = NotificationIconSpec.parse(source)) {
            is NotificationIconSpec.Default -> null

            is NotificationIconSpec.DrawableName ->
                runCatching {
                    val resId = identifierOf(spec.name)
                    if (resId != 0) BitmapFactory.decodeResource(context.resources, resId)
                    else null
                }.getOrNull()

            is NotificationIconSpec.RemoteUrl ->
                resolveCachedBitmap(spec.url)

            is NotificationIconSpec.Base64Image ->
                decodeBase64(spec.raw)
        }
    }

    private fun defaultSmallIcon(): IconCompat =
        IconCompat.createWithResource(context, R.drawable.round_notifications_24)

    private fun resolveDrawableAsIcon(name: String): IconCompat? {
        val resId = identifierOf(name)
        return if (resId != 0) IconCompat.createWithResource(context, resId) else null
    }

    private suspend fun resolveCachedBitmap(url: String): Bitmap? {
        // Fast path — already in cache (null means a previous download failed).
        bitmapCache[url]?.let { return it.bitmap }

        val mutex = mutexMap.getOrPut(url) { Mutex() }
        return mutex.withLock {
            // Re-check after acquiring the lock; another coroutine may have
            // completed the download while we were waiting.
            bitmapCache[url]?.let { return@withLock it.bitmap }

            val bitmap = downloadBitmap(url)
            bitmapCache[url] = CacheEntry(bitmap)
            bitmap
        }
    }

    private suspend fun downloadBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            okHttpClient.newCall(Request.Builder().url(url).build())
                .execute()
                .use { response ->
                    val bytes = response.body.bytes()
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
        }.onFailure { Log.e(TAG, "Failed to download icon: $url", it) }
            .getOrNull()
    }

    private fun decodeBase64(raw: String): Bitmap? {
        return runCatching {
            val commaIdx = raw.indexOf(',')
            require(commaIdx >= 0) { "Invalid data URI — no comma found" }
            val bytes = Base64.decode(raw.substring(commaIdx + 1), Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.onFailure { Log.w(TAG, "Failed to decode base64 icon", it) }
            .getOrNull()
    }

    @SuppressLint("DiscouragedApi")
    private fun identifierOf(name: String): Int =
        context.resources.getIdentifier(name, "drawable", context.packageName)

    private companion object {
        const val TAG = "NotificationIconResolver"
    }
}
