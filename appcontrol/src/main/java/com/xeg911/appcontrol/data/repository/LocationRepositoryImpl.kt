package com.xeg911.appcontrol.data.repository

import com.google.firebase.database.FirebaseDatabase
import com.xeg911.appcontrol.core.util.DeviceNode
import com.xeg911.appcontrol.core.util.FirebaseNode
import com.xeg911.appcontrol.core.util.LocationField
import com.xeg911.appcontrol.core.util.getValueOrNull
import com.xeg911.appcontrol.core.util.observeFlow
import com.xeg911.appcontrol.domain.model.DeviceLocation
import com.xeg911.appcontrol.domain.repository.LocationRepository
import com.xeg911.shared.data.model.LocationSharingStatus
import com.xeg911.shared.data.model.LocationSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val database: FirebaseDatabase,
) : LocationRepository {

    private fun locationRef(deviceId: String) = database.reference
        .child(FirebaseNode.NODE_DEVICES)
        .child(deviceId)
        .child(DeviceNode.NODE_LOCATION)

    override fun observeLocation(deviceId: String): Flow<Result<DeviceLocation>> =
        locationRef(deviceId).observeFlow().map { result ->
            result.map { snapshot ->
                DeviceLocation(
                    latest = snapshot.child(LocationField.NODE_LATEST)
                        .getValueOrNull<LocationSnapshot>(),
                    status = snapshot.child(LocationField.NODE_STATUS)
                        .getValueOrNull<LocationSharingStatus>(),
                    history = snapshot.child(LocationField.NODE_HISTORY).children
                        .mapNotNull { it.getValueOrNull<LocationSnapshot>() }
                        .sortedByDescending { it.capturedAt },
                )
            }
        }

    override suspend fun requestCurrentLocation(deviceId: String): Result<Long> = runCatching {
        val requestId = System.currentTimeMillis()
        locationRef(deviceId)
            .child(LocationField.FIELD_REQUEST)
            .setValue(requestId)
            .await()
        requestId
    }
}
