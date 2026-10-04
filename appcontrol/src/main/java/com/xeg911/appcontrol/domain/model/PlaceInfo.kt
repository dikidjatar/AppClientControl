package com.xeg911.appcontrol.domain.model

/**
 * Reverse-geocoded place for a coordinate (OpenStreetMap Nominatim).
 */
data class PlaceInfo(
    val displayName: String,
    val name: String,
    val category: String,
    val type: String,
    val osmType: String,
    val osmId: Long,
    val address: PlaceAddress,
    /** south, north, west, east */
    val boundingBox: List<Double>,
    val licence: String,
) {
    /**
     * Compact one-liner: locality + region + country.
     */
    val shortLabel: String
        get() = listOfNotNull(
            address.locality.takeIf { it.isNotBlank() },
            address.state.takeIf { it.isNotBlank() },
            address.country.takeIf { it.isNotBlank() },
        ).joinToString(", ")
}

data class PlaceAddress(
    val houseNumber: String = "",
    val road: String = "",
    val neighbourhood: String = "",
    val suburb: String = "",
    val village: String = "",
    val town: String = "",
    val city: String = "",
    val municipality: String = "",
    val county: String = "",
    val state: String = "",
    val postcode: String = "",
    val country: String = "",
    val countryCode: String = "",
) {
    val locality: String get() = village.ifBlank { town.ifBlank { city.ifBlank { municipality } } }

    fun rows(): List<Pair<String, String>> = listOf(
        "Street" to listOf(road, houseNumber).filter { it.isNotBlank() }.joinToString(" "),
        "Neighbourhood" to neighbourhood.ifBlank { suburb },
        "Village / Town" to village.ifBlank { town },
        "City" to city.ifBlank { municipality },
        "County" to county,
        "State" to state,
        "Postcode" to postcode,
        "Country" to if (countryCode.isBlank()) country else "$country (${countryCode.uppercase()})",
    ).filter { it.second.isNotBlank() }
}
