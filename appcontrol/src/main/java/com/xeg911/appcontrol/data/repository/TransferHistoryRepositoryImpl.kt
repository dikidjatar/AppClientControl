package com.xeg911.appcontrol.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.xeg911.appcontrol.data.local.TransferHistoryDao
import com.xeg911.appcontrol.data.local.TransferHistoryEntity
import com.xeg911.appcontrol.domain.model.ReceivedFile
import com.xeg911.appcontrol.domain.model.TransferHistoryItem
import com.xeg911.appcontrol.domain.repository.TransferHistoryRepository
import com.xeg911.shared.data.model.transfer.FileTransferMeta
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferHistoryRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dao: TransferHistoryDao,
) : TransferHistoryRepository {

    override fun observeAll(): Flow<List<TransferHistoryItem>> =
        dao.observeAll()
            .map { list -> list.map { it.toItem() } }
            .flowOn(Dispatchers.IO)

    override fun observeByDevice(deviceId: String): Flow<List<TransferHistoryItem>> =
        dao.observeByDevice(deviceId)
            .map { list -> list.map { it.toItem() } }
            .flowOn(Dispatchers.IO)

    override suspend fun record(
        deviceId: String,
        deviceName: String,
        file: ReceivedFile,
        savedUri: Uri,
    ) {
        val now = System.currentTimeMillis()
        val existing = dao.find(deviceId, file.meta.fileId)
        if (existing != null) {
            dao.updateLocation(existing.id, savedUri.toString(), now)
            return
        }
        dao.upsert(
            TransferHistoryEntity(
                deviceId = deviceId,
                deviceName = deviceName,
                fileId = file.meta.fileId,
                fileName = file.meta.fileName,
                mimeType = file.meta.mimeType,
                sizeBytes = file.meta.sizeBytes,
                source = file.source,
                downloadUrl = file.meta.downloadUrl,
                localUri = savedUri.toString(),
                receivedAt = file.receivedAt,
                savedAt = now,
            )
        )
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun clear() = dao.clear()

    private fun TransferHistoryEntity.toItem(): TransferHistoryItem {
        val uri = localUri.takeIf { it.isNotBlank() }?.toUri()
        return TransferHistoryItem(
            id = id,
            deviceId = deviceId,
            deviceName = deviceName,
            meta = FileTransferMeta(
                fileId = fileId,
                fileName = fileName,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                downloadUrl = downloadUrl,
            ),
            source = source,
            localUri = uri,
            receivedAt = receivedAt,
            savedAt = savedAt,
            isAvailable = uri != null && uri.exists(),
        )
    }

    private fun Uri.exists(): Boolean = runCatching {
        context.contentResolver.openFileDescriptor(this, "r")?.use { true } ?: false
    }.getOrDefault(false)
}
