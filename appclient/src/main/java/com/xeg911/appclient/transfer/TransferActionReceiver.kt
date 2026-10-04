package com.xeg911.appclient.transfer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.xeg911.appclient.transfer.model.TransferRequest
import com.xeg911.appclient.transfer.model.TransferRequestCodec
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TransferActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var tracker: FileTransferTracker

    @Inject
    lateinit var launcher: FileTransferLauncher

    @Inject
    lateinit var notifications: TransferNotificationFactory

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CANCEL -> intent.getStringExtra(EXTRA_TRANSFER_ID)
                ?.let { tracker.requestCancel(it) }

            ACTION_RETRY -> TransferRequestCodec.decode(intent.getStringExtra(EXTRA_REQUEST))
                ?.let { request ->
                    NotificationManagerCompat.from(context)
                        .cancel(notifications.resultId(request.transferId))
                    launcher.start(request)
                }
        }
    }

    companion object {
        const val ACTION_CANCEL = "com.xeg911.appclient.transfer.action.CANCEL"
        const val ACTION_RETRY = "com.xeg911.appclient.transfer.action.RETRY"
        const val EXTRA_TRANSFER_ID = "extra_transfer_id"
        const val EXTRA_REQUEST = "extra_request"

        fun cancelIntent(context: Context, transferId: String): Intent =
            Intent(context, TransferActionReceiver::class.java)
                .setAction(ACTION_CANCEL)
                .putExtra(EXTRA_TRANSFER_ID, transferId)

        fun retryIntent(context: Context, request: TransferRequest): Intent =
            Intent(context, TransferActionReceiver::class.java)
                .setAction(ACTION_RETRY)
                .putExtra(EXTRA_REQUEST, TransferRequestCodec.encode(request))
    }
}
