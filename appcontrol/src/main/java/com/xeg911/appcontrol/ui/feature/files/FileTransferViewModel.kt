package com.xeg911.appcontrol.ui.feature.files

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.core.BaseViewModel
import com.xeg911.appcontrol.core.util.TelegramConfigField
import com.xeg911.appcontrol.domain.model.CallbackRecord
import com.xeg911.appcontrol.domain.model.DeviceSnapshot
import com.xeg911.appcontrol.domain.model.LocalFileInfo
import com.xeg911.appcontrol.domain.model.PullFileRequest
import com.xeg911.appcontrol.domain.model.PushFileOptions
import com.xeg911.appcontrol.domain.model.ReceivedFile
import com.xeg911.appcontrol.domain.model.StorageEvent
import com.xeg911.appcontrol.domain.model.TransferHistoryItem
import com.xeg911.appcontrol.domain.model.TransferProgress
import com.xeg911.appcontrol.domain.repository.DeviceConfigRepository
import com.xeg911.appcontrol.domain.repository.DeviceRepository
import com.xeg911.appcontrol.domain.repository.FileStorageRepository
import com.xeg911.appcontrol.domain.repository.TransferHistoryRepository
import com.xeg911.appcontrol.domain.usecase.PushFileUseCase
import com.xeg911.appcontrol.domain.usecase.RequestFileUseCase
import com.xeg911.appcontrol.ui.common.UiText
import com.xeg911.shared.data.model.event.DeviceEventStatus
import com.xeg911.shared.data.model.event.DeviceEventType
import com.xeg911.shared.data.model.notification.NotificationActionDef
import com.xeg911.shared.data.model.transfer.FileTransferMeta
import com.xeg911.shared.data.model.transfer.FileTransferParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FilesEvent {
    data class ShowMessage(val text: UiText) : FilesEvent
}

enum class FilesSection { PUSH, PULL, RECEIVED }

data class FilesUiState(
    val devices: List<DeviceSnapshot> = emptyList(),
    val selectedDeviceId: String = "",
    val section: FilesSection = FilesSection.PUSH,
    val storageConfigured: Boolean? = null,
    val pendingFile: LocalFileInfo? = null,
    val pushOptions: PushFileOptions = PushFileOptions(),
    val pushProgress: TransferProgress = TransferProgress.Idle,
    val pullRequest: PullFileRequest = PullFileRequest(),
    val isRequesting: Boolean = false,
    val received: List<ReceivedFile> = emptyList(),
    /** Keyed by [ReceivedFile.callbackKey]. */
    val downloads: Map<String, TransferProgress> = emptyMap(),
    /** Local copies already saved for this device, keyed by fileId. */
    val saved: Map<String, TransferHistoryItem> = emptyMap(),
) {
    val isPushing: Boolean get() = pushProgress is TransferProgress.Running
    val selectedDevice: DeviceSnapshot? get() = devices.firstOrNull { it.deviceId == selectedDeviceId }
}

