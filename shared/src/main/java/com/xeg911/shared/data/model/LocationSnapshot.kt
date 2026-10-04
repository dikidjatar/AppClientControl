package com.xeg911.shared.data.model

enum class LocationSource {
    /**
     * Delivered by the periodic background update loop.
     */
    PERIODIC,

    /**
     * Answer to an explicit AppControl request.
     */
    ON_DEMAND
}

/**
 * A single location fix published by AppClient. Only written while the user has
 * explicitly enabled location sharing.
 */
data class LocationSnapshot(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracyMeters: Float = 0f,
    val altitudeMeters: Double = 0.0,
    val speedMps: Float = 0f,
    val bearingDegrees: Float = 0f,
    val provider: String = "",
    val source: LocationSource = LocationSource.PERIODIC,
    /**
     * The `location/request` timestamp this fix answers; 0 for periodic fixes.
     */
    val requestId: Long = 0L,
    /**
     * Time of the GPS fix itself.
     */
    val fixedAt: Long = 0L,
    /**
     * Time the fix was published to the backend.
     */
    val capturedAt: Long = 0L,
)
