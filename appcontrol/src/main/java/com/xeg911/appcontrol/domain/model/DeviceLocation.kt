package com.xeg911.appcontrol.domain.model

import com.xeg911.shared.data.model.LocationSharingState
import com.xeg911.shared.data.model.LocationSharingStatus
import com.xeg911.shared.data.model.LocationSnapshot

data class DeviceLocation(
    val latest: LocationSnapshot? = null,
    val status: LocationSharingStatus? = null,
    val history: List<LocationSnapshot> = emptyList(),
)

val LocationSharingStatus?.isActive: Boolean
    get() = this?.state == LocationSharingState.ACTIVE

/**
 * Universal Google Maps link, opens the Maps app when installed.
 */
fun LocationSnapshot.googleMapsUrl(): String =
    "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"

fun LocationSnapshot.coordinatesLabel(): String = "%.6f, %.6f".format(latitude, longitude)