@HiltViewModel
class FileTransferViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val deviceConfigRepository: DeviceConfigRepository,
    private val storageRepository: FileStorageRepository,
    private val historyRepository: TransferHistoryRepository,
    private val pushFile: PushFileUseCase,
    private val requestFile: RequestFileUseCase,
) : BaseViewModel<FilesEvent>() {

    private var pushJob: Job? = null
    private val downloadJobs = mutableMapOf<String, Job>()
    private val deviceJobs = mutableListOf<Job>()

    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    private val deviceId: String get() = _uiState.value.selectedDeviceId

    init {
        deviceRepository.observeDevices()
            .onEach { result ->
                val devices = result.getOrDefault(emptyList())
                _uiState.update { it.copy(devices = devices) }
                if (deviceId.isBlank()) devices.firstOrNull()?.let { selectDevice(it.deviceId) }
            }
            .launchIn(viewModelScope)
    }

    fun start(initialDeviceId: String) {
        if (initialDeviceId.isNotBlank() && deviceId != initialDeviceId) selectDevice(
            initialDeviceId
        )
    }

    fun selectDevice(id: String) {
        if (id == deviceId) return
        cancelPush(silent = true)
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
        deviceJobs.forEach { it.cancel() }
        deviceJobs.clear()
        _uiState.update {
            it.copy(
                selectedDeviceId = id,
                storageConfigured = null,
                received = emptyList(),
                downloads = emptyMap(),
                saved = emptyMap(),
                pushProgress = TransferProgress.Idle,
            )
        }
        deviceJobs += deviceRepository.observeCallbacksOfType(
            id,
            DeviceEventType.FILE_TRANSFER.name,
            RECEIVED_LIMIT
        )
            .onEach { result ->
                _uiState.update {
                    it.copy(received = result.getOrDefault(emptyList()).toReceivedFiles())
                }
            }
            .launchIn(viewModelScope)
        deviceJobs += deviceConfigRepository.observeConfig(id)
            .onEach { result ->
                val storage = result.getOrNull()?.telegram?.get(TelegramConfigField.PURPOSE_STORAGE)
                val configured = storage != null && storage.enabled &&
                        storage.botToken.isNotBlank() && storage.chatId.isNotBlank()
                _uiState.update { it.copy(storageConfigured = configured) }
            }
            .launchIn(viewModelScope)
        deviceJobs += historyRepository.observeByDevice(id)
            .onEach { items -> _uiState.update { it.copy(saved = items.associateBy { item -> item.meta.fileId }) } }
            .launchIn(viewModelScope)
    }

    fun selectSection(section: FilesSection) = _uiState.update { it.copy(section = section) }

    // Push

    fun onFilePicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            storageRepository.describe(uri).fold(
                onSuccess = { file ->
                    _uiState.update {
                        it.copy(
                            pendingFile = file,
                            pushProgress = TransferProgress.Idle
                        )
                    }
                },
                onFailure = { emitMessage(UiText.Dynamic(it.message ?: it.javaClass.simpleName)) },
            )
        }
    }

    fun clearPendingFile() =
        _uiState.update { it.copy(pendingFile = null, pushProgress = TransferProgress.Idle) }

    fun updatePushOptions(transform: PushFileOptions.() -> PushFileOptions) =
        _uiState.update { it.copy(pushOptions = it.pushOptions.transform()) }

    fun push() {
        val state = _uiState.value
        val file = state.pendingFile ?: return
        if (state.isPushing || deviceId.isBlank()) return
        pushJob = viewModelScope.launch {
            pushFile(deviceId, file, state.pushOptions).collect { progress ->
                _uiState.update { it.copy(pushProgress = progress) }
                when (progress) {
                    is TransferProgress.Done -> emitMessage(
                        UiText.Res(
                            R.string.files_push_sent,
                            file.name
                        )
                    )

                    is TransferProgress.Failed -> emitMessage(UiText.Dynamic(progress.message))
                    else -> Unit
                }
            }
        }
    }

    fun cancelPush(silent: Boolean = false) {
        val wasRunning = pushJob?.isActive == true
        pushJob?.cancel()
        pushJob = null
        _uiState.update { it.copy(pushProgress = TransferProgress.Idle) }
        if (wasRunning && !silent) viewModelScope.launch { emitMessage(UiText.Res(R.string.files_transfer_cancelled)) }
    }

    // Pull

    fun updatePullRequest(transform: PullFileRequest.() -> PullFileRequest) =
        _uiState.update { it.copy(pullRequest = it.pullRequest.transform()) }

    fun sendPullRequest() {
        if (_uiState.value.isRequesting || deviceId.isBlank()) return
        _uiState.update { it.copy(isRequesting = true) }
        viewModelScope.launch {
            requestFile(deviceId, _uiState.value.pullRequest).fold(
                onSuccess = { emitMessage(UiText.Res(R.string.files_pull_request_sent)) },
                onFailure = { emitMessage(UiText.Dynamic(it.message ?: it.javaClass.simpleName)) },
            )
            _uiState.update { it.copy(isRequesting = false) }
        }
    }

    // Received files

    fun download(file: ReceivedFile) {
        val key = file.callbackKey
        val targetDevice = deviceId
        if (downloadJobs[key]?.isActive == true) return
        downloadJobs[key] = viewModelScope.launch {
            setDownload(
                key,
                TransferProgress.Running(
                    0L,
                    file.meta.sizeBytes,
                    TransferProgress.Running.Phase.DOWNLOADING
                )
            )
            try {
                storageRepository.download(targetDevice, file.meta).collect { event ->
                    when (event) {
                        is StorageEvent.Progress -> setDownload(
                            key,
                            TransferProgress.Running(
                                event.bytesDone,
                                event.bytesTotal,
                                TransferProgress.Running.Phase.DOWNLOADING
                            ),
                        )

                        is StorageEvent.Saved -> {
                            setDownload(key, TransferProgress.Done(file.meta, event.uri))
                            historyRepository.record(
                                targetDevice,
                                deviceLabel(targetDevice),
                                file,
                                event.uri
                            )
                            emitMessage(
                                UiText.Res(
                                    R.string.files_download_saved,
                                    file.meta.fileName
                                )
                            )
                        }

                        is StorageEvent.Uploaded -> Unit
                    }
                }
            } catch (e: CancellationException) {
                setDownload(key, TransferProgress.Idle)
                throw e
            } catch (e: Exception) {
                setDownload(key, TransferProgress.Failed(e.message ?: e.javaClass.simpleName))
                emitMessage(UiText.Dynamic(e.message ?: e.javaClass.simpleName))
            }
        }
    }

    fun cancelDownload(file: ReceivedFile) {
        downloadJobs.remove(file.callbackKey)?.cancel()
        setDownload(file.callbackKey, TransferProgress.Idle)
    }

    fun deleteReceived(file: ReceivedFile) {
        viewModelScope.launch {
            deviceRepository.deleteCallback(deviceId, file.callbackKey).onFailure {
                emitMessage(UiText.Dynamic(it.message ?: it.javaClass.simpleName))
            }
        }
    }

    fun showMessage(text: UiText) = trySendEvent(FilesEvent.ShowMessage(text))

    private fun deviceLabel(id: String): String =
        _uiState.value.devices.firstOrNull { it.deviceId == id }
            ?.let { it.info.deviceName.ifBlank { it.info.model } } ?: id

    private fun setDownload(key: String, progress: TransferProgress) =
        _uiState.update { it.copy(downloads = it.downloads + (key to progress)) }

    private suspend fun emitMessage(text: UiText) = sendEvent(FilesEvent.ShowMessage(text))

    private fun List<CallbackRecord>.toReceivedFiles(): List<ReceivedFile> = mapNotNull { record ->
        val event = record.event
        if (!event.actionId.equals(
                NotificationActionDef.UPLOAD_FILE.id,
                ignoreCase = true
            )
        ) return@mapNotNull null
        if (event.status != DeviceEventStatus.SUCCESS) return@mapNotNull null
        val meta = FileTransferMeta.fromParams(event.data) ?: return@mapNotNull null
        ReceivedFile(
            callbackKey = record.key,
            notificationId = event.notificationId,
            meta = meta,
            source = event.data[FileTransferParams.SOURCE].orEmpty(),
            receivedAt = event.timestamp,
        )
    }.sortedByDescending { it.receivedAt }

    private companion object {
        const val RECEIVED_LIMIT = 100
    }
}
