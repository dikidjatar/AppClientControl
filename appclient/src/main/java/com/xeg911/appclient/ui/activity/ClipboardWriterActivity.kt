package com.xeg911.appclient.ui.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.xeg911.appclient.R
import com.xeg911.appclient.event.DeviceEventReporter
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.notification.NotificationActionDef
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ClipboardWriterActivity : ComponentActivity() {

    @Inject
    lateinit var eventReporter: DeviceEventReporter

    private var handled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getStringExtra(EXTRA_TEXT).isNullOrBlank()) finish()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || handled) return
        handled = true
        write()
        finish()
    }

    private fun write() {
        val notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID).orEmpty()
        val text = intent.getStringExtra(EXTRA_TEXT) ?: return
        val label = intent.getStringExtra(EXTRA_LABEL)?.trim().orEmpty()

        runCatching {
            getSystemService(ClipboardManager::class.java).setPrimaryClip(
                ClipData.newPlainText(
                    label,
                    text
                )
            )
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                val message =
                    if (label.isNotEmpty()) getString(R.string.clipboard_copied_with_label, label)
                    else getString(R.string.clipboard_copied_generic)
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }.fold(
            onSuccess = {
                eventReporter.reportAction(
                    notificationId,
                    NotificationActionDef.COPY_TO_CLIPBOARD.id,
                    DeviceEventStatus.SUCCESS,
                    mapOf("label" to label, "length" to text.length.toString()),
                )
            },
            onFailure = { e ->
                eventReporter.reportAction(
                    notificationId,
                    NotificationActionDef.COPY_TO_CLIPBOARD.id,
                    DeviceEventStatus.FAILED,
                    mapOf("error" to (e.message ?: "unknown")),
                )
            },
        )
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_TEXT = "extra_clipboard_text"
        const val EXTRA_LABEL = "extra_clipboard_label"
    }
}
