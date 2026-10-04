package com.xeg911.shared.data.model

enum class LocationSharingState {
    /**
     * User has not consented, or explicitly stopped sharing.
     */
    DISABLED,

    /**
     * User consented but the runtime location permission is missing.
     */
    PERMISSION_REQUIRED,

    /**
     * Sharing is enabled but device location services (GPS) are turned off.
     */
    LOCATION_OFF,

    /**
     * Enabled and permitted, foreground service not running yet.
     */
    STARTING,

    /**
     * Foreground service running and publishing fixes.
     */
    ACTIVE
}

/**
 * Description of the location sharing state on a device.
 */
data class LocationSharingStatus(
    val state: LocationSharingState = LocationSharingState.DISABLED,
    val consentGiven: Boolean = false,
    val consentAt: Long = 0L,
    val foregroundPermission: Boolean = false,
    val backgroundPermission: Boolean = false,
    val locationServicesEnabled: Boolean = false,
    val serviceRunning: Boolean = false,
    val intervalMs: Long = 0L,
    val lastUpdateAt: Long = 0L,
    val lastError: String = "",
    val updatedAt: Long = 0L,
)
