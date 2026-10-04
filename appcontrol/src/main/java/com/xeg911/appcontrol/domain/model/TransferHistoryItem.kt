package com.xeg911.appcontrol.domain.model

import android.net.Uri
import com.xeg911.shared.data.model.transfer.FileTransferMeta

data class TransferHistoryItem(
    val id: Long,
    val deviceId: String,
    val deviceName: String,
    val meta: FileTransferMeta,
    val source: String,
    val localUri: Uri?,
    val receivedAt: Long,
    val savedAt: Long,
    val isAvailable: Boolean,
)
