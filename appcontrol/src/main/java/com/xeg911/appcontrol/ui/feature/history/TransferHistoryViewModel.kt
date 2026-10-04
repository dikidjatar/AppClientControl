package com.xeg911.appcontrol.ui.feature.history

import androidx.lifecycle.viewModelScope
import com.xeg911.appcontrol.R
import com.xeg911.appcontrol.core.BaseViewModel
import com.xeg911.appcontrol.domain.model.ReceivedFile
import com.xeg911.appcontrol.domain.model.StorageEvent
import com.xeg911.appcontrol.domain.model.TransferHistoryItem
import com.xeg911.appcontrol.domain.model.TransferProgress
import com.xeg911.appcontrol.domain.repository.FileStorageRepository
import com.xeg911.appcontrol.domain.repository.TransferHistoryRepository
import com.xeg911.appcontrol.ui.common.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HistoryEvent {
    data class ShowMessage(val text: UiText) : HistoryEvent
}

data class HistoryFilter(
    val deviceId: String? = null,
    val onlyMissing: Boolean = false,
    val query: String = "",
)

data class HistoryUiState(
    val items: List<TransferHistoryItem> = emptyList(),
    val filter: HistoryFilter = HistoryFilter(),
    /** Keyed by history id, only present while a re-download runs. */
    val redownloads: Map<Long, TransferProgress> = emptyMap(),
    val isLoading: Boolean = true,
) {
    val devices: List<Pair<String, String>>
        get() = items.map { it.deviceId to it.deviceName }.distinctBy { it.first }
    val visible: List<TransferHistoryItem>
        get() = items.filter { item ->
            (filter.deviceId == null || item.deviceId == filter.deviceId) &&
                    (!filter.onlyMissing || !item.isAvailable) &&
                    (filter.query.isBlank() || item.matches(filter.query))
        }
    val missingCount: Int get() = items.count { !it.isAvailable }
}

private fun TransferHistoryItem.matches(query: String): Boolean =
    meta.fileName.contains(query, ignoreCase = true) ||
            deviceName.contains(query, ignoreCase = true) ||
            meta.mimeType.contains(query, ignoreCase = true)

@HiltViewModel
class TransferHistoryViewModel @Inject constructor(
    private val historyRepository: TransferHistoryRepository,
    private val storageRepository: FileStorageRepository,
) : BaseViewModel<HistoryEvent>() {

    private val filter = MutableStateFlow(HistoryFilter())
    private val redownloads = MutableStateFlow<Map<Long, TransferProgress>>(emptyMap())
    private val jobs = mutableMapOf<Long, Job>()

    val uiState: StateFlow<HistoryUiState> = combine(
        historyRepository.observeAll(), filter, redownloads,
    ) { items, filter, redownloads ->
        HistoryUiState(items = items, filter = filter, redownloads = redownloads, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setDeviceFilter(deviceId: String?) = filter.update { it.copy(deviceId = deviceId) }

    fun setQuery(query: String) = filter.update { it.copy(query = query) }

    fun toggleOnlyMissing() = filter.update { it.copy(onlyMissing = !it.onlyMissing) }

    fun delete(item: TransferHistoryItem) {
        viewModelScope.launch {
            historyRepository.delete(item.id)
            emitMessage(UiText.Res(R.string.history_entry_removed))
        }
    }

    fun clear() {
        viewModelScope.launch {
            historyRepository.clear()
            emitMessage(UiText.Res(R.string.history_cleared))
        }
    }

    /**
     * Fetches the file again from Telegram Storage when the local copy was deleted.
     */
    fun redownload(item: TransferHistoryItem) {
        if (jobs[item.id]?.isActive == true) return
        val received = ReceivedFile(
            callbackKey = "",
            notificationId = "",
            meta = item.meta,
            source = item.source,
            receivedAt = item.receivedAt,
        )
        jobs[item.id] = viewModelScope.launch {
            setProgress(
                item.id,
                TransferProgress.Running(
                    0L,
                    item.meta.sizeBytes,
                    TransferProgress.Running.Phase.DOWNLOADING
                )
            )
            try {
                storageRepository.download(item.deviceId, item.meta).collect { event ->
                    when (event) {
                        is StorageEvent.Progress -> setProgress(
                            item.id,
                            TransferProgress.Running(
                                event.bytesDone,
                                event.bytesTotal,
                                TransferProgress.Running.Phase.DOWNLOADING
                            ),
                        )

                        is StorageEvent.Saved -> {
                            historyRepository.record(
                                item.deviceId,
                                item.deviceName,
                                received,
                                event.uri
                            )
                            setProgress(item.id, null)
                            emitMessage(
                                UiText.Res(
                                    R.string.files_download_saved,
                                    item.meta.fileName
                                )
                            )
                        }

                        is StorageEvent.Uploaded -> Unit
                    }
                }
            } catch (e: CancellationException) {
                setProgress(item.id, null)
                throw e
            } catch (e: Exception) {
                setProgress(item.id, TransferProgress.Failed(e.message ?: e.javaClass.simpleName))
                emitMessage(UiText.Dynamic(e.message ?: e.javaClass.simpleName))
            }
        }
    }

    fun cancelRedownload(item: TransferHistoryItem) {
        jobs.remove(item.id)?.cancel()
        setProgress(item.id, null)
    }

    fun showMessage(text: UiText) = trySendEvent(HistoryEvent.ShowMessage(text))

    private fun setProgress(id: Long, progress: TransferProgress?) = redownloads.update { map ->
        if (progress == null) map - id else map + (id to progress)
    }

    private suspend fun emitMessage(text: UiText) = sendEvent(HistoryEvent.ShowMessage(text))
}
