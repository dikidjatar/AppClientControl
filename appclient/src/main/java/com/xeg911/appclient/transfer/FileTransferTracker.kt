package com.xeg911.appclient.transfer

import com.xeg911.appclient.transfer.model.TransferState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileTransferTracker @Inject constructor() {

    private val _states = MutableStateFlow<Map<String, TransferState>>(emptyMap())
    val states: StateFlow<Map<String, TransferState>> = _states.asStateFlow()

    private val _cancelRequests = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val cancelRequests: SharedFlow<String> = _cancelRequests.asSharedFlow()

    fun update(state: TransferState) {
        _states.update { it + (state.request.transferId to state) }
    }

    fun remove(transferId: String) {
        _states.update { it - transferId }
    }

    fun requestCancel(transferId: String) {
        _cancelRequests.tryEmit(transferId)
    }

    fun isTerminal(transferId: String): Boolean = when (_states.value[transferId]) {
        null, is TransferState.Succeeded, is TransferState.Failed, is TransferState.Cancelled -> true
        else -> false
    }
}
