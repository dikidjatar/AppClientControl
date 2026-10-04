package com.xeg911.appclient.transfer.picker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.xeg911.appclient.R
import com.xeg911.appclient.event.DeviceEventReporter
import com.xeg911.appclient.transfer.FileTransferLauncher
import com.xeg911.appclient.transfer.model.TransferError
import com.xeg911.appclient.transfer.model.TransferException
import com.xeg911.appclient.transfer.model.TransferRequest
import com.xeg911.appclient.transfer.model.TransferRequestCodec
import com.xeg911.appclient.transfer.model.toTransferError
import com.xeg911.appclient.transfer.source.UriFileResolver
import com.xeg911.appclient.ui.theme.AppClientTheme
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.event.DeviceEventType
import com.xeg911.shared.data.model.transfer.FileInputSource
import com.xeg911.shared.data.model.transfer.FileTransferLimits
import com.xeg911.shared.data.model.transfer.FileTransferParams
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FilePickerActivity : ComponentActivity() {

    @Inject
    lateinit var fileResolver: UriFileResolver

    @Inject
    lateinit var launcher: FileTransferLauncher

    @Inject
    lateinit var eventReporter: DeviceEventReporter

    private var request: TransferRequest.Upload? = null
    private var isPreparing by mutableStateOf(false)
    private var finished = false

    private val pickContent = registerForActivityResult(ActivityResultContracts.GetContent()) {
        onPicked(listOfNotNull(it))
    }
    private val pickContents =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) {
            onPicked(it)
        }
    private val pickVisual = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        onPicked(listOfNotNull(it))
    }
    private val pickVisuals = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(FileTransferLimits.MAX_FILES_PER_REQUEST)
    ) { onPicked(it) }
    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let(::persistAccess)
        onPicked(listOfNotNull(it))
    }
    private val openDocuments =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
            it.forEach(::persistAccess)
            onPicked(it)
        }
    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants.values.any { it }) handleDirectUris()
            else finishFailed(
                TransferError(
                    TransferError.Code.PERMISSION_DENIED,
                    getString(R.string.transfer_error_storage_permission)
                ),
                status = DeviceEventStatus.DENIED,
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val decoded =
            TransferRequestCodec.decode(intent.getStringExtra(EXTRA_REQUEST)) as? TransferRequest.Upload
        if (decoded == null) {
            finish(); return
        }
        request = decoded
        setContent { AppClientTheme { if (isPreparing) PreparingDialog() } }
        if (savedInstanceState == null) launchSource(decoded)
    }

    private fun launchSource(request: TransferRequest.Upload) {
        val source = FileInputSource.fromId(request.source)
        val mimes = request.allowedMime.split(',').map { it.trim() }.filter { it.isNotBlank() }
            .ifEmpty { listOf("*/*") }
        runCatching {
            when (source) {
                FileInputSource.MEDIA_PICKER -> launchVisual(
                    request,
                    ActivityResultContracts.PickVisualMedia.ImageAndVideo
                )

                FileInputSource.PHOTO_PICKER -> launchVisual(
                    request,
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )

                FileInputSource.FILE_PICKER -> {
                    val mime = mimes.singleOrNull() ?: "*/*"
                    if (request.allowMultiple) pickContents.launch(mime) else pickContent.launch(
                        mime
                    )
                }

                FileInputSource.SAF -> {
                    if (request.allowMultiple) openDocuments.launch(mimes.toTypedArray())
                    else openDocument.launch(mimes.toTypedArray())
                }

                FileInputSource.DIRECT_URI -> handleDirectUris()
                null -> finishFailed(
                    TransferError(
                        TransferError.Code.INVALID_REQUEST,
                        "Unknown source: ${request.source}"
                    )
                )
            }
        }.onFailure {
            finishFailed(
                TransferError(
                    TransferError.Code.INVALID_REQUEST,
                    it.message ?: "No app can handle the picker"
                )
            )
        }
    }

    private fun launchVisual(
        request: TransferRequest.Upload,
        default: ActivityResultContracts.PickVisualMedia.VisualMediaType,
    ) {
        val mime = request.allowedMime.takeIf {
            it.isNotBlank() && it != "*/*" && !it.contains(',') && !it.endsWith("/*")
        }
        val type =
            mime?.let { ActivityResultContracts.PickVisualMedia.SingleMimeType(it) } ?: default
        val visualRequest = PickVisualMediaRequest(type)
        if (request.allowMultiple) pickVisuals.launch(visualRequest) else pickVisual.launch(
            visualRequest
        )
    }

    private fun handleDirectUris() {
        val request = request ?: return
        val uris = request.uris.map { raw ->
            fileResolver.parse(raw) ?: run {
                finishFailed(
                    TransferError(
                        TransferError.Code.INVALID_REQUEST,
                        "Invalid URI: $raw"
                    )
                ); return
            }
        }
        val missing = uris.flatMap(fileResolver::missingPermissions).distinct()
        if (missing.isNotEmpty()) {
            requestPermissions.launch(missing.toTypedArray())
            return
        }
        onPicked(uris)
    }

    private fun persistAccess(uri: Uri) {
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    private fun onPicked(uris: List<Uri>) {
        val request = request ?: return
        if (uris.isEmpty()) {
            finishCancelled(); return
        }
        isPreparing = true
        lifecycleScope.launch {
            runCatching {
                uris.take(FileTransferLimits.MAX_FILES_PER_REQUEST).map { uri ->
                    val file = fileResolver.describe(uri)
                    fileResolver.validate(file, request.allowedMime, request.maxSizeBytes)
                        ?.let { throw TransferException(it) }
                    Uri.fromFile(fileResolver.stage(uri, request.transferId, file)).toString()
                }
            }.fold(
                onSuccess = { staged ->
                    launcher.start(request.copy(uris = staged))
                    finishSilently()
                },
                onFailure = { error ->
                    fileResolver.clearStaging(request.transferId)
                    finishFailed(error.toTransferError())
                },
            )
        }
    }

    private fun finishCancelled() {
        report(DeviceEventStatus.CANCELLED, emptyMap())
        finishSilently()
    }

    private fun finishFailed(
        error: TransferError,
        status: DeviceEventStatus = DeviceEventStatus.FAILED
    ) {
        report(
            status,
            mapOf(
                FileTransferParams.ERROR to error.message,
                FileTransferParams.ERROR_CODE to error.code.name,
                FileTransferParams.RETRYABLE to error.retryable.toString(),
            ),
        )
        Toast.makeText(this, error.message, Toast.LENGTH_LONG).show()
        finishSilently()
    }

    private fun report(status: DeviceEventStatus, data: Map<String, String>) {
        val request = request ?: return
        eventReporter.reportAction(
            type = DeviceEventType.FILE_TRANSFER,
            notificationId = request.notificationId,
            actionId = request.actionId,
            status = status,
            data = mapOf(
                FileTransferParams.TRANSFER_ID to request.transferId,
                FileTransferParams.SOURCE to request.source,
            ) + data,
        )
    }

    private fun finishSilently() {
        if (finished) return
        finished = true
        isPreparing = false
        finish()
    }

    @Composable
    private fun PreparingDialog() {
        Dialog(onDismissRequest = {}) {
            Card {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    Text(text = getString(R.string.transfer_preparing_upload))
                }
            }
        }
    }

    companion object {
        const val EXTRA_REQUEST = "extra_request"
    }
}
