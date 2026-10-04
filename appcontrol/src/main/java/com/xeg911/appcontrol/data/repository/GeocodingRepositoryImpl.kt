package com.xeg911.appcontrol.data.repository

import com.xeg911.appcontrol.data.remote.geocoding.NominatimClient
import com.xeg911.appcontrol.domain.model.PlaceInfo
import com.xeg911.appcontrol.domain.repository.GeocodingRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

@Singleton
class GeocodingRepositoryImpl @Inject constructor(
    private val client: NominatimClient,
) : GeocodingRepository {

    private val lock = Mutex()
    private val cache = object : LinkedHashMap<String, PlaceInfo>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PlaceInfo>) =
            size > MAX_ENTRIES
    }

    override suspend fun reverse(latitude: Double, longitude: Double): Result<PlaceInfo> {
        val key = cacheKey(latitude, longitude)
        lock.withLock { cache[key] }?.let { return Result.success(it) }
        return runCatching { client.reverse(latitude, longitude) }
            .onSuccess { place -> lock.withLock { cache[key] = place } }
    }

    private fun cacheKey(lat: Double, lon: Double) =
        "${(lat * 10_000).roundToLong()}:${(lon * 10_000).roundToLong()}"

    private companion object {
        const val MAX_ENTRIES = 64
    }
}
