package com.xeg911.appcontrol.domain.repository

import android.net.Uri
import com.xeg911.appcontrol.domain.model.ReceivedFile
import com.xeg911.appcontrol.domain.model.TransferHistoryItem
import kotlinx.coroutines.flow.Flow

interface TransferHistoryRepository {
    fun observeAll(): Flow<List<TransferHistoryItem>>

    fun observeByDevice(deviceId: String): Flow<List<TransferHistoryItem>>

    suspend fun record(deviceId: String, deviceName: String, file: ReceivedFile, savedUri: Uri)

    suspend fun delete(id: Long)

    suspend fun clear()
}
