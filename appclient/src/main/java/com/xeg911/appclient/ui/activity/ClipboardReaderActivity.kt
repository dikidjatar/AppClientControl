package com.xeg911.appclient.ui.activity

import android.content.ClipDescription
import android.content.ClipboardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.xeg911.appclient.core.di.ApplicationScope
import com.xeg911.appclient.data.remote.channel.RemoteChannelRegistry
import com.xeg911.appclient.event.DeviceEventReporter
import com.xeg911.shared.data.model.ClipboardSnapshot
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.notification.NotificationActionDef
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ClipboardReaderActivity : ComponentActivity() {

    @Inject
    lateinit var channelRegistry: RemoteChannelRegistry

    @Inject
    lateinit var eventReporter: DeviceEventReporter

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    private var handled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || handled) return
        handled = true
        val snapshot = readClipboard()
        val notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID).orEmpty()
        appScope.launch {
            channelRegistry.sendClipboard(snapshot)
            eventReporter.reportAction(
                notificationId = notificationId,
                actionId = NotificationActionDef.GET_CLIPBOARD.id,
                status = if (snapshot.isEmpty) DeviceEventStatus.NOT_AVAILABLE else DeviceEventStatus.SUCCESS,
                data = mapOf("items" to snapshot.items.size.toString()),
            )
        }
        finish()
    }

    private fun readClipboard(): ClipboardSnapshot {
        val clip =
            getSystemService(ClipboardManager::class.java).primaryClip ?: return ClipboardSnapshot()
        return ClipboardSnapshot(
            items = (0 until clip.itemCount).map {
                clip.getItemAt(it).coerceToText(this).toString()
            },
            label = clip.description?.label?.toString(),
            mimeType = clip.description?.getMimeType(0) ?: ClipDescription.MIMETYPE_TEXT_PLAIN,
        )
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
