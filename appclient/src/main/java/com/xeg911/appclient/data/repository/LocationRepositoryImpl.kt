package com.xeg911.appclient.data.repository

import com.xeg911.appclient.core.device.DeviceIdentifier
import com.xeg911.appclient.data.remote.firebase.DeviceFirebaseDataSource
import com.xeg911.appclient.domain.repository.LocationRepository
import com.xeg911.shared.data.model.LocationSharingStatus
import com.xeg911.shared.data.model.LocationSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val deviceIdentifier: DeviceIdentifier,
    private val dataSource: DeviceFirebaseDataSource,
) : LocationRepository {

    override suspend fun publishLocation(snapshot: LocationSnapshot): Result<Unit> = runCatching {
        dataSource.writeLocation(deviceIdentifier.resolveDeviceId(), snapshot)
    }

    override suspend fun publishStatus(status: LocationSharingStatus): Result<Unit> = runCatching {
        dataSource.writeLocationStatus(deviceIdentifier.resolveDeviceId(), status)
    }

    override fun observeLocationRequest(): Flow<Long> = flow {
        emitAll(dataSource.observeLocationRequest(deviceIdentifier.resolveDeviceId()))
    }
}
