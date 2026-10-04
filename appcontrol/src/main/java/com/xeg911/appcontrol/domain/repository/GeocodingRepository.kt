package com.xeg911.appcontrol.domain.repository

import com.xeg911.appcontrol.domain.model.PlaceInfo

interface GeocodingRepository {
    suspend fun reverse(latitude: Double, longitude: Double): Result<PlaceInfo>
}
