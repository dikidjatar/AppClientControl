package com.xeg911.appclient.notification.icon

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

/**
 * Composites a source badge icon onto the bottom-right corner of an avatar bitmap.
 */
object NotificationBadgeOverlay {

    /**
     * @param avatar         The sender's profile picture. Cropped to a circle.
     * @param badge          The source-app icon (e.g. WhatsApp green). Cropped to a circle.
     * @param badgeFraction  Badge diameter as a fraction of [avatar] size (default 36 %).
     * @param borderFraction White ring thickness as a fraction of badge diameter (default 18 %).
     * @return A new [Bitmap] of the same dimensions as [avatar] with the badge composited.
     */
    fun compose(
        avatar: Bitmap,
        badge: Bitmap,
        badgeFraction: Float = 0.36f,
        borderFraction: Float = 0.18f
    ): Bitmap {
        val size = minOf(avatar.width, avatar.height)
        val output = createBitmap(size, size)
        val canvas = Canvas(output)

        // Circular avatar
        val avatarPaint = circlePaint(avatar, offsetX = 0f, offsetY = 0f)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, avatarPaint)

        // Badge metrics
        val badgeDiam = (size * badgeFraction).toInt().coerceAtLeast(4)
        val borderThick = (badgeDiam * borderFraction).toInt().coerceAtLeast(2)

        // Badge circle center, pushed to bottom-right quadrant, fully inside the avatar circle.
        // cos(45°) ≈ 0.707; the badge is centred along the 45° diagonal from the avatar center.
        val offset = (size / 2f) * 0.707f          // distance from avatar centre to badge centre
        val cx = size / 2f + offset                // x center of badge circle
        val cy = size / 2f + offset                // y center of badge circle

        // White border ring
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawCircle(cx, cy, badgeDiam / 2f + borderThick, borderPaint)

        // Circular badge
        val scaledBadge = badge.scale(badgeDiam, badgeDiam)
        val badgePaint = circlePaint(
            scaledBadge, offsetX = cx - badgeDiam / 2f, offsetY = cy - badgeDiam / 2f
        )
        canvas.drawCircle(cx, cy, badgeDiam / 2f, badgePaint)

        return output
    }

    private fun circlePaint(bitmap: Bitmap, offsetX: Float, offsetY: Float): Paint {
        val shader = android.graphics.BitmapShader(
            bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP
        )
        if (offsetX != 0f || offsetY != 0f) {
            shader.setLocalMatrix(Matrix().apply { setTranslate(offsetX, offsetY) })
        }
        return Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
    }
}