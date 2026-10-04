package com.xeg911.appcontrol.domain.repository

import com.xeg911.appcontrol.domain.model.DeviceLocation
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun observeLocation(deviceId: String): Flow<Result<DeviceLocation>>

    /**
     * Writes `location/request = now`, a running AppClient sharing service answers with a
     * fix whose `requestId` equals that value, which arrives through [observeLocation].
     */
    suspend fun requestCurrentLocation(deviceId: String): Result<Long>
}
