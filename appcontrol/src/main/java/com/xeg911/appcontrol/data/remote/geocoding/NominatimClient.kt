package com.xeg911.appcontrol.data.remote.geocoding

import com.xeg911.appcontrol.domain.model.PlaceAddress
import com.xeg911.appcontrol.domain.model.PlaceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nominatim reverse geocoding. Usage policy: identify the app via User-Agent and stay
 * at or below one request per second, hence the shared [gate].
 */
@Singleton
class NominatimClient @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) {
    private val gate = Mutex()
    private var lastRequestAt = 0L

    suspend fun reverse(latitude: Double, longitude: Double): PlaceInfo =
        withContext(Dispatchers.IO) {
            gate.withLock {
                val wait = MIN_INTERVAL_MS - (System.currentTimeMillis() - lastRequestAt)
                if (wait > 0) delay(wait)
                lastRequestAt = System.currentTimeMillis()
            }
            val url = BASE_URL.toHttpUrl().newBuilder()
                .addQueryParameter("format", "jsonv2")
                .addQueryParameter("lat", latitude.toString())
                .addQueryParameter("lon", longitude.toString())
                .addQueryParameter("zoom", "18")
                .addQueryParameter("addressdetails", "1")
                .addQueryParameter("accept-language", Locale.getDefault().language)
                .build()
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body!!.string()
                if (!response.isSuccessful) throw IOException("Nominatim HTTP ${response.code}")
                val dto = json.decodeFromString(ReverseResponse.serializer(), body)
                if (dto.error != null) throw IOException(dto.error)
                dto.toDomain()
            }
        }

    private companion object {
        const val BASE_URL = "https://nominatim.openstreetmap.org/reverse"
        const val USER_AGENT = "AppControl/1.0 (com.xeg911.appcontrol)"
        const val MIN_INTERVAL_MS = 1_100L
    }
}

@Serializable
private data class ReverseResponse(
    @SerialName("place_id") val placeId: Long = 0L,
    val licence: String = "",
    @SerialName("osm_type") val osmType: String = "",
    @SerialName("osm_id") val osmId: Long = 0L,
    val category: String = "",
    val type: String = "",
    val name: String = "",
    @SerialName("display_name") val displayName: String = "",
    val address: AddressDto = AddressDto(),
    @SerialName("boundingbox") val boundingBox: List<String> = emptyList(),
    val error: String? = null,
) {
    fun toDomain() = PlaceInfo(
        displayName = displayName,
        name = name,
        category = category,
        type = type,
        osmType = osmType,
        osmId = osmId,
        address = address.toDomain(),
        boundingBox = boundingBox.mapNotNull { it.toDoubleOrNull() },
        licence = licence,
    )
}

@Serializable
private data class AddressDto(
    @SerialName("house_number") val houseNumber: String = "",
    val road: String = "",
    val neighbourhood: String = "",
    val suburb: String = "",
    val village: String = "",
    val hamlet: String = "",
    val town: String = "",
    val city: String = "",
    val municipality: String = "",
    val county: String = "",
    val state: String = "",
    val region: String = "",
    val postcode: String = "",
    val country: String = "",
    @SerialName("country_code") val countryCode: String = "",
) {
    fun toDomain() = PlaceAddress(
        houseNumber = houseNumber,
        road = road,
        neighbourhood = neighbourhood,
        suburb = suburb,
        village = village.ifBlank { hamlet },
        town = town,
        city = city,
        municipality = municipality,
        county = county,
        state = state.ifBlank { region },
        postcode = postcode,
        country = country,
        countryCode = countryCode,
    )
}
