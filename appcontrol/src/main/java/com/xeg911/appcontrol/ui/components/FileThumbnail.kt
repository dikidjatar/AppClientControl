package com.xeg911.appcontrol.ui.components

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.video.videoFrameMillis
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.ui.theme.AppTheme

enum class FileKind(@param:DrawableRes val iconRes: Int) {
    IMAGE(R.drawable.image_24px),
    VIDEO(R.drawable.videocam_24px),
    AUDIO(R.drawable.audiotrack_24px),
    PDF(R.drawable.picture_as_pdf_24px),
    DOCUMENT(R.drawable.description_24px),
    ARCHIVE(R.drawable.archive_24px),
    APK(R.drawable.apps_24px),
    OTHER(R.drawable.insert_drive_file_24px);

    val hasPreview: Boolean get() = this == IMAGE || this == VIDEO

    companion object {
        fun fromMime(mime: String): FileKind {
            val m = mime.lowercase()
            return when {
                m.startsWith("image/") -> IMAGE
                m.startsWith("video/") -> VIDEO
                m.startsWith("audio/") -> AUDIO
                m == "application/pdf" -> PDF
                m == "application/vnd.android.package-archive" -> APK
                m.contains("zip") || m.contains("compressed") || m.contains("tar") ||
                        m.contains("rar") || m.contains("7z") -> ARCHIVE

                m.startsWith("text/") || m.contains("document") || m.contains("word") ||
                        m.contains("sheet") || m.contains("excel") || m.contains("presentation") ||
                        m.contains("json") || m.contains("xml") -> DOCUMENT

                else -> OTHER
            }
        }
    }
}

@Composable
fun FileKind.tint(): Color = when (this) {
    FileKind.IMAGE -> AppTheme.colors.info
    FileKind.VIDEO -> AppTheme.colors.tertiary
    FileKind.AUDIO -> AppTheme.colors.warning
    FileKind.PDF -> AppTheme.colors.error
    FileKind.DOCUMENT -> AppTheme.colors.primary
    FileKind.ARCHIVE -> AppTheme.colors.secondary
    FileKind.APK -> AppTheme.colors.success
    FileKind.OTHER -> AppTheme.colors.neutral
}

@Composable
fun FileThumbnail(
    mimeType: String,
    modifier: Modifier = Modifier,
    localUri: Uri? = null,
    size: Dp = AppTheme.dimens.thumbnail,
    unavailable: Boolean = false,
) {
    val kind = FileKind.fromMime(mimeType)
    val tint = if (unavailable) MaterialTheme.colorScheme.onSurfaceVariant else kind.tint()
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(tint.copy(alpha = if (AppTheme.isDark) 0.2f else 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        if (localUri != null && kind.hasPreview && !unavailable) {
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(localUri)
                    .crossfade(true)
                    .apply { if (kind == FileKind.VIDEO) videoFrameMillis(1_000) }
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = painterResource(kind.iconRes),
            )
        } else {
            Icon(
                painter = painterResource(if (unavailable) R.drawable.broken_image_24px else kind.iconRes),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(size / 2),
            )
        }
    }
}
