package com.xeg911.appclient.transfer

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.xeg911.appclient.core.di.IoDispatcher
import com.xeg911.appclient.event.DeviceEventReporter
import com.xeg911.appclient.transfer.model.TransferError
import com.xeg911.appclient.transfer.model.TransferRequest
import com.xeg911.appclient.transfer.model.TransferRequestCodec
import com.xeg911.appclient.transfer.model.TransferState
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.event.DeviceEventType
import com.xeg911.shared.data.model.transfer.FileTransferParams
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@AndroidEntryPoint
class FileTransferService : Service() {

    @Inject
    lateinit var worker: TransferWorker

    @Inject
    lateinit var tracker: FileTransferTracker

    @Inject
    lateinit var notifications: TransferNotificationFactory

    @Inject
    lateinit var eventReporter: DeviceEventReporter

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private lateinit var scope: CoroutineScope
    private var ready = false
    private val queue = Channel<TransferRequest>(Channel.UNLIMITED)
    private val pending = ConcurrentHashMap<String, TransferRequest>()
    private val activeCount = AtomicInteger(0)

    @Volatile
    private var current: Pair<String, Job>? = null
    private var lastNotified = 0L

    override fun onCreate() {
        super.onCreate()
        scope = CoroutineScope(SupervisorJob() + ioDispatcher)
        ready = startAsForeground()
        if (!ready) return
        scope.launch { for (request in queue) process(request) }
        scope.launch { tracker.cancelRequests.collect { cancelTransfer(it) } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!ready) {
            TransferRequestCodec.decode(intent?.getStringExtra(EXTRA_REQUEST))?.let { request ->
                report(
                    TransferState.Failed(
                        request,
                        TransferError(
                            TransferError.Code.UNKNOWN,
                            "Transfer service could not start in foreground"
                        )
                    )
                )
            }
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent != null) {
            when (intent.action) {
                ACTION_START -> TransferRequestCodec.decode(intent.getStringExtra(EXTRA_REQUEST))
                    ?.let { enqueue(it) }

                ACTION_CANCEL -> intent.getStringExtra(EXTRA_TRANSFER_ID)
                    ?.let { cancelTransfer(it) }
            }
        }
        if (activeCount.get() <= 0) stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun enqueue(request: TransferRequest) {
        if (pending.putIfAbsent(request.transferId, request) != null) return
        activeCount.incrementAndGet()
        tracker.update(TransferState.Queued(request))
        queue.trySend(request)
    }

    private fun cancelTransfer(transferId: String) {
        pending.remove(transferId)?.let { queued ->
            tracker.update(TransferState.Cancelled(queued))
            report(TransferState.Cancelled(queued))
            worker.cleanup(queued)
            activeCount.decrementAndGet()
        }
        current?.takeIf { it.first == transferId }?.second?.cancel()
    }

    private suspend fun process(request: TransferRequest) {
        if (pending.remove(request.transferId) == null) return
        val job = scope.launch {
            val outcome = worker.execute(request) { state -> onProgress(state) }
            val state = when (outcome) {
                is TransferOutcome.Success -> TransferState.Succeeded(request, outcome.results)
                is TransferOutcome.Failure -> TransferState.Failed(request, outcome.error)
                TransferOutcome.Cancelled -> TransferState.Cancelled(request)
            }
            finish(state)
        }
        current = request.transferId to job
        job.join()
        if (job.isCancelled && !tracker.isTerminal(request.transferId)) {
            finish(TransferState.Cancelled(request))
        }
        current = null
        if (activeCount.decrementAndGet() <= 0) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    @SuppressLint("MissingPermission")
    private fun finish(state: TransferState) {
        tracker.update(state)
        report(state)
        worker.cleanup(state.request)
        val manager = NotificationManagerCompat.from(this)
        runCatching {
            when (state) {
                is TransferState.Succeeded ->
                    manager.notify(
                        notifications.resultId(state.request.transferId),
                        notifications.success(state.request, state.results)
                    )

                is TransferState.Failed ->
                    manager.notify(
                        notifications.resultId(state.request.transferId),
                        notifications.failure(state.request, state.error)
                    )

                else -> Unit
            }
        }.onFailure { Log.w(TAG, "Cannot post result notification", it) }
    }

    @SuppressLint("MissingPermission")
    private fun onProgress(state: TransferState.Running) {
        tracker.update(state)
        val now = System.currentTimeMillis()
        if (now - lastNotified < PROGRESS_THROTTLE_MS && state.bytesDone != state.bytesTotal) return
        lastNotified = now
        runCatching {
            NotificationManagerCompat.from(this).notify(
                TransferNotificationFactory.SERVICE_NOTIFICATION_ID,
                notifications.progress(state, pending.size),
            )
        }
    }

    private fun report(state: TransferState) {
        val request = state.request
        val base = mapOf(FileTransferParams.TRANSFER_ID to request.transferId)
        when (state) {
            is TransferState.Succeeded -> state.results.forEach { data ->
                eventReporter.reportAction(
                    type = DeviceEventType.FILE_TRANSFER,
                    notificationId = request.notificationId,
                    actionId = request.actionId,
                    status = DeviceEventStatus.SUCCESS,
                    data = base + data
                )
            }

            is TransferState.Failed -> eventReporter.reportAction(
                type = DeviceEventType.FILE_TRANSFER,
                notificationId = request.notificationId,
                actionId = request.actionId,
                status = DeviceEventStatus.FAILED,
                data = base + mapOf(
                    FileTransferParams.ERROR to state.error.message,
                    FileTransferParams.ERROR_CODE to state.error.code.name,
                    FileTransferParams.RETRYABLE to state.error.retryable.toString(),
                ) + request.describe(),
            )

            is TransferState.Cancelled -> eventReporter.reportAction(
                type = DeviceEventType.FILE_TRANSFER,
                notificationId = request.notificationId,
                actionId = request.actionId,
                status = DeviceEventStatus.CANCELLED,
                data = base + request.describe()
            )

            else -> Unit
        }
    }

    private fun TransferRequest.describe(): Map<String, String> = when (this) {
        is TransferRequest.Download -> mapOf(
            FileTransferParams.FILE_ID to fileId,
            FileTransferParams.FILE_NAME to fileName,
        )

        is TransferRequest.Upload -> mapOf(FileTransferParams.SOURCE to source)
    }

    private fun startAsForeground(): Boolean = runCatching {
        ServiceCompat.startForeground(
            this,
            TransferNotificationFactory.SERVICE_NOTIFICATION_ID,
            notifications.idle(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }.onFailure {
        Log.w(TAG, "startForeground(dataSync) rejected", it)
        stopSelf()
    }.isSuccess

    companion object {
        const val ACTION_START = "com.xeg911.appclient.transfer.START"
        const val ACTION_CANCEL = "com.xeg911.appclient.transfer.CANCEL"
        const val EXTRA_REQUEST = "extra_request"
        const val EXTRA_TRANSFER_ID = "extra_transfer_id"
        private const val TAG = "FileTransferService"
        private const val PROGRESS_THROTTLE_MS = 400L
    }
}
