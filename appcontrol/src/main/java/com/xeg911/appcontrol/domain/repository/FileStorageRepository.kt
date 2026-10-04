package com.xeg911.appcontrol.domain.repository

import android.net.Uri
import com.xeg911.appcontrol.domain.model.LocalFileInfo
import com.xeg911.appcontrol.domain.model.StorageEvent
import com.xeg911.shared.data.model.transfer.FileTransferMeta
import kotlinx.coroutines.flow.Flow

interface FileStorageRepository {
    suspend fun describe(uri: Uri): Result<LocalFileInfo>

    suspend fun isStorageConfigured(deviceId: String): Boolean

    fun upload(deviceId: String, file: LocalFileInfo, caption: String): Flow<StorageEvent>

    fun download(deviceId: String, meta: FileTransferMeta): Flow<StorageEvent>
}
