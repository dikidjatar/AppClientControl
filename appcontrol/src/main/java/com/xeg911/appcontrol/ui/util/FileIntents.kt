package com.xeg911.appcontrol.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri

fun Context.openFile(uri: Uri, mimeType: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mimeType.ifBlank { "*/*" })
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return runCatching { startActivity(Intent.createChooser(intent, null)) }.isSuccess
}

fun Context.shareFile(uri: Uri, mimeType: String): Boolean {
    val intent = Intent(Intent.ACTION_SEND)
        .setType(mimeType.ifBlank { "*/*" })
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    return runCatching { startActivity(Intent.createChooser(intent, null)) }.isSuccess
}

fun Context.shareText(
    text: String,
    subject: String? = null,
    mimeType: String = "text/plain"
): Boolean {
    val intent = Intent(Intent.ACTION_SEND)
        .setType(mimeType)
        .putExtra(Intent.EXTRA_TEXT, text)
        .apply { subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) } }
    return runCatching { startActivity(Intent.createChooser(intent, subject)) }.isSuccess
}
